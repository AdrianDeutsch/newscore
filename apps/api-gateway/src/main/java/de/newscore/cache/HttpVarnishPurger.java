package de.newscore.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * {@link CachePurger} that sends HTTP {@code PURGE} requests to Varnish (see the PURGE handling in
 * {@code infrastructure/varnish/default.vcl}). Active when {@code newscore.varnish.purge.enabled=true}.
 *
 * <p>Purge failures are swallowed (logged): a missed invalidation falls back to the TTL and must
 * never break event consumption.</p>
 */
@Component
@ConditionalOnProperty(name = "newscore.varnish.purge.enabled", havingValue = "true")
public class HttpVarnishPurger implements CachePurger {

    private static final Logger log = LoggerFactory.getLogger(HttpVarnishPurger.class);
    private static final HttpMethod PURGE = HttpMethod.valueOf("PURGE");

    private final RestClient restClient;

    public HttpVarnishPurger(RestClient.Builder builder,
                             @Value("${newscore.varnish.base-url:http://varnish:80}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public void purgeArticle(String articleId) {
        purge("/article/" + articleId);
    }

    @Override
    public void purgeHomepage() {
        purge("/");
    }

    private void purge(String path) {
        try {
            restClient.method(PURGE).uri(path).retrieve().toBodilessEntity();
            log.info("Purged Varnish path {}", path);
        } catch (RuntimeException ex) {
            log.warn("Varnish purge failed for {}: {}", path, ex.getMessage());
        }
    }
}
