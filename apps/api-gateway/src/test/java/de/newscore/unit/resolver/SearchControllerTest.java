package de.newscore.unit.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.newscore.domain.SearchResult;
import de.newscore.kafka.EventPublisher;
import de.newscore.resolver.SearchController;
import de.newscore.service.SearchService;
import de.newscore.unit.fixtures.ArticleFixture;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private SearchService searchService;
    @Mock
    private EventPublisher eventPublisher;
    @InjectMocks
    private SearchController controller;

    @Test
    @DisplayName("search() delegates to the service and returns its result")
    void search_delegatesToService() {
        SearchResult expected = new SearchResult("klima", 1, List.of(ArticleFixture.published()));
        when(searchService.search("klima", 10)).thenReturn(expected);

        assertThat(controller.search("klima", 10)).isSameAs(expected);
        verify(searchService).search("klima", 10);
    }

    @Test
    @DisplayName("search() publishes a search-executed analytics event")
    void search_publishesAnalyticsEvent() {
        SearchResult result = new SearchResult("klima", 2, List.of(ArticleFixture.published()));
        when(searchService.search("klima", 10)).thenReturn(result);

        controller.search("klima", 10);

        verify(eventPublisher).publishSearchExecuted(
                argThat(event -> event.query().equals("klima") && event.resultCount() == 2));
    }
}
