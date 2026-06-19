package de.newscore.domain;

/**
 * A content category (ressort).
 *
 * @param id   stable identifier
 * @param name display name
 * @param slug URL-safe identifier used for filtering, e.g. {@code "politik"}
 */
public record Category(String id, String name, String slug) {
}
