package de.newscore.unit.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.newscore.domain.Article;
import de.newscore.domain.SearchResult;
import de.newscore.search.ArticleDocument;
import de.newscore.search.ElasticsearchSearchService;
import de.newscore.unit.fixtures.ArticleFixture;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

@ExtendWith(MockitoExtension.class)
class ElasticsearchSearchServiceTest {

    @Mock
    private ElasticsearchOperations operations;
    @InjectMocks
    private ElasticsearchSearchService service;

    @Test
    @DisplayName("search() short-circuits a blank query without querying ElasticSearch")
    void search_blankQuery_shortCircuits() {
        SearchResult result = service.search("   ", 10);

        assertThat(result.totalCount()).isZero();
        assertThat(result.results()).isEmpty();
        verifyNoInteractions(operations);
    }

    @Test
    @DisplayName("search() maps ElasticSearch hits back to domain articles")
    void search_mapsHitsToArticles() {
        ArticleDocument doc1 = ArticleDocument.from(ArticleFixture.anArticle().withId("1").build());
        ArticleDocument doc2 = ArticleDocument.from(ArticleFixture.anArticle().withId("6").build());
        // Build the hit mocks first — calling when() inside another when()'s arguments would nest stubbing.
        List<SearchHit<ArticleDocument>> hitList = List.of(searchHit(doc1), searchHit(doc2));

        @SuppressWarnings("unchecked")
        SearchHits<ArticleDocument> hits = mock(SearchHits.class);
        when(hits.getTotalHits()).thenReturn(2L);
        when(hits.getSearchHits()).thenReturn(hitList);
        when(operations.search(any(Query.class), eq(ArticleDocument.class))).thenReturn(hits);

        SearchResult result = service.search("klima", 10);

        assertThat(result.query()).isEqualTo("klima");
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.results()).extracting(Article::id).containsExactly("1", "6");
    }

    @SuppressWarnings("unchecked")
    private static SearchHit<ArticleDocument> searchHit(ArticleDocument content) {
        SearchHit<ArticleDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(content);
        return hit;
    }
}
