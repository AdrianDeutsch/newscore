package de.newscore.integration.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import de.newscore.NewsCoreApplication;
import de.newscore.config.CachingConfig;
import de.newscore.kafka.ArticleEvent;
import de.newscore.kafka.ArticleEventType;
import de.newscore.kafka.EventPublisher;
import de.newscore.kafka.KafkaTopics;
import de.newscore.kafka.SearchExecutedEvent;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end Kafka test using an in-JVM broker (EmbeddedKafka) and a <strong>mock</strong> Confluent
 * Schema Registry ({@code mock://} URL) — no Docker / external registry required, so it runs in
 * {@code ./mvnw verify} everywhere. Covers the Avro producer, the consumer's active cache eviction
 * and the dead-letter routing of poison records (ADR-011).
 */
@SpringBootTest(
        classes = NewsCoreApplication.class,
        properties = "spring.kafka.properties.schema.registry.url=mock://newscore")
@ActiveProfiles("kafka")
@EmbeddedKafka(
        partitions = 1,
        topics = {KafkaTopics.ARTICLE_EVENTS, KafkaTopics.ARTICLE_EVENTS_DLT, KafkaTopics.SEARCH_EVENTS},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@Tag("integration")
class KafkaEventFlowIntegrationTest {

    private static final String REGISTRY = "mock://newscore";

    @Autowired
    private EventPublisher eventPublisher;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    @DisplayName("search-executed events are published as Avro to the search topic")
    void searchExecutedEvent_isPublishedAsAvro() {
        eventPublisher.publishSearchExecuted(SearchExecutedEvent.of("klima", 2));

        try (Consumer<String, de.newscore.kafka.avro.SearchExecutedEvent> consumer = avroConsumer("search-it")) {
            consumer.subscribe(List.of(KafkaTopics.SEARCH_EVENTS));
            ConsumerRecord<String, de.newscore.kafka.avro.SearchExecutedEvent> record =
                    KafkaTestUtils.getSingleRecord(consumer, KafkaTopics.SEARCH_EVENTS, Duration.ofSeconds(15));
            assertThat(record.value().getQuery()).isEqualTo("klima");
            assertThat(record.value().getResultCount()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("an article event evicts the article caches (active invalidation)")
    void articleEvent_evictsCaches() {
        Cache cache = cacheManager.getCache(CachingConfig.ARTICLE_CACHE);
        assertThat(cache).isNotNull();
        cache.put("1", "stale-value");

        eventPublisher.publishArticleEvent(ArticleEvent.of(ArticleEventType.PUBLISHED, "1"));

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(cache.get("1")).isNull());
    }

    @Test
    @DisplayName("a poison record (non-Avro) is routed to the dead-letter topic")
    void poisonRecord_goesToDeadLetterTopic() {
        try (Producer<String, byte[]> producer = bytesProducer()) {
            producer.send(new ProducerRecord<>(KafkaTopics.ARTICLE_EVENTS, "bad", "not-avro".getBytes()));
            producer.flush();
        }

        try (Consumer<String, byte[]> consumer = bytesConsumer("dlt-it")) {
            consumer.subscribe(List.of(KafkaTopics.ARTICLE_EVENTS_DLT));
            ConsumerRecord<String, byte[]> record =
                    KafkaTestUtils.getSingleRecord(consumer, KafkaTopics.ARTICLE_EVENTS_DLT, Duration.ofSeconds(20));
            assertThat(record.key()).isEqualTo("bad");
        }
    }

    private <T> Consumer<String, T> avroConsumer(String group) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(group, "true", broker);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, REGISTRY);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        return new DefaultKafkaConsumerFactory<String, T>(props).createConsumer();
    }

    private Consumer<String, byte[]> bytesConsumer(String group) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(group, "true", broker);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new ByteArrayDeserializer())
                .createConsumer();
    }

    private Producer<String, byte[]> bytesProducer() {
        Map<String, Object> props = KafkaTestUtils.producerProps(broker);
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new ByteArraySerializer())
                .createProducer();
    }
}
