package de.newscore.unit.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.newscore.search.ArticleDocument;
import de.newscore.search.ArticleIndexer;
import de.newscore.search.ArticleSearchRepository;
import de.newscore.service.ArticleService;
import de.newscore.unit.fixtures.ArticleFixture;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

@ExtendWith(MockitoExtension.class)
class ArticleIndexerTest {

    @Mock
    private ArticleService articleService;
    @Mock
    private ArticleSearchRepository repository;
    @Mock
    private ElasticsearchOperations operations;
    @Mock
    private IndexOperations indexOps;
    @InjectMocks
    private ArticleIndexer indexer;

    @Test
    @DisplayName("indexAll() creates the index when missing, then indexes and refreshes")
    void indexAll_createsIndexWhenMissing() {
        when(operations.indexOps(ArticleDocument.class)).thenReturn(indexOps);
        when(indexOps.exists()).thenReturn(false);
        when(articleService.findAll()).thenReturn(List.of(ArticleFixture.published()));

        long count = indexer.indexAll();

        assertThat(count).isEqualTo(1);
        verify(indexOps).createWithMapping();
        verify(repository).saveAll(anyList());
        verify(indexOps).refresh();
    }

    @Test
    @DisplayName("indexAll() skips index creation when the index already exists")
    void indexAll_skipsCreateWhenIndexExists() {
        when(operations.indexOps(ArticleDocument.class)).thenReturn(indexOps);
        when(indexOps.exists()).thenReturn(true);
        when(articleService.findAll()).thenReturn(List.of());

        long count = indexer.indexAll();

        assertThat(count).isZero();
        verify(indexOps, never()).createWithMapping();
        verify(repository).saveAll(anyList());
        verify(indexOps).refresh();
    }

    @Test
    @DisplayName("indexOne() saves the article when it still exists")
    void indexOne_existingArticle_saves() {
        when(operations.indexOps(ArticleDocument.class)).thenReturn(indexOps);
        when(indexOps.exists()).thenReturn(true);
        when(articleService.findById("1")).thenReturn(Optional.of(ArticleFixture.published()));

        indexer.indexOne("1");

        verify(repository).save(any(ArticleDocument.class));
        verify(indexOps).refresh();
    }

    @Test
    @DisplayName("indexOne() deletes from the index when the article no longer exists")
    void indexOne_missingArticle_deletes() {
        when(operations.indexOps(ArticleDocument.class)).thenReturn(indexOps);
        when(indexOps.exists()).thenReturn(true);
        when(articleService.findById("x")).thenReturn(Optional.empty());

        indexer.indexOne("x");

        verify(repository).deleteById("x");
    }

    @Test
    @DisplayName("delete() removes the article by id and refreshes")
    void delete_removesByIdAndRefreshes() {
        when(operations.indexOps(ArticleDocument.class)).thenReturn(indexOps);

        indexer.delete("9");

        verify(repository).deleteById("9");
        verify(indexOps).refresh();
    }
}
