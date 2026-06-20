package de.newscore.kafka;

import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka-backed {@link EventPublisher} (active under the {@code kafka} profile). Article events are
 * keyed by article id to preserve per-article ordering across partitions.
 */
@Component
@Profile("kafka")
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishArticleEvent(ArticleEvent event) {
        kafkaTemplate.send(KafkaTopics.ARTICLE_EVENTS, event.articleId(), event);
    }

    @Override
    public void publishSearchExecuted(SearchExecutedEvent event) {
        kafkaTemplate.send(KafkaTopics.SEARCH_EVENTS, event);
    }

    @Override
    public void publishPageView(PageViewEvent event) {
        kafkaTemplate.send(KafkaTopics.USER_EVENTS, event);
    }
}
