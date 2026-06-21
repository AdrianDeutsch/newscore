package de.newscore.unit.kafka;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import de.newscore.kafka.ArticleEvent;
import de.newscore.kafka.ArticleEventType;
import de.newscore.kafka.KafkaEventPublisher;
import de.newscore.kafka.KafkaTopics;
import de.newscore.kafka.PageViewEvent;
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
    @DisplayName("article events are mapped to Avro and keyed by article id on the article topic")
    void publishArticleEvent_sendsKeyedAvro() {
        publisher.publishArticleEvent(ArticleEvent.of(ArticleEventType.PUBLISHED, "42"));

        verify(kafkaTemplate).send(eq(KafkaTopics.ARTICLE_EVENTS), eq("42"),
                argThat(value -> value instanceof de.newscore.kafka.avro.ArticleEvent avro
                        && avro.getArticleId().equals("42")
                        && avro.getType().name().equals("PUBLISHED")));
    }

    @Test
    @DisplayName("search events are mapped to Avro on the search topic")
    void publishSearchExecuted_sendsAvro() {
        publisher.publishSearchExecuted(SearchExecutedEvent.of("klima", 3));

        verify(kafkaTemplate).send(eq(KafkaTopics.SEARCH_EVENTS),
                argThat(value -> value instanceof de.newscore.kafka.avro.SearchExecutedEvent avro
                        && avro.getQuery().equals("klima")
                        && avro.getResultCount() == 3));
    }

    @Test
    @DisplayName("page-view events are mapped to Avro on the user topic")
    void publishPageView_sendsAvro() {
        publisher.publishPageView(PageViewEvent.of("/article/1"));

        verify(kafkaTemplate).send(eq(KafkaTopics.USER_EVENTS),
                argThat(value -> value instanceof de.newscore.kafka.avro.PageViewEvent avro
                        && avro.getPath().equals("/article/1")));
    }
}
