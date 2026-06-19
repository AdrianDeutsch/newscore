package de.newscore.service;

import de.newscore.domain.Article;
import de.newscore.domain.SearchResult;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * In-memory {@link SearchService} performing a case-insensitive substring match over an article's
 * title, teaser, body and tags. Reuses {@link ArticleService} as the single source of article data
 * (DRY); a future implementation will delegate to ElasticSearch (Epic 3).
 */
@Service
public class InMemorySearchService implements SearchService {

    private static final int MAX_LIMIT = 50;

    private final ArticleService articleService;

    public InMemorySearchService(ArticleService articleService) {
        this.articleService = articleService;
    }

    @Override
    public SearchResult search(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            return new SearchResult(query == null ? "" : query, 0, List.of());
        }

        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<Article> matches = articleService.findAll().stream()
                .filter(article -> matches(article, needle))
                .toList();

        int effectiveLimit = Math.max(0, Math.min(limit, MAX_LIMIT));
        List<Article> results = matches.stream().limit(effectiveLimit).toList();
        return new SearchResult(query, matches.size(), results);
    }

    private boolean matches(Article article, String needle) {
        return contains(article.title(), needle)
                || contains(article.teaser(), needle)
                || contains(article.body(), needle)
                || article.tags().stream().anyMatch(tag -> contains(tag, needle));
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
