package de.newscore.domain;

import java.util.List;

/**
 * A page of articles with offset-based pagination metadata. The field names mirror the GraphQL
 * {@code ArticleConnection} type so Spring GraphQL can map them directly.
 *
 * @param nodes       the articles on this page
 * @param totalCount  total number of matching articles across all pages
 * @param hasNextPage whether another page exists after this one
 */
public record ArticleConnection(List<Article> nodes, int totalCount, boolean hasNextPage) {
}
