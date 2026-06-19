package de.newscore.domain;

import java.util.List;

/**
 * Result of a full-text search over articles. The field names mirror the GraphQL
 * {@code SearchResult} type.
 *
 * @param query      the original query string
 * @param totalCount number of matches (before the limit is applied)
 * @param results    the matching articles, capped by the requested limit
 */
public record SearchResult(String query, int totalCount, List<Article> results) {
}
