package de.newscore.unit.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.newscore.domain.Article;
import de.newscore.domain.ArticleConnection;
import de.newscore.service.InMemoryArticleService;
import de.newscore.service.InMemoryCategoryService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryArticleServiceTest {

    private InMemoryArticleService service;

    @BeforeEach
    void setUp() {
        // Use the real in-memory category service so slug-based filtering is exercised end-to-end.
        this.service = new InMemoryArticleService(new InMemoryCategoryService());
    }

    @Test
    @DisplayName("findById() returns the article when the id exists")
    void findById_existingId_returnsArticle() {
        Optional<Article> result = service.findById("1");

        assertThat(result).isPresent();
        assertThat(result.get().title()).isEqualTo("EU einigt sich auf neues Klimapaket");
    }

    @Test
    @DisplayName("findById() returns empty for an unknown id")
    void findById_unknownId_returnsEmpty() {
        assertThat(service.findById("does-not-exist")).isEmpty();
    }

    @Test
    @DisplayName("findAll() returns articles sorted newest-first")
    void findAll_returnsNewestFirst() {
        assertThat(service.findAll())
                .extracting(Article::id)
                .containsExactly("1", "3", "2", "4", "5", "6", "7", "8");
    }

    @Test
    @DisplayName("findArticles() applies offset/limit pagination and reports hasNextPage")
    void findArticles_paginates() {
        ArticleConnection page = service.findArticles(null, 3, 0);

        assertThat(page.nodes()).extracting(Article::id).containsExactly("1", "3", "2");
        assertThat(page.totalCount()).isEqualTo(8);
        assertThat(page.hasNextPage()).isTrue();
    }

    @Test
    @DisplayName("findArticles() on the last page reports hasNextPage = false")
    void findArticles_lastPage_hasNoNextPage() {
        ArticleConnection page = service.findArticles(null, 5, 6);

        assertThat(page.nodes()).extracting(Article::id).containsExactly("7", "8");
        assertThat(page.totalCount()).isEqualTo(8);
        assertThat(page.hasNextPage()).isFalse();
    }

    @Test
    @DisplayName("findArticles() filters by category slug")
    void findArticles_filtersByCategorySlug() {
        ArticleConnection page = service.findArticles("politik", 20, 0);

        assertThat(page.nodes()).extracting(Article::id).containsExactly("1", "5");
        assertThat(page.totalCount()).isEqualTo(2);
        assertThat(page.hasNextPage()).isFalse();
    }

    @Test
    @DisplayName("findArticles() returns an empty page for an unknown category")
    void findArticles_unknownCategory_returnsEmpty() {
        ArticleConnection page = service.findArticles("gibtsnicht", 20, 0);

        assertThat(page.nodes()).isEmpty();
        assertThat(page.totalCount()).isZero();
        assertThat(page.hasNextPage()).isFalse();
    }

    @Test
    @DisplayName("findArticles() clamps an offset beyond the total to an empty page")
    void findArticles_offsetBeyondTotal_returnsEmptyPage() {
        ArticleConnection page = service.findArticles(null, 20, 100);

        assertThat(page.nodes()).isEmpty();
        assertThat(page.totalCount()).isEqualTo(8);
        assertThat(page.hasNextPage()).isFalse();
    }

    @Test
    @DisplayName("findArticles() with limit 0 returns no nodes but signals more pages")
    void findArticles_zeroLimit_returnsEmptyWithNextPage() {
        ArticleConnection page = service.findArticles(null, 0, 0);

        assertThat(page.nodes()).isEmpty();
        assertThat(page.totalCount()).isEqualTo(8);
        assertThat(page.hasNextPage()).isTrue();
    }
}
