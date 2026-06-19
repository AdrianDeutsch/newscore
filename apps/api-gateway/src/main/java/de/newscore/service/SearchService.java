package de.newscore.service;

import de.newscore.domain.SearchResult;

/**
 * Full-text search over articles.
 *
 * <p>The current implementation performs an in-memory substring match. A future implementation will
 * delegate to ElasticSearch (Epic 3) without changing this contract.</p>
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
