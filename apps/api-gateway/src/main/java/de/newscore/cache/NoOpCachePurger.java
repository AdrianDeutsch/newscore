package de.newscore.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default {@link CachePurger} used when no edge cache is configured. Purges are no-ops.
 */
@Component
@ConditionalOnProperty(name = "newscore.varnish.purge.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpCachePurger implements CachePurger {

    private static final Logger log = LoggerFactory.getLogger(NoOpCachePurger.class);

    @Override
    public void purgeArticle(String articleId) {
        log.debug("Edge cache disabled — skipping purge for article {}", articleId);
    }

    @Override
    public void purgeHomepage() {
        log.debug("Edge cache disabled — skipping homepage purge");
    }
}
