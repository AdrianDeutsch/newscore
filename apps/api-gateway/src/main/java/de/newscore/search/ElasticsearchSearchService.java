package de.newscore.search;

import de.newscore.domain.Article;
import de.newscore.domain.SearchResult;
import de.newscore.service.SearchService;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * ElasticSearch-backed {@link SearchService} (see ADR-004). Active under the {@code elasticsearch}
 * profile; the in-memory implementation is used otherwise.
 *
 * <p>Runs an analyzed multi-field match across title, teaser, body and tags. {@code totalCount}
 * reflects all matches; the returned list is capped by the requested limit.</p>
 */
@Service
@Profile("elasticsearch")
public class ElasticsearchSearchService implements SearchService {

    private static final int MAX_LIMIT = 50;

    private final ElasticsearchOperations operations;

    public ElasticsearchSearchService(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @Override
    public SearchResult search(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            return new SearchResult(query == null ? "" : query, 0, List.of());
        }

        Criteria criteria = new Criteria("title").matches(query)
                .or("teaser").matches(query)
                .or("body").matches(query)
                .or("tags").matches(query);
        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);
        criteriaQuery.setPageable(PageRequest.of(0, MAX_LIMIT));

        SearchHits<ArticleDocument> hits = operations.search(criteriaQuery, ArticleDocument.class);

        int effectiveLimit = Math.max(0, Math.min(limit, MAX_LIMIT));
        List<Article> results = hits.getSearchHits().stream()
                .limit(effectiveLimit)
                .map(hit -> hit.getContent().toArticle())
                .toList();
        return new SearchResult(query, (int) hits.getTotalHits(), results);
    }
}
