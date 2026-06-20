package de.newscore.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

/**
 * Analytics event emitted when a user views a page. Produced by the frontend (via the gateway's
 * pageview endpoint), published to {@link KafkaTopics#USER_EVENTS} and consumed by the
 * analytics-service. {@code occurredAt} is serialized as an ISO-8601 string for polyglot consumers.
 *
 * @param path       the viewed route, e.g. {@code /article/1}
 * @param occurredAt event timestamp
 */
public record PageViewEvent(
        String path,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant occurredAt) {

    /** Convenience factory using the current instant. */
    public static PageViewEvent of(String path) {
        return new PageViewEvent(path, Instant.now());
    }
}
