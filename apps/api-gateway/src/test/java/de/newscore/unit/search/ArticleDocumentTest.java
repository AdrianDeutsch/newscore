package de.newscore.unit.search;

import static org.assertj.core.api.Assertions.assertThat;

import de.newscore.domain.Article;
import de.newscore.search.ArticleDocument;
import de.newscore.unit.fixtures.ArticleFixture;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArticleDocumentTest {

    @Test
    @DisplayName("from()/toArticle() round-trips all fields")
    void roundTrip_preservesAllFields() {
        Article original = ArticleFixture.published();

        Article restored = ArticleDocument.from(original).toArticle();

        assertThat(restored).isEqualTo(original);
    }

    @Test
    @DisplayName("from() maps every field onto the document")
    void from_mapsAllFields() {
        Article article = ArticleFixture.anArticle()
                .withId("42").withTitle("Titel").withAuthorId("a9").withCategoryId("c9").build();

        ArticleDocument document = ArticleDocument.from(article);

        assertThat(document.getId()).isEqualTo("42");
        assertThat(document.getTitle()).isEqualTo("Titel");
        assertThat(document.getAuthorId()).isEqualTo("a9");
        assertThat(document.getCategoryId()).isEqualTo("c9");
        assertThat(document.getPublishedAt()).isEqualTo(article.publishedAt().toString());
        assertThat(document.getTags()).isEqualTo(article.tags());
    }

    @Test
    @DisplayName("getters reflect values set through setters")
    void settersAndGetters_roundTrip() {
        ArticleDocument document = new ArticleDocument();
        document.setId("1");
        document.setTitle("Title");
        document.setTeaser("Teaser");
        document.setBody("Body");
        document.setAuthorId("a1");
        document.setCategoryId("c1");
        document.setPublishedAt("2026-06-18T07:15:00Z");
        document.setTags(List.of("klima"));
        document.setImageUrl("https://example.test/x.jpg");

        assertThat(document.getId()).isEqualTo("1");
        assertThat(document.getTitle()).isEqualTo("Title");
        assertThat(document.getTeaser()).isEqualTo("Teaser");
        assertThat(document.getBody()).isEqualTo("Body");
        assertThat(document.getAuthorId()).isEqualTo("a1");
        assertThat(document.getCategoryId()).isEqualTo("c1");
        assertThat(document.getPublishedAt()).isEqualTo("2026-06-18T07:15:00Z");
        assertThat(document.getTags()).containsExactly("klima");
        assertThat(document.getImageUrl()).isEqualTo("https://example.test/x.jpg");
        assertThat(document.toArticle().id()).isEqualTo("1");
    }
}
