package de.newscore.service;

import de.newscore.domain.SearchResult;

/**
 * Full-text search over articles.
 *
 * <p>The default implementation performs an in-memory substring match; under the
 * {@code elasticsearch} profile the ElasticSearch implementation takes over (see ADR-004).</p>
 */
public interface SearchService {

    /**
     * Searches articles by a free-text query.
     *
     * @param query the search term; blank queries yield an empty result
     * @param limit maximum number of results to return
     * @return the search result, capped by {@code limit}
     */
    SearchResult search(String query, int limit);
}
