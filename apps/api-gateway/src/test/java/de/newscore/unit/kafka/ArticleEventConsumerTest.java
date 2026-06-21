package de.newscore.unit.kafka;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.newscore.cache.CachePurger;
import de.newscore.kafka.ArticleEventConsumer;
import de.newscore.kafka.avro.ArticleEvent;
import de.newscore.kafka.avro.ArticleEventType;
import de.newscore.search.ArticleIndexer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class ArticleEventConsumerTest {

    @Mock
    private CacheManager cacheManager;
    @Mock
    private ObjectProvider<ArticleIndexer> indexerProvider;
    @Mock
    private ArticleIndexer indexer;
    @Mock
    private Cache cache;
    @Mock
    private CachePurger cachePurger;

    private ArticleEventConsumer consumer() {
        return new ArticleEventConsumer(cacheManager, indexerProvider, cachePurger);
    }

    private static ArticleEvent event(ArticleEventType type, String articleId) {
        return ArticleEvent.newBuilder()
                .setType(type)
                .setArticleId(articleId)
                .setOccurredAt("2026-06-20T10:00:00Z")
                .build();
    }

    @Test
    @DisplayName("PUBLISHED evicts caches and re-indexes the article")
    void published_evictsAndReindexes() {
        when(cacheManager.getCache("article")).thenReturn(cache);
        when(cacheManager.getCache("articles")).thenReturn(cache);
        when(indexerProvider.getIfAvailable()).thenReturn(indexer);

        consumer().onArticleEvent(event(ArticleEventType.PUBLISHED, "1"));

        verify(cache, atLeastOnce()).clear();
        verify(cachePurger).purgeArticle("1");
        verify(cachePurger).purgeHomepage();
        verify(indexer).indexOne("1");
    }

    @Test
    @DisplayName("DELETED removes the article from the index")
    void deleted_removesFromIndex() {
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(indexerProvider.getIfAvailable()).thenReturn(indexer);

        consumer().onArticleEvent(event(ArticleEventType.DELETED, "9"));

        verify(indexer).delete("9");
    }

    @Test
    @DisplayName("without an indexer (no ElasticSearch profile) it only evicts caches")
    void withoutIndexer_onlyEvicts() {
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(indexerProvider.getIfAvailable()).thenReturn(null);

        consumer().onArticleEvent(event(ArticleEventType.UPDATED, "1"));

        verify(cache, atLeastOnce()).clear();
        verifyNoInteractions(indexer);
    }
}
