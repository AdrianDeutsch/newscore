package de.newscore.unit.cache;

import static org.assertj.core.api.Assertions.assertThatCode;

import de.newscore.cache.NoOpCachePurger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoOpCachePurgerTest {

    private final NoOpCachePurger purger = new NoOpCachePurger();

    @Test
    @DisplayName("the no-op purger silently ignores purges")
    void purgesAreNoOps() {
        assertThatCode(() -> {
            purger.purgeArticle("1");
            purger.purgeHomepage();
        }).doesNotThrowAnyException();
    }
}
