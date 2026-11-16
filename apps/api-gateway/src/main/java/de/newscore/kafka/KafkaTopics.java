package de.newscore.kafka;

/**
 * Central registry of Kafka topic names (see ADR-005). Keeping them in one place avoids
 * stringly-typed drift between producers, consumers and topic declarations.
 */
public final class KafkaTopics {

    /** Editorial lifecycle events (key = article id). Consumed for ES indexing + cache eviction. */
    public static final String ARTICLE_EVENTS = "newscore.article.events";

    /** Suffix appended to a topic name to form its dead-letter topic. */
    public static final String DLT_SUFFIX = ".DLT";

    /** Dead-letter topic for {@link #ARTICLE_EVENTS} (poison / unprocessable records). */
    public static final String ARTICLE_EVENTS_DLT = ARTICLE_EVENTS + DLT_SUFFIX;

    /** Search analytics events, consumed by the analytics-service. */
    public static final String SEARCH_EVENTS = "newscore.search.events";

    /** User page-view events (produced by the frontend), consumed by the analytics-service. */
    public static final String USER_EVENTS = "newscore.user.events";

    private KafkaTopics() {
    }
}
