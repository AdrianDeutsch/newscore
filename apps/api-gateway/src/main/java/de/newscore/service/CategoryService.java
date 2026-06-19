package de.newscore.service;

import de.newscore.domain.Category;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Read access to categories.
 *
 * <p>{@link #findByIds(Collection)} supports DataLoader batch loading of the nested
 * {@code Article.category} field, avoiding N+1 lookups (see ADR-002).</p>
 */
public interface CategoryService {

    /**
     * Finds a single category by id.
     *
     * @param id the category id
     * @return the category, or {@link Optional#empty()} if none exists
     */
    Optional<Category> findById(String id);

    /**
     * Batch-loads categories for the given ids.
     *
     * @param ids the category ids to resolve
     * @return a map from id to category; ids without a match are absent from the map
     */
    Map<String, Category> findByIds(Collection<String> ids);
}
