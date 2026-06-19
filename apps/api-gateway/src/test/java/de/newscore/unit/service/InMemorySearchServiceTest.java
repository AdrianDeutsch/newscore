package de.newscore.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.newscore.domain.Article;
import de.newscore.domain.SearchResult;
import de.newscore.service.ArticleService;
import de.newscore.service.InMemorySearchService;
import de.newscore.unit.fixtures.ArticleFixture;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InMemorySearchServiceTest {

    @Mock
    private ArticleService articleService;

    private InMemorySearchService service;

    private final Article klima = ArticleFixture.anArticle()
            .withId("1").withTitle("Klimapaket beschlossen").withTeaser("EU einigt sich")
            .withBody("Details zum Klimapaket").withTags(List.of("klima", "eu")).build();
    private final Article inflation = ArticleFixture.anArticle()
            .withId("2").withTitle("Wirtschaft heute").withTeaser("Marktbericht")
            .withBody("Die Inflation sinkt weiter").withTags(List.of("wirtschaft")).build();
    private final Article sport = ArticleFixture.anArticle()
            .withId("3").withTitle("Spieltag").withTeaser("Zusammenfassung")
            .withBody("Ein spannendes Spiel").withTags(List.of("fussball")).build();

    @BeforeEach
    void setUp() {
        this.service = new InMemorySearchService(articleService);
    }

    private void givenCorpus() {
        when(articleService.findAll()).thenReturn(List.of(klima, inflation, sport));
    }

    @Test
    @DisplayName("search() matches on the title")
    void search_matchInTitle_returnsArticle() {
        givenCorpus();

        SearchResult result = service.search("Klimapaket", 10);

        assertThat(result.query()).isEqualTo("Klimapaket");
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.results()).extracting(Article::id).containsExactly("1");
    }

    @Test
    @DisplayName("search() matches on the body and tags")
    void search_matchInBodyAndTags_returnsArticle() {
        givenCorpus();

        assertThat(service.search("inflation", 10).results())
                .extracting(Article::id).containsExactly("2");
        assertThat(service.search("fussball", 10).results())
                .extracting(Article::id).containsExactly("3");
    }

    @Test
    @DisplayName("search() is case-insensitive")
    void search_caseInsensitive_matches() {
        givenCorpus();

        assertThat(service.search("KLIMA", 10).results())
                .extracting(Article::id).containsExactly("1");
    }

    @Test
    @DisplayName("search() caps the number of results at the requested limit")
    void search_limit_capsResults() {
        givenCorpus();

        // All three articles contain the letter "e"; limit restricts the returned slice.
        SearchResult result = service.search("e", 2);

        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.results()).hasSize(2);
    }

    @Test
    @DisplayName("search() returns an empty result for a non-matching query")
    void search_noMatch_returnsEmpty() {
        givenCorpus();

        SearchResult result = service.search("xyz-nicht-vorhanden", 10);

        assertThat(result.totalCount()).isZero();
        assertThat(result.results()).isEmpty();
    }

    @Test
    @DisplayName("search() short-circuits a blank query without touching the corpus")
    void search_blankQuery_returnsEmpty() {
        SearchResult result = service.search("   ", 10);

        assertThat(result.totalCount()).isZero();
        assertThat(result.results()).isEmpty();
        verifyNoInteractions(articleService);
    }
}
