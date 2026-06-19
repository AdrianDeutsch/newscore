package de.newscore.service;

import de.newscore.domain.Author;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Read access to authors.
 *
 * <p>{@link #findByIds(Collection)} exists specifically to support DataLoader batch loading of the
 * nested {@code Article.author} field, avoiding N+1 lookups (see ADR-002).</p>
 */
public interface AuthorService {

    /**
     * Finds a single author by id.
     *
     * @param id the author id
     * @return the author, or {@link Optional#empty()} if none exists
     */
    Optional<Author> findById(String id);

    /**
     * Batch-loads authors for the given ids.
     *
     * @param ids the author ids to resolve
     * @return a map from id to author; ids without a match are absent from the map
     */
    Map<String, Author> findByIds(Collection<String> ids);
}
