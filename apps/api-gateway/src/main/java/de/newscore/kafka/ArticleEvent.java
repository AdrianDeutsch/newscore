package de.newscore.kafka;

import java.time.Instant;

/**
 * Domain event signalling a change to an article. Published to {@link KafkaTopics#ARTICLE_EVENTS}
 * with the article id as the partition key (ordering per article).
 *
 * <p>On the wire this is the Avro record of the same name ({@code AvroEventMapper});
 * {@code occurredAt} travels as an ISO-8601 string so polyglot consumers parse it directly.</p>
 *
 * @param type       the kind of change
 * @param articleId  the affected article id
 * @param occurredAt event timestamp
 */
public record ArticleEvent(
        ArticleEventType type,
        String articleId,
        Instant occurredAt) {

    /** Convenience factory using the current instant. */
    public static ArticleEvent of(ArticleEventType type, String articleId) {
        return new ArticleEvent(type, articleId, Instant.now());
    }
}
