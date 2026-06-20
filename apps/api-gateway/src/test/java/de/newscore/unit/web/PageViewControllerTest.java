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

    @Test
    @DisplayName("a missing body is rejected")
    void pageView_nullBody_returnsBadRequest() {
        var response = controller.pageView(null);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(eventPublisher);
    }
}
