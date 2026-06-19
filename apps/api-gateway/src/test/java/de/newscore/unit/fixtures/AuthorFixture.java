package de.newscore.unit.fixtures;

import de.newscore.domain.Author;

/**
 * Test data factory for {@link Author}.
 */
public final class AuthorFixture {

    private AuthorFixture() {
    }

    /** An author with default values. */
    public static Author lena() {
        return new Author("a1", "Lena Hoffmann", "lena.hoffmann@newscore.de", "Politikredakteurin.");
    }

    /** An author with the given id and a derived name. */
    public static Author withId(String id) {
        return new Author(id, "Author " + id, id + "@newscore.de", "Bio " + id);
    }
}
