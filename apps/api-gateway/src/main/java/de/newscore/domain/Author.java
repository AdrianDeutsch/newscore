package de.newscore.domain;

/**
 * An editorial author.
 *
 * @param id    stable identifier
 * @param name  display name
 * @param email contact address (optional, may be {@code null})
 * @param bio   short biography (optional, may be {@code null})
 */
public record Author(String id, String name, String email, String bio) {
}
