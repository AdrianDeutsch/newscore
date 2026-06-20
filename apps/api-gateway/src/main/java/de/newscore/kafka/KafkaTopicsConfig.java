package de.newscore.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the Kafka topics so they are auto-created on startup (via {@code KafkaAdmin}) in the
 * {@code kafka} profile. Article events are partitioned for per-article ordering by key; the
 * single broker in local/CI uses replication factor 1.
 */
@Configuration
@Profile("kafka")
public class KafkaTopicsConfig {

    private static final int PARTITIONS = 3;
    private static final short REPLICAS = 1;

    @Bean
    public NewTopic articleEventsTopic() {
        return TopicBuilder.name(KafkaTopics.ARTICLE_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    public NewTopic articleEventsDltTopic() {
        return TopicBuilder.name(KafkaTopics.ARTICLE_EVENTS_DLT).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    public NewTopic searchEventsTopic() {
        return TopicBuilder.name(KafkaTopics.SEARCH_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name(KafkaTopics.USER_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }
}
