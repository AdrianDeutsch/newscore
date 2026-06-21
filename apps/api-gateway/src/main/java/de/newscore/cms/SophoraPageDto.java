package de.newscore.cms;

import java.util.List;

/**
 * Wire representation of a paged article response from the Sophora CMS HTTP API.
 *
 * @param nodes       the articles on this page
 * @param totalCount  total matches across all pages
 * @param hasNextPage whether a further page exists
 */
public record SophoraPageDto(List<SophoraArticleDto> nodes, int totalCount, boolean hasNextPage) {
}
