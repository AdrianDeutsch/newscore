-- Schema for NewsCore analytics. Mounted into the postgres container's
-- docker-entrypoint-initdb.d so the tables exist before the analytics-service connects.
--
-- source_topic/source_partition/source_offset identify the Kafka record a row came from. The unique
-- constraint makes the insert idempotent: Kafka delivers at least once, and a record that arrives a
-- second time must not produce a second row (the service inserts with ON CONFLICT DO NOTHING).

CREATE TABLE IF NOT EXISTS search_analytics (
    id               BIGSERIAL PRIMARY KEY,
    query            TEXT        NOT NULL,
    result_count     INTEGER     NOT NULL,
    occurred_at      TIMESTAMPTZ NOT NULL,
    recorded_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    source_topic     TEXT        NOT NULL,
    source_partition INTEGER     NOT NULL,
    source_offset    BIGINT      NOT NULL,
    CONSTRAINT uq_search_analytics_source UNIQUE (source_topic, source_partition, source_offset)
);

CREATE INDEX IF NOT EXISTS idx_search_analytics_occurred_at ON search_analytics (occurred_at);
CREATE INDEX IF NOT EXISTS idx_search_analytics_query ON search_analytics (query);

CREATE TABLE IF NOT EXISTS page_view_analytics (
    id               BIGSERIAL PRIMARY KEY,
    path             TEXT        NOT NULL,
    occurred_at      TIMESTAMPTZ NOT NULL,
    recorded_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    source_topic     TEXT        NOT NULL,
    source_partition INTEGER     NOT NULL,
    source_offset    BIGINT      NOT NULL,
    CONSTRAINT uq_page_view_analytics_source UNIQUE (source_topic, source_partition, source_offset)
);

CREATE INDEX IF NOT EXISTS idx_page_view_analytics_occurred_at ON page_view_analytics (occurred_at);
CREATE INDEX IF NOT EXISTS idx_page_view_analytics_path ON page_view_analytics (path);
