package de.newscore.unit.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import de.newscore.kafka.ArticleEventType;
import de.newscore.kafka.EventPublisher;
import de.newscore.web.CmsSimulatorController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CmsSimulatorControllerTest {

    @Mock
    private EventPublisher eventPublisher;
    @InjectMocks
    private CmsSimulatorController controller;

    @Test
    @DisplayName("a publish action emits a PUBLISHED event and returns 202")
    void publishAction_emitsPublishedEvent() {
        var response = controller.emit("1", "publish");

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        verify(eventPublisher).publishArticleEvent(
                argThat(event -> event.type() == ArticleEventType.PUBLISHED && event.articleId().equals("1")));
    }

    @Test
    @DisplayName("a delete action emits a DELETED event")
    void deleteAction_emitsDeletedEvent() {
        controller.emit("9", "delete");

        verify(eventPublisher).publishArticleEvent(argThat(event -> event.type() == ArticleEventType.DELETED));
    }

    @Test
    @DisplayName("an unknown action returns 400 and emits nothing")
    void unknownAction_returnsBadRequest() {
        var response = controller.emit("1", "frobnicate");

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(eventPublisher);
    }
}
