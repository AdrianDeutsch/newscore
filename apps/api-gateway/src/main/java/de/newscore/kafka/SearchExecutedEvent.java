package de.newscore.kafka;

import java.time.Instant;
import java.util.UUID;

/**
 * Analytics event emitted whenever a search query is executed. Published to
 * {@link KafkaTopics#SEARCH_EVENTS} and consumed by the analytics-service.
 *
 * <p>On the wire {@code occurredAt} is an ISO-8601 string (not an epoch number) so polyglot
 * consumers (e.g. the Node analytics-service writing a {@code timestamptz}) parse it directly.</p>
 *
 * <p>{@code eventId} identifies this one event. The analytics-service stores it under a unique
 * constraint, so a redelivered message does not produce a second row (ADR-013). It is created here,
 * not derived from the Kafka offset: offsets start again at 0 when a topic is recreated.</p>
 *
 * @param eventId     unique id of this event (random UUID)
 * @param query       the search term
 * @param resultCount number of matches reported to the user
 * @param occurredAt  event timestamp
 */
public record SearchExecutedEvent(
        String eventId,
        String query,
        int resultCount,
        Instant occurredAt) {

    /** Creates an event with a new id and the current instant. */
    public static SearchExecutedEvent of(String query, int resultCount) {
        return new SearchExecutedEvent(UUID.randomUUID().toString(), query, resultCount, Instant.now());
    }
}
