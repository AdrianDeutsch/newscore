package de.newscore.web;

import de.newscore.kafka.EventPublisher;
import de.newscore.kafka.PageViewEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives page-view beacons and emits {@code user.pageview} events for the analytics-service.
 * The browser sends the beacon to the frontend's own origin, which forwards it here, so no CORS
 * handling is needed. Without the {@code kafka} profile the {@link EventPublisher} is a no-op, so
 * the endpoint is harmless but inert.
 */
@RestController
@RequestMapping("/events")
public class PageViewController {

    /** Longest accepted path; longer values are rejected rather than truncated. */
    static final int MAX_PATH_LENGTH = 2048;

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
     * @return 202 if accepted, 400 if the path is missing or not a site-relative path
     */
    @PostMapping("/pageview")
    public ResponseEntity<Void> pageView(@RequestBody(required = false) PageViewRequest request) {
        if (request == null || !isSitePath(request.path())) {
            return ResponseEntity.badRequest().build();
        }
        eventPublisher.publishPageView(PageViewEvent.of(request.path()));
        return ResponseEntity.accepted().build();
    }

    /**
     * The endpoint is public by nature (a browser beacon), so it only accepts what a page view of
     * this site can look like: a site-relative path of bounded length. Anything else would let a
     * caller put arbitrary text onto the event bus and into the analytics table.
     */
    private static boolean isSitePath(String path) {
        return StringUtils.hasText(path)
                && path.startsWith("/")
                && !path.startsWith("//")
                && path.length() <= MAX_PATH_LENGTH;
    }
}
