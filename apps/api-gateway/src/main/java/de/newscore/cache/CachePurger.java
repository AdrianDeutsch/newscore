package de.newscore.cache;

/**
 * Invalidates the HTTP edge cache (Varnish) for specific content (Epic 3 / ADR-008).
 *
 * <p>Strategy abstraction: a no-op implementation is used unless {@code newscore.varnish.purge.enabled}
 * is set, so the gateway runs without an edge cache by default.</p>
 */
public interface CachePurger {

    /**
     * Purges the page of a single article.
     *
     * @param articleId the affected article id
     */
    void purgeArticle(String articleId);

    /**
     * Purges the homepage (its article list changes whenever content changes).
     */
    void purgeHomepage();
}
