package de.newscore.web;

import de.newscore.kafka.EventPublisher;
import de.newscore.kafka.PageViewEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives page-view beacons from the frontend and emits {@code user.pageview} events for the
 * analytics-service (completes the Epic 4 event matrix). Without the {@code kafka} profile the
 * {@link EventPublisher} is a no-op, so the endpoint is harmless but inert.
 */
@RestController
@RequestMapping("/events")
@CrossOrigin(origins = "*")
public class PageViewController {

    private final EventPublisher eventPublisher;

    public PageViewController(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /** Request body for a page view. */
    public record PageViewRequest(String path) {
    }

    /**
     * Records a page view.
     *
     * @param request the page-view payload
     * @return 202 if accepted, 400 if the path is missing
     */
    @PostMapping("/pageview")
    public ResponseEntity<Void> pageView(@RequestBody(required = false) PageViewRequest request) {
        if (request == null || !StringUtils.hasText(request.path())) {
            return ResponseEntity.badRequest().build();
        }
        eventPublisher.publishPageView(PageViewEvent.of(request.path()));
        return ResponseEntity.accepted().build();
    }
}
