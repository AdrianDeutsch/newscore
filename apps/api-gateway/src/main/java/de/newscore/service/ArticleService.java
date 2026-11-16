package de.newscore.service;

import de.newscore.domain.Article;
import de.newscore.domain.ArticleConnection;
import java.util.List;
import java.util.Optional;

/**
 * Read access to editorial articles.
 *
 * <p>This abstraction decouples the GraphQL resolvers from the concrete data source. The default
 * implementation serves in-memory demo data; under the {@code sophora} profile an HTTP adapter
 * takes over without any change to the resolver layer (see ADR-002, ADR-010).</p>
 */
public interface ArticleService {

    /**
     * Finds a single article by its id.
     *
     * @param id the article id
     * @return the article, or {@link Optional#empty()} if none exists
     */
    Optional<Article> findById(String id);

    /**
     * Returns a page of articles, optionally filtered by category slug.
     *
     * @param categorySlug category slug to filter by, or {@code null}/blank for all categories
     * @param limit        maximum number of articles to return (clamped to a sane range)
     * @param offset       number of leading articles to skip
     * @return a connection containing the page and pagination metadata
     */
    ArticleConnection findArticles(String categorySlug, int limit, int offset);

    /**
     * Returns the full article corpus, most recent first. Used by the search service.
     *
     * @return all known articles
     */
    List<Article> findAll();
}
