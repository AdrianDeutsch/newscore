package de.newscore.unit.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.newscore.domain.Article;
import de.newscore.domain.ArticleConnection;
import de.newscore.domain.Author;
import de.newscore.domain.Category;
import de.newscore.resolver.ArticleController;
import de.newscore.service.ArticleService;
import de.newscore.service.AuthorService;
import de.newscore.service.CategoryService;
import de.newscore.unit.fixtures.ArticleFixture;
import de.newscore.unit.fixtures.AuthorFixture;
import de.newscore.unit.fixtures.CategoryFixture;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleControllerTest {

    @Mock
    private ArticleService articleService;
    @Mock
    private AuthorService authorService;
    @Mock
    private CategoryService categoryService;
    @InjectMocks
    private ArticleController controller;

    @Test
    @DisplayName("article() returns the article when the id exists")
    void article_validId_returnsArticle() {
        Article expected = ArticleFixture.published();
        when(articleService.findById("1")).thenReturn(Optional.of(expected));

        assertThat(controller.article("1")).isEqualTo(expected);
    }

    @Test
    @DisplayName("article() returns null when the id is unknown")
    void article_unknownId_returnsNull() {
        when(articleService.findById("unknown")).thenReturn(Optional.empty());

        assertThat(controller.article("unknown")).isNull();
    }

    @Test
    @DisplayName("articles() delegates to the service with the given arguments")
    void articles_delegatesToService() {
        ArticleConnection connection = new ArticleConnection(List.of(ArticleFixture.published()), 1, false);
        when(articleService.findArticles("politik", 20, 0)).thenReturn(connection);

        assertThat(controller.articles("politik", 20, 0)).isSameAs(connection);
        verify(articleService).findArticles("politik", 20, 0);
    }

    @Test
    @DisplayName("author() batch-maps each article to its author")
    void author_batchMapping_associatesEachArticle() {
        Article first = ArticleFixture.anArticle().withId("1").withAuthorId("a1").build();
        Article second = ArticleFixture.anArticle().withId("2").withAuthorId("a2").build();
        Author lena = AuthorFixture.lena();
        Author other = AuthorFixture.withId("a2");
        when(authorService.findByIds(anyCollection())).thenReturn(Map.of("a1", lena, "a2", other));

        Map<Article, Author> result = controller.author(List.of(first, second));

        assertThat(result)
                .containsEntry(first, lena)
                .containsEntry(second, other)
                .hasSize(2);
    }

    @Test
    @DisplayName("category() batch-maps each article to its category")
    void category_batchMapping_associatesEachArticle() {
        Article article = ArticleFixture.anArticle().withId("1").withCategoryId("c1").build();
        Category politik = CategoryFixture.politik();
        when(categoryService.findByIds(anyCollection())).thenReturn(Map.of("c1", politik));

        Map<Article, Category> result = controller.category(List.of(article));

        assertThat(result).containsEntry(article, politik).hasSize(1);
    }
}
