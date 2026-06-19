package de.newscore.search;

import de.newscore.service.ArticleService;
import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * Indexes articles into ElasticSearch. On startup it (re)builds the {@code articles} index from the
 * {@link ArticleService} corpus. In a later iteration this will be driven by Kafka
 * {@code article.published} events (Epic 4) instead of a bulk reindex.
 */
@Component
@Profile("elasticsearch")
public class ArticleIndexer {

    private final ArticleService articleService;
    private final ArticleSearchRepository repository;
    private final ElasticsearchOperations operations;

    public ArticleIndexer(ArticleService articleService,
                          ArticleSearchRepository repository,
                          ElasticsearchOperations operations) {
        this.articleService = articleService;
        this.repository = repository;
        this.operations = operations;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        indexAll();
    }

    /**
     * Ensures the index exists and (re)indexes the full article corpus, then refreshes so the
     * documents are immediately searchable. Idempotent — documents are upserted by id.
     *
     * @return the number of indexed documents
     */
    public long indexAll() {
        ensureIndex();
        List<ArticleDocument> documents = articleService.findAll().stream()
                .map(ArticleDocument::from)
                .toList();
        repository.saveAll(documents);
        refresh();
        return documents.size();
    }

    /**
     * (Re)indexes a single article by id, driven by an {@code article.published}/{@code updated}
     * event. If the article no longer exists it is removed from the index instead.
     *
     * @param articleId the affected article id
     */
    public void indexOne(String articleId) {
        ensureIndex();
        articleService.findById(articleId).ifPresentOrElse(
                article -> {
                    repository.save(ArticleDocument.from(article));
                    refresh();
                },
                () -> delete(articleId));
    }

    /**
     * Removes an article from the index, driven by an {@code article.deleted} event.
     *
     * @param articleId the affected article id
     */
    public void delete(String articleId) {
        repository.deleteById(articleId);
        refresh();
    }

    private void ensureIndex() {
        IndexOperations indexOps = operations.indexOps(ArticleDocument.class);
        if (!indexOps.exists()) {
            indexOps.createWithMapping();
        }
    }

    private void refresh() {
        operations.indexOps(ArticleDocument.class).refresh();
    }
}
