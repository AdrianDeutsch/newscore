package de.newscore.domain;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * A published editorial article.
 *
 * <p>The article references its author and category by id rather than embedding them. The nested
 * GraphQL fields {@code author} and {@code category} are resolved through DataLoader batch mappings
 * (see {@code ArticleController}) to avoid the N+1 query problem.</p>
 *
 * @param authorId   id of the authoring {@link Author}
 * @param categoryId id of the owning {@link Category}
 * @param tags       free-form tags, never {@code null}
 * @param imageUrl   teaser image URL (optional, may be {@code null})
 */
public record Article(
        String id,
        String title,
        String teaser,
        String body,
        String authorId,
        String categoryId,
        OffsetDateTime publishedAt,
        List<String> tags,
        String imageUrl) {
}
