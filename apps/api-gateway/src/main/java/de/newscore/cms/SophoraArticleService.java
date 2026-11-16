package de.newscore.cms;

import de.newscore.config.CachingConfig;
import de.newscore.domain.Article;
import de.newscore.domain.ArticleConnection;
import de.newscore.service.ArticleService;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * {@link ArticleService} backed by the Sophora CMS HTTP API (see ADR-010). Active under the
 * {@code sophora} profile; the in-memory mock is used otherwise.
 *
 * <p>Demonstrates the swappable data source promised by ADR-002: the GraphQL resolvers are
 * unchanged — only this implementation replaces the mock. Filtering and pagination are delegated to
 * the CMS via query parameters. Reads are cached with Caffeine (ADR-003), which is where the cache
 * pays off: every miss is a network call.</p>
 *
 * <p>The HTTP contract ({@code /articles}, {@code /articles/{id}}) is defined by this project and
 * only modelled on a CMS delivery API. It has not been run against a real Sophora installation.</p>
 */
@Service
@Profile("sophora")
public class SophoraArticleService implements ArticleService {

    private static final int FULL_CORPUS_LIMIT = 1000;

    private final RestClient restClient;

    public SophoraArticleService(RestClient.Builder builder,
                                 @Value("${newscore.sophora.base-url:http://sophora:8080}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    @Cacheable(CachingConfig.ARTICLE_CACHE)
    public Optional<Article> findById(String id) {
        try {
            SophoraArticleDto dto = restClient.get()
                    .uri("/articles/{id}", id)
                    .retrieve()
                    .body(SophoraArticleDto.class);
            return Optional.ofNullable(dto).map(SophoraArticleDto::toArticle);
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        }
    }

    @Override
    @Cacheable(CachingConfig.ARTICLES_CACHE)
    public ArticleConnection findArticles(String categorySlug, int limit, int offset) {
        SophoraPageDto page = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/articles")
                        .queryParamIfPresent("category", Optional.ofNullable(categorySlug))
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .build())
                .retrieve()
                .body(SophoraPageDto.class);
        if (page == null) {
            return new ArticleConnection(List.of(), 0, false);
        }
        List<Article> nodes = page.nodes().stream().map(SophoraArticleDto::toArticle).toList();
        return new ArticleConnection(nodes, page.totalCount(), page.hasNextPage());
    }

    @Override
    public List<Article> findAll() {
        return findArticles(null, FULL_CORPUS_LIMIT, 0).nodes();
    }
}
