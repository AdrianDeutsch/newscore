package de.newscore.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Default {@link EventPublisher} used when Kafka is not enabled. Events are dropped (logged at
 * debug) so the application runs without a broker.
 */
@Component
@Profile("!kafka")
public class NoOpEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpEventPublisher.class);

    @Override
    public void publishArticleEvent(ArticleEvent event) {
        log.debug("Event bus disabled — dropping article event {}", event);
    }

    @Override
    public void publishSearchExecuted(SearchExecutedEvent event) {
        log.debug("Event bus disabled — dropping search event {}", event);
    }
}
