package de.newscore.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application-level caching with Caffeine (see ADR-003).
 *
 * <p>Provides short-lived in-process caches for the hot read paths. Varnish remains the primary
 * edge cache; this second tier reduces repeated resolver work on edge cache misses. TTL is kept
 * short so a missing active invalidation (planned for the Kafka iteration) cannot serve stale
 * content for long.</p>
 */
@Configuration
@EnableCaching
public class CachingConfig {

    /** Cache name for {@code ArticleService#findById}. */
    public static final String ARTICLE_CACHE = "article";

    /** Cache name for {@code ArticleService#findArticles}. */
    public static final String ARTICLES_CACHE = "articles";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(ARTICLE_CACHE, ARTICLES_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofMinutes(5)));
        return cacheManager;
    }
}
