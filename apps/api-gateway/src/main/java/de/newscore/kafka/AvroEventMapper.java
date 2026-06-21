package de.newscore.kafka;

/**
 * Maps the internal domain event records to their Avro wire representations (see ADR-011). Avro is
 * confined to the Kafka boundary; controllers and the {@link EventPublisher} API keep using the
 * plain domain records.
 */
final class AvroEventMapper {

    private AvroEventMapper() {
    }

    static de.newscore.kafka.avro.ArticleEvent toAvro(ArticleEvent event) {
        return de.newscore.kafka.avro.ArticleEvent.newBuilder()
                .setType(de.newscore.kafka.avro.ArticleEventType.valueOf(event.type().name()))
                .setArticleId(event.articleId())
                .setOccurredAt(event.occurredAt().toString())
                .build();
    }

    static de.newscore.kafka.avro.SearchExecutedEvent toAvro(SearchExecutedEvent event) {
        return de.newscore.kafka.avro.SearchExecutedEvent.newBuilder()
                .setQuery(event.query())
                .setResultCount(event.resultCount())
                .setOccurredAt(event.occurredAt().toString())
                .build();
    }

    static de.newscore.kafka.avro.PageViewEvent toAvro(PageViewEvent event) {
        return de.newscore.kafka.avro.PageViewEvent.newBuilder()
                .setPath(event.path())
                .setOccurredAt(event.occurredAt().toString())
                .build();
    }
}
