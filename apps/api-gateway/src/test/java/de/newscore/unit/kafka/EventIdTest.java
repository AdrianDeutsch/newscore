package de.newscore.unit.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import de.newscore.kafka.PageViewEvent;
import de.newscore.kafka.SearchExecutedEvent;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The analytics store deduplicates on eventId, so two events must never share one — including two
 * identical searches in the same millisecond.
 */
class EventIdTest {

    @Test
    @DisplayName("every search event gets its own UUID, even for identical searches")
    void searchEvents_haveDistinctIds() {
        var ids = IntStream.range(0, 1_000).mapToObj(i -> SearchExecutedEvent.of("klima", 2).eventId()).toList();

        assertThat(ids).doesNotHaveDuplicates();
        assertThat(UUID.fromString(ids.get(0)).toString()).isEqualTo(ids.get(0));
    }

    @Test
    @DisplayName("every page-view event gets its own UUID, even for the same path")
    void pageViewEvents_haveDistinctIds() {
        var ids = IntStream.range(0, 1_000).mapToObj(i -> PageViewEvent.of("/").eventId()).toList();

        assertThat(ids).doesNotHaveDuplicates();
        assertThat(UUID.fromString(ids.get(0)).toString()).isEqualTo(ids.get(0));
    }
}
