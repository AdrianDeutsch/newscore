package de.newscore.web;

import de.newscore.kafka.ArticleEvent;
import de.newscore.kafka.ArticleEventType;
import de.newscore.kafka.EventPublisher;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stands in for the Sophora CMS webhook: turns an HTTP call into an {@link ArticleEvent} on the
 * event bus, which the gateway then consumes to re-index ElasticSearch and evict caches (Epic 4).
 *
 * <p>Without the {@code kafka} profile the {@link EventPublisher} is a no-op, so this endpoint is
 * harmless but inert.</p>
 */
@RestController
@RequestMapping("/internal/cms")
public class CmsSimulatorController {

    private final EventPublisher eventPublisher;

    public CmsSimulatorController(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Emits an article lifecycle event.
     *
     * @param id     the article id
     * @param action one of {@code publish}, {@code update}, {@code delete}
     * @return 202 with a confirmation, or 400 for an unknown action
     */
    @PostMapping("/articles/{id}/{action}")
    public ResponseEntity<String> emit(@PathVariable String id, @PathVariable String action) {
        ArticleEventType type = switch (action.toLowerCase(Locale.ROOT)) {
            case "publish", "published" -> ArticleEventType.PUBLISHED;
            case "update", "updated" -> ArticleEventType.UPDATED;
            case "delete", "deleted" -> ArticleEventType.DELETED;
            default -> null;
        };
        if (type == null) {
            return ResponseEntity.badRequest().body("Unknown action: " + action);
        }
        eventPublisher.publishArticleEvent(ArticleEvent.of(type, id));
        return ResponseEntity.accepted().body(type + " event emitted for article " + id);
    }
}
