package de.newscore.kafka;

import java.time.Instant;
import java.util.UUID;

/**
 * Analytics event emitted when a user views a page. Produced by the frontend (via the gateway's
 * pageview endpoint), published to {@link KafkaTopics#USER_EVENTS} and consumed by the
 * analytics-service. On the wire {@code occurredAt} is an ISO-8601 string for polyglot consumers.
 *
 * <p>{@code eventId} makes storing the event idempotent; see {@link SearchExecutedEvent}.</p>
 *
 * @param eventId    unique id of this event (random UUID)
 * @param path       the viewed route, e.g. {@code /article/1}
 * @param occurredAt event timestamp
 */
public record PageViewEvent(
        String eventId,
        String path,
        Instant occurredAt) {

    /** Creates an event with a new id and the current instant. */
    public static PageViewEvent of(String path) {
        return new PageViewEvent(UUID.randomUUID().toString(), path, Instant.now());
    }
}
