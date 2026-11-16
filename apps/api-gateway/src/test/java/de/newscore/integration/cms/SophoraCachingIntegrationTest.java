package de.newscore.integration.cms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.newscore.cms.SophoraArticleService;
import de.newscore.config.CachingConfig;
import de.newscore.service.ArticleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

/**
 * The cache has to sit in front of the remote CMS, where a miss costs a network call. The test goes
 * through the Spring proxy (a plain {@code new SophoraArticleService(...)} would bypass caching).
 */
@RestClientTest(SophoraArticleService.class)
@Import(CachingConfig.class)
@ActiveProfiles("sophora")
@TestPropertySource(properties = "newscore.sophora.base-url=http://cms.test")
@Tag("integration")
class SophoraCachingIntegrationTest {

    private static final String ARTICLE_JSON = """
            {"id":"42","title":"T","teaser":"t","body":"b","authorId":"a1","categoryId":"c1",
             "publishedAt":"2026-06-18T07:15:00Z","tags":["x"],"imageUrl":null}
            """;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private MockRestServiceServer cms;

    @Test
    @DisplayName("a repeated article lookup is answered from the cache, the CMS is called once")
    void findById_secondCall_doesNotHitTheCms() {
        cms.expect(once(), requestTo("http://cms.test/articles/42"))
                .andRespond(withSuccess(ARTICLE_JSON, MediaType.APPLICATION_JSON));

        assertThat(articleService.findById("42")).isPresent();
        assertThat(articleService.findById("42")).isPresent();

        cms.verify();
    }

    @Test
    @DisplayName("a repeated list request with the same arguments is answered from the cache")
    void findArticles_sameArguments_doesNotHitTheCms() {
        cms.expect(once(), requestTo("http://cms.test/articles?category=politik&limit=5&offset=0"))
                .andRespond(withSuccess(
                        "{\"nodes\":[" + ARTICLE_JSON + "],\"totalCount\":1,\"hasNextPage\":false}",
                        MediaType.APPLICATION_JSON));

        assertThat(articleService.findArticles("politik", 5, 0).totalCount()).isEqualTo(1);
        assertThat(articleService.findArticles("politik", 5, 0).totalCount()).isEqualTo(1);

        cms.verify();
    }
}
