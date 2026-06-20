package de.newscore.kafka;

/**
 * Publishes domain events to the event bus.
 *
 * <p>Strategy abstraction: the default profile uses a no-op implementation so the application runs
 * without a Kafka broker (and unit tests stay fast); the {@code kafka} profile activates the Kafka
 * implementation (see ADR-005). Mirrors the search-backend approach from ADR-004.</p>
 */
public interface EventPublisher {

    /**
     * Publishes an article lifecycle event.
     *
     * @param event the event to publish
     */
    void publishArticleEvent(ArticleEvent event);

    /**
     * Publishes a search-executed analytics event.
     *
     * @param event the event to publish
     */
    void publishSearchExecuted(SearchExecutedEvent event);

    /**
     * Publishes a page-view analytics event.
     *
     * @param event the event to publish
     */
    void publishPageView(PageViewEvent event);
}
