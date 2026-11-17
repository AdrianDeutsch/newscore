package de.newscore.unit.kafka;

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
    /** Two caches, two mocks: a consumer that cleared only one of them must fail the test. */
    @Mock
    private Cache articleCache;
    @Mock
    private Cache articlesCache;
    @Mock
    private CachePurger cachePurger;

    private ArticleEventConsumer consumer() {
        return new ArticleEventConsumer(cacheManager, indexerProvider, cachePurger);
    }

    private void stubCaches() {
        when(cacheManager.getCache("article")).thenReturn(articleCache);
        when(cacheManager.getCache("articles")).thenReturn(articlesCache);
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
        stubCaches();
        when(indexerProvider.getIfAvailable()).thenReturn(indexer);

        consumer().onArticleEvent(event(ArticleEventType.PUBLISHED, "1"));

        verify(articleCache).clear();
        verify(articlesCache).clear();
        verify(cachePurger).purgeArticle("1");
        verify(cachePurger).purgeHomepage();
        verify(indexer).indexOne("1");
    }

    @Test
    @DisplayName("DELETED removes the article from the index")
    void deleted_removesFromIndex() {
        stubCaches();
        when(indexerProvider.getIfAvailable()).thenReturn(indexer);

        consumer().onArticleEvent(event(ArticleEventType.DELETED, "9"));

        verify(indexer).delete("9");
    }

    @Test
    @DisplayName("without an indexer (no ElasticSearch profile) it only evicts caches")
    void withoutIndexer_onlyEvicts() {
        stubCaches();
        when(indexerProvider.getIfAvailable()).thenReturn(null);

        consumer().onArticleEvent(event(ArticleEventType.UPDATED, "1"));

        verify(articleCache).clear();
        verify(articlesCache).clear();
        verifyNoInteractions(indexer);
    }
}
