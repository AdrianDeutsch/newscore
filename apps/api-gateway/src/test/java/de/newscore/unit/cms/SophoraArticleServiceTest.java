package de.newscore.unit.cms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.newscore.cms.SophoraArticleService;
import de.newscore.domain.Article;
import de.newscore.domain.ArticleConnection;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SophoraArticleServiceTest {

    private static final String BASE_URL = "http://sophora:8080";

    private static final String ARTICLE_JSON = """
            {"id":"1","title":"EU-Klimapaket","teaser":"Teaser","body":"Body","authorId":"a1",
             "categoryId":"c1","publishedAt":"2026-06-18T07:15:00Z","tags":["klima"],
             "imageUrl":"https://example.test/x.jpg"}
            """;

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private SophoraArticleService service;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new SophoraArticleService(builder, BASE_URL);
    }

    @Test
    @DisplayName("findById maps the CMS article to the domain model")
    void findById_mapsArticle() {
        server.expect(requestTo(BASE_URL + "/articles/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(ARTICLE_JSON, MediaType.APPLICATION_JSON));

        Optional<Article> article = service.findById("1");

        assertThat(article).isPresent();
        assertThat(article.get().title()).isEqualTo("EU-Klimapaket");
        assertThat(article.get().publishedAt()).isEqualTo(OffsetDateTime.parse("2026-06-18T07:15:00Z"));
        assertThat(article.get().tags()).containsExactly("klima");
        server.verify();
    }

    @Test
    @DisplayName("findById returns empty on a 404 from the CMS")
    void findById_notFound_returnsEmpty() {
        server.expect(requestTo(BASE_URL + "/articles/unknown"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(service.findById("unknown")).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("findArticles delegates filter/paging to the CMS and maps the page")
    void findArticles_mapsPage() {
        String pageJson = "{\"nodes\":[" + ARTICLE_JSON + "],\"totalCount\":2,\"hasNextPage\":false}";
        server.expect(requestTo(BASE_URL + "/articles?category=politik&limit=20&offset=0"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(pageJson, MediaType.APPLICATION_JSON));

        ArticleConnection connection = service.findArticles("politik", 20, 0);

        assertThat(connection.totalCount()).isEqualTo(2);
        assertThat(connection.hasNextPage()).isFalse();
        assertThat(connection.nodes()).extracting(Article::id).containsExactly("1");
        server.verify();
    }

    @Test
    @DisplayName("findAll requests the full corpus without a category filter")
    void findAll_requestsFullCorpus() {
        String pageJson = "{\"nodes\":[" + ARTICLE_JSON + "],\"totalCount\":1,\"hasNextPage\":false}";
        server.expect(requestTo(BASE_URL + "/articles?limit=1000&offset=0"))
                .andRespond(withSuccess(pageJson, MediaType.APPLICATION_JSON));

        assertThat(service.findAll()).extracting(Article::id).containsExactly("1");
        server.verify();
    }
}
