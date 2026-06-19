package de.newscore.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

/**
 * Analytics event emitted whenever a search query is executed. Published to
 * {@link KafkaTopics#SEARCH_EVENTS} and consumed by the analytics-service.
 *
 * <p>{@code occurredAt} is serialized as an ISO-8601 string (not an epoch number) so polyglot
 * consumers (e.g. the Node analytics-service writing a {@code timestamptz}) parse it directly.</p>
 *
 * @param query       the search term
 * @param resultCount number of matches reported to the user
 * @param occurredAt  event timestamp
 */
public record SearchExecutedEvent(
        String query,
        int resultCount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant occurredAt) {

    /** Convenience factory using the current instant. */
    public static SearchExecutedEvent of(String query, int resultCount) {
        return new SearchExecutedEvent(query, resultCount, Instant.now());
    }
}
