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
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
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
 * End-to-end Kafka test using an in-JVM broker (EmbeddedKafka) — no Docker required, so it runs in
 * {@code ./mvnw verify} everywhere. Covers the producer, the consumer's active cache eviction and
 * the dead-letter routing of poison records (Epic 4 / ADR-005).
 */
@SpringBootTest(classes = NewsCoreApplication.class)
@ActiveProfiles("kafka")
@EmbeddedKafka(
        partitions = 1,
        topics = {KafkaTopics.ARTICLE_EVENTS, KafkaTopics.ARTICLE_EVENTS_DLT, KafkaTopics.SEARCH_EVENTS},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@Tag("integration")
class KafkaEventFlowIntegrationTest {

    @Autowired
    private EventPublisher eventPublisher;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    @DisplayName("search-executed events are published to the search topic")
    void searchExecutedEvent_isPublished() {
        eventPublisher.publishSearchExecuted(SearchExecutedEvent.of("klima", 2));

        try (Consumer<String, String> consumer = stringConsumer("search-it")) {
            consumer.subscribe(List.of(KafkaTopics.SEARCH_EVENTS));
            ConsumerRecord<String, String> record =
                    KafkaTestUtils.getSingleRecord(consumer, KafkaTopics.SEARCH_EVENTS, Duration.ofSeconds(15));
            assertThat(record.value()).contains("\"query\":\"klima\"").contains("\"resultCount\":2");
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
    @DisplayName("a poison record is routed to the dead-letter topic")
    void poisonRecord_goesToDeadLetterTopic() {
        try (Producer<String, String> producer = stringProducer()) {
            producer.send(new ProducerRecord<>(KafkaTopics.ARTICLE_EVENTS, "bad", "this-is-not-valid-json"));
            producer.flush();
        }

        try (Consumer<String, String> consumer = stringConsumer("dlt-it")) {
            consumer.subscribe(List.of(KafkaTopics.ARTICLE_EVENTS_DLT));
            // Reaching here means a record arrived on the DLT (getSingleRecord throws otherwise).
            // The recoverer preserves the original key; the payload is re-serialized by the
            // gateway's JSON template, so we assert on the key rather than the raw value.
            ConsumerRecord<String, String> record =
                    KafkaTestUtils.getSingleRecord(consumer, KafkaTopics.ARTICLE_EVENTS_DLT, Duration.ofSeconds(20));
            assertThat(record.key()).isEqualTo("bad");
            assertThat(record.value()).isNotBlank();
        }
    }

    private Consumer<String, String> stringConsumer(String group) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(group, "true", broker);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
    }

    private Producer<String, String> stringProducer() {
        Map<String, Object> props = KafkaTestUtils.producerProps(broker);
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer())
                .createProducer();
    }
}
