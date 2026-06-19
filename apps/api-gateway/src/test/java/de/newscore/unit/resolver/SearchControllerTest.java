package de.newscore.unit.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.newscore.domain.SearchResult;
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
}
