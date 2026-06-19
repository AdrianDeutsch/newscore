package de.newscore.resolver;

import de.newscore.domain.SearchResult;
import de.newscore.kafka.EventPublisher;
import de.newscore.kafka.SearchExecutedEvent;
import de.newscore.service.SearchService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL resolver for the {@code Query.search} field. Emits a {@link SearchExecutedEvent} for
 * analytics (consumed by the analytics-service via Kafka, Epic 4).
 */
@Controller
public class SearchController {

    private final SearchService searchService;
    private final EventPublisher eventPublisher;

    public SearchController(SearchService searchService, EventPublisher eventPublisher) {
        this.searchService = searchService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Resolves {@code Query.search(query, limit)}.
     *
     * @param query the search term
     * @param limit maximum number of results (schema default applies when omitted)
     * @return the search result
     */
    @QueryMapping
    public SearchResult search(@Argument String query, @Argument int limit) {
        SearchResult result = searchService.search(query, limit);
        eventPublisher.publishSearchExecuted(SearchExecutedEvent.of(query, result.totalCount()));
        return result;
    }
}
