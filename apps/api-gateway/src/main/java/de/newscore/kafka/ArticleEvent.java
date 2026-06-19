package de.newscore.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

/**
 * Domain event signalling a change to an article. Published to {@link KafkaTopics#ARTICLE_EVENTS}
 * with the article id as the partition key (ordering per article).
 *
 * <p>{@code occurredAt} is serialized as an ISO-8601 string so polyglot consumers parse it directly.</p>
 *
 * @param type       the kind of change
 * @param articleId  the affected article id
 * @param occurredAt event timestamp
 */
public record ArticleEvent(
        ArticleEventType type,
        String articleId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant occurredAt) {

    /** Convenience factory using the current instant. */
    public static ArticleEvent of(ArticleEventType type, String articleId) {
        return new ArticleEvent(type, articleId, Instant.now());
    }
}
