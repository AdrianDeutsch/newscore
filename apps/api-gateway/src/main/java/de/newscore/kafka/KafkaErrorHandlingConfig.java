package de.newscore.kafka;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Error handling for Kafka consumers (see ADR-005).
 *
 * <p>Failed records are retried a fixed number of times; if they still fail (or fail to
 * deserialize), they are routed to the {@code <topic>.DLT} dead-letter topic by the
 * {@link DeadLetterPublishingRecoverer} instead of blocking the partition. Spring Boot wires this
 * {@link DefaultErrorHandler} bean into the auto-configured listener container factory.</p>
 *
 * <p>The dead-letter topic name is resolved here rather than left to the library default: Spring
 * Kafka changed that default from {@code .DLT} to {@code -dlt}, which would silently route poison
 * records past the topic declared in {@link KafkaTopicsConfig} (see docs/defects/DEFECT-004).</p>
 */
@Configuration
@Profile("kafka")
public class KafkaErrorHandlingConfig {

    private static final long RETRY_INTERVAL_MS = 500L;
    private static final long MAX_RETRIES = 2L;

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, exception) ->
                        new TopicPartition(record.topic() + KafkaTopics.DLT_SUFFIX, record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRIES));
    }
}
