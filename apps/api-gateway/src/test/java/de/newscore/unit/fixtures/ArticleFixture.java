package de.newscore.unit.fixtures;

import de.newscore.domain.Article;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Test data builder for {@link Article}. Provides sensible defaults with targeted overrides so each
 * test only states what is relevant to it.
 */
public final class ArticleFixture {

    private String id = "1";
    private String title = "EU einigt sich auf neues Klimapaket";
    private String teaser = "Die Mitgliedstaaten haben sich geeinigt.";
    private String body = "Brüssel — ausführlicher Artikeltext über das Klimapaket.";
    private String authorId = "a1";
    private String categoryId = "c1";
    private OffsetDateTime publishedAt = OffsetDateTime.parse("2026-06-18T07:15:00Z");
    private List<String> tags = List.of("klima", "eu", "politik");
    private String imageUrl = "https://example.test/eu-klima.jpg";

    private ArticleFixture() {
    }

    public static ArticleFixture anArticle() {
        return new ArticleFixture();
    }

    /** A fully populated, published article with default values. */
    public static Article published() {
        return anArticle().build();
    }

    public ArticleFixture withId(String id) {
        this.id = id;
        return this;
    }

    public ArticleFixture withTitle(String title) {
        this.title = title;
        return this;
    }

    public ArticleFixture withTeaser(String teaser) {
        this.teaser = teaser;
        return this;
    }

    public ArticleFixture withBody(String body) {
        this.body = body;
        return this;
    }

    public ArticleFixture withAuthorId(String authorId) {
        this.authorId = authorId;
        return this;
    }

    public ArticleFixture withCategoryId(String categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    public ArticleFixture withPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
        return this;
    }

    public ArticleFixture withTags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public ArticleFixture withImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    public Article build() {
        return new Article(id, title, teaser, body, authorId, categoryId, publishedAt, tags, imageUrl);
    }
}
