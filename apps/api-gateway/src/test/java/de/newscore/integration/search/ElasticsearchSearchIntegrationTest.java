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
import org.springframework.boot.test.web.client.TestRestTemplate;
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
@SpringBootTest(
        classes = NewsCoreApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.endpoint.health.show-components=always")
@ActiveProfiles("elasticsearch")
@Testcontainers(disabledWithoutDocker = true)
@Tag("integration")
class ElasticsearchSearchIntegrationTest {

    // Same version as docker-compose.yml, and the version of the Java client that Spring Boot
    // manages (elasticsearch-client.version). A server older than the client still answers
    // searches, but the client cannot decode its cluster-health response, so the health check fails.
    @Container
    @ServiceConnection
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.18.8"))
            .withEnv("xpack.security.enabled", "false");

    @Autowired
    private SearchService searchService;

    @Autowired
    private ArticleIndexer indexer;

    @Autowired
    private TestRestTemplate http;

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
    @DisplayName("under this profile the health endpoint checks ElasticSearch")
    void health_includesElasticsearch() {
        var health = http.getForEntity("/actuator/health/elasticsearch", String.class);

        assertThat(health.getStatusCode().value()).isEqualTo(200);
        assertThat(health.getBody()).contains("\"status\":\"UP\"");
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
