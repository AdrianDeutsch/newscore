package de.newscore.integration.graphql;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import de.newscore.NewsCoreApplication;
import de.newscore.service.AuthorService;
import de.newscore.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Pins the N+1 claim: a list query that selects the nested {@code author} and {@code category} of
 * every article must load each of them with one batched call, not one call per article.
 */
@SpringBootTest(classes = NewsCoreApplication.class)
@AutoConfigureGraphQlTester
@Tag("integration")
class BatchLoadingIntegrationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoSpyBean
    private AuthorService authorService;

    @MockitoSpyBean
    private CategoryService categoryService;

    @Test
    @DisplayName("eight articles with author and category cost one batch call each, no per-article lookups")
    void articles_nestedFields_areLoadedInOneBatchEach() {
        // language=GraphQL
        String document = """
                query {
                  articles(limit: 8) {
                    nodes { id author { name } category { name } }
                  }
                }
                """;

        graphQlTester.document(document)
                .execute()
                .path("articles.nodes").entityList(Object.class).hasSize(8);

        verify(authorService, times(1)).findByIds(anyCollection());
        verify(authorService, never()).findById(anyString());
        // The in-memory article source resolves the category filter through CategoryService as
        // well, but only when a filter is given — this query has none.
        verify(categoryService, times(1)).findByIds(anyCollection());
        verify(categoryService, never()).findById(anyString());
    }
}
