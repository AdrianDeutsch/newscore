package de.newscore.cms;

import de.newscore.domain.Article;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Wire representation of an article as returned by the Sophora CMS HTTP API. Decouples the CMS's
 * JSON contract from the domain model; {@link #toArticle()} maps it to {@link Article}.
 *
 * @param publishedAt ISO-8601 date-time string
 */
public record SophoraArticleDto(
        String id,
        String title,
        String teaser,
        String body,
        String authorId,
        String categoryId,
        String publishedAt,
        List<String> tags,
        String imageUrl) {

    /** Maps this DTO to the domain {@link Article}. */
    public Article toArticle() {
        return new Article(
                id, title, teaser, body, authorId, categoryId,
                OffsetDateTime.parse(publishedAt),
                tags == null ? List.of() : tags,
                imageUrl);
    }
}
