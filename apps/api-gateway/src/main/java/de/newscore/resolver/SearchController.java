package de.newscore.resolver;

import de.newscore.domain.SearchResult;
import de.newscore.service.SearchService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL resolver for the {@code Query.search} field.
 */
@Controller
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
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
        return searchService.search(query, limit);
    }
}
