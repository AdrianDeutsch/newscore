package de.newscore.unit.kafka;

import static org.mockito.Mockito.verify;

import de.newscore.kafka.ArticleEvent;
import de.newscore.kafka.ArticleEventType;
import de.newscore.kafka.KafkaEventPublisher;
import de.newscore.kafka.KafkaTopics;
import de.newscore.kafka.SearchExecutedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks
    private KafkaEventPublisher publisher;

    @Test
    @DisplayName("article events are keyed by article id on the article topic")
    void publishArticleEvent_sendsKeyedToArticleTopic() {
        ArticleEvent event = ArticleEvent.of(ArticleEventType.PUBLISHED, "42");

        publisher.publishArticleEvent(event);

        verify(kafkaTemplate).send(KafkaTopics.ARTICLE_EVENTS, "42", event);
    }

    @Test
    @DisplayName("search events go to the search topic")
    void publishSearchExecuted_sendsToSearchTopic() {
        SearchExecutedEvent event = SearchExecutedEvent.of("klima", 3);

        publisher.publishSearchExecuted(event);

        verify(kafkaTemplate).send(KafkaTopics.SEARCH_EVENTS, event);
    }
}
