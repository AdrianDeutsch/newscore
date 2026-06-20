package de.newscore.kafka;

import de.newscore.cache.CachePurger;
import de.newscore.config.CachingConfig;
import de.newscore.search.ArticleIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link ArticleEvent}s and keeps derived state in sync (Epic 4): it evicts the in-process
 * caches (active invalidation, complementing the TTL from ADR-003), purges the Varnish edge cache
 * (ADR-008) and, when the ElasticSearch profile is active, re-indexes or removes the affected
 * article.
 *
 * <p>The {@link ArticleIndexer} dependency is optional ({@link ObjectProvider}) so this consumer
 * also works in a Kafka-only profile without ElasticSearch — it then just evicts/purges.</p>
 */
@Component
@Profile("kafka")
public class ArticleEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ArticleEventConsumer.class);

    private final CacheManager cacheManager;
    private final ObjectProvider<ArticleIndexer> indexerProvider;
    private final CachePurger cachePurger;

    public ArticleEventConsumer(CacheManager cacheManager,
                                ObjectProvider<ArticleIndexer> indexerProvider,
                                CachePurger cachePurger) {
        this.cacheManager = cacheManager;
        this.indexerProvider = indexerProvider;
        this.cachePurger = cachePurger;
    }

    @KafkaListener(topics = KafkaTopics.ARTICLE_EVENTS, groupId = "${spring.application.name}-article-indexer")
    public void onArticleEvent(ArticleEvent event) {
        log.info("Handling {} for article {}", event.type(), event.articleId());
        evictArticleCaches();
        cachePurger.purgeArticle(event.articleId());
        cachePurger.purgeHomepage();

        ArticleIndexer indexer = indexerProvider.getIfAvailable();
        if (indexer == null) {
            return;
        }
        if (event.type() == ArticleEventType.DELETED) {
            indexer.delete(event.articleId());
        } else {
            indexer.indexOne(event.articleId());
        }
    }

    private void evictArticleCaches() {
        clear(CachingConfig.ARTICLE_CACHE);
        clear(CachingConfig.ARTICLES_CACHE);
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
