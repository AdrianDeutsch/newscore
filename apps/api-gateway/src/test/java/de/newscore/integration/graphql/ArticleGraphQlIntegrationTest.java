package de.newscore.integration.graphql;

import de.newscore.NewsCoreApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;

/**
 * End-to-end GraphQL test that boots the full application context and drives the schema through
 * {@link GraphQlTester}. It verifies query wiring, the custom {@code DateTime} scalar and the
 * batched resolution of nested {@code author}/{@code category} fields.
 */
@SpringBootTest(classes = NewsCoreApplication.class)
@AutoConfigureGraphQlTester
@Tag("integration")
class ArticleGraphQlIntegrationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    @DisplayName("article(id) resolves the article with batched author and category")
    void article_resolvesNestedAuthorAndCategory() {
        // language=GraphQL
        String document = """
                query {
                  article(id: "1") {
                    id
                    title
                    author { id name }
                    category { slug }
                    publishedAt
                    tags
                  }
                }
                """;

        graphQlTester.document(document)
                .execute()
                .path("article.id").entity(String.class).isEqualTo("1")
                .path("article.author.name").entity(String.class).isEqualTo("Lena Hoffmann")
                .path("article.category.slug").entity(String.class).isEqualTo("politik")
                .path("article.publishedAt").entity(String.class).isEqualTo("2026-06-18T07:15:00.000Z")
                .path("article.tags").entityList(String.class).hasSize(3);
    }

    @Test
    @DisplayName("article(id) returns null for an unknown id")
    void article_unknownId_returnsNull() {
        graphQlTester.document("query { article(id: \"does-not-exist\") { id } }")
                .execute()
                .path("article").valueIsNull();
    }

    @Test
    @DisplayName("articles(limit) returns a paginated connection")
    void articles_returnsPaginatedConnection() {
        // language=GraphQL
        String document = """
                query {
                  articles(limit: 3) {
                    totalCount
                    hasNextPage
                    nodes { id author { name } }
                  }
                }
                """;

        graphQlTester.document(document)
                .execute()
                .path("articles.totalCount").entity(Integer.class).isEqualTo(8)
                .path("articles.hasNextPage").entity(Boolean.class).isEqualTo(true)
                .path("articles.nodes").entityList(Object.class).hasSize(3);
    }

    @Test
    @DisplayName("search(query) returns matching articles newest-first")
    void search_returnsMatches() {
        // "klima" matches article 1 (title/tags) and article 6 (teaser "Klima- und KI-Startups").
        // language=GraphQL
        String document = """
                query {
                  search(query: "klima") {
                    totalCount
                    results { id title }
                  }
                }
                """;

        graphQlTester.document(document)
                .execute()
                .path("search.totalCount").entity(Integer.class).isEqualTo(2)
                .path("search.results[0].id").entity(String.class).isEqualTo("1");
    }
}
