package de.newscore.unit.kafka;

import static org.assertj.core.api.Assertions.assertThatCode;

import de.newscore.kafka.ArticleEvent;
import de.newscore.kafka.ArticleEventType;
import de.newscore.kafka.NoOpEventPublisher;
import de.newscore.kafka.SearchExecutedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoOpEventPublisherTest {

    private final NoOpEventPublisher publisher = new NoOpEventPublisher();

    @Test
    @DisplayName("the no-op publisher silently drops events")
    void dropsEventsWithoutError() {
        assertThatCode(() -> {
            publisher.publishArticleEvent(ArticleEvent.of(ArticleEventType.PUBLISHED, "1"));
            publisher.publishSearchExecuted(SearchExecutedEvent.of("klima", 0));
        }).doesNotThrowAnyException();
    }
}
