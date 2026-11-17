package de.newscore.unit.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import de.newscore.kafka.EventPublisher;
import de.newscore.web.PageViewController;
import de.newscore.web.PageViewController.PageViewRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PageViewControllerTest {

    @Mock
    private EventPublisher eventPublisher;
    @InjectMocks
    private PageViewController controller;

    @Test
    @DisplayName("a valid page view is accepted and published")
    void pageView_valid_publishesEvent() {
        var response = controller.pageView(new PageViewRequest("/article/1"));

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        verify(eventPublisher).publishPageView(argThat(event -> event.path().equals("/article/1")));
    }

    @Test
    @DisplayName("a blank path is rejected and emits nothing")
    void pageView_blankPath_returnsBadRequest() {
        var response = controller.pageView(new PageViewRequest("  "));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(eventPublisher);
    }

    @ParameterizedTest(name = "\"{0}\" is rejected")
    @ValueSource(strings = {"article/1", "https://evil.example/x", "//evil.example/x", "/\\evil.example",
        "/a b", "/a\tb", "/a\u0000b", "/<script>", "/ümlaut"})
    @DisplayName("a path that is not a site-relative URL path is rejected and emits nothing")
    void pageView_notSiteRelative_returnsBadRequest(String path) {
        var response = controller.pageView(new PageViewRequest(path));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(eventPublisher);
    }

    @ParameterizedTest(name = "\"{0}\" is accepted")
    @ValueSource(strings = {"/", "/article/1", "/?category=politik", "/search?q=klima+eu", "/search?q=%C3%A4rzte",
        "/article/2?e2e=4f1c-9a", "/article/1#kommentare", "/search?q=[klima]", "/search?q=a|b",
        "/search?q={x}^`y`"})
    @DisplayName("paths as the router sends them are accepted")
    void pageView_routerPaths_areAccepted(String path) {
        assertThat(controller.pageView(new PageViewRequest(path)).getStatusCode().value()).isEqualTo(202);
    }

    @Test
    @DisplayName("a path of 2048 characters is accepted, one more is rejected")
    void pageView_pathLength_isBounded() {
        String longest = "/" + "a".repeat(2047);

        assertThat(controller.pageView(new PageViewRequest(longest)).getStatusCode().value()).isEqualTo(202);
        assertThat(controller.pageView(new PageViewRequest(longest + "a")).getStatusCode().value()).isEqualTo(400);
        verify(eventPublisher).publishPageView(argThat(event -> event.path().equals(longest)));
    }

    @Test
    @DisplayName("a missing body is rejected")
    void pageView_nullBody_returnsBadRequest() {
        var response = controller.pageView(null);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(eventPublisher);
    }
}
