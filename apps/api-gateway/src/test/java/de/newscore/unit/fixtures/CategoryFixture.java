package de.newscore.unit.fixtures;

import de.newscore.domain.Category;

/**
 * Test data factory for {@link Category}.
 */
public final class CategoryFixture {

    private CategoryFixture() {
    }

    /** The "Politik" category (id {@code c1}, slug {@code politik}). */
    public static Category politik() {
        return new Category("c1", "Politik", "politik");
    }

    /** A category with the given id, name and slug. */
    public static Category of(String id, String name, String slug) {
        return new Category(id, name, slug);
    }
}
