package de.newscore.integration.search;

import static org.assertj.core.api.Assertions.assertThat;

import de.newscore.NewsCoreApplication;
import de.newscore.domain.Article;
import de.newscore.domain.SearchResult;
import de.newscore.search.ArticleIndexer;
import de.newscore.search.ElasticsearchSearchService;
import de.newscore.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration test for the ElasticSearch-backed search, running against a real ElasticSearch via
 * Testcontainers. {@code @ServiceConnection} auto-configures {@code spring.elasticsearch.uris} from
 * the started container.
 */
@SpringBootTest(classes = NewsCoreApplication.class)
@ActiveProfiles("elasticsearch")
@Testcontainers(disabledWithoutDocker = true)
@Tag("integration")
class ElasticsearchSearchIntegrationTest {

    @Container
    @ServiceConnection
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.13.4"))
            .withEnv("xpack.security.enabled", "false");

    @Autowired
    private SearchService searchService;

    @Autowired
    private ArticleIndexer indexer;

    @BeforeEach
    void reindex() {
        indexer.indexAll();
    }

    @Test
    @DisplayName("the SearchService is the ElasticSearch implementation under this profile")
    void searchService_isElasticsearchImplementation() {
        assertThat(searchService).isInstanceOf(ElasticsearchSearchService.class);
    }

    @Test
    @DisplayName("search() matches across title, teaser and tags")
    void search_findsMatchesAcrossFields() {
        SearchResult result = searchService.search("klima", 10);

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.results()).extracting(Article::id).containsExactlyInAnyOrder("1", "6");
    }

    @Test
    @DisplayName("search() returns no matches for an unknown term")
    void search_unknownTerm_returnsEmpty() {
        assertThat(searchService.search("supercalifragilistic", 10).totalCount()).isZero();
    }

    @Test
    @DisplayName("search() caps results at the requested limit while reporting the full total")
    void search_capsResultsAtLimit() {
        SearchResult result = searchService.search("klima", 1);

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.results()).hasSize(1);
    }
}
