package de.newscore.kafka;

/**
 * Lifecycle of an editorial article, as emitted by the CMS (here: the CMS simulator).
 */
public enum ArticleEventType {
    PUBLISHED,
    UPDATED,
    DELETED
}
