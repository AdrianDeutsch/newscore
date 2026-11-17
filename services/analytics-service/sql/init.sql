-- Schema for NewsCore analytics. Mounted into the postgres container's
-- docker-entrypoint-initdb.d, so it runs once, when the data volume is created. There is no migration
-- tool: after a schema change, start with a fresh volume (`docker compose down -v`).
--
-- event_id is the idempotency key: Kafka delivers at least once, and an event that arrives a second
-- time must not produce a second row (the service inserts with ON CONFLICT (event_id) DO NOTHING).
-- source_topic/source_partition/source_offset record which Kafka message a row came from. They are
-- deliberately not unique: offsets start again at 0 when a topic is recreated (DEFECT-010).

CREATE TABLE IF NOT EXISTS search_analytics (
    id               BIGSERIAL PRIMARY KEY,
    event_id         TEXT        NOT NULL,
    query            TEXT        NOT NULL,
    result_count     INTEGER     NOT NULL,
    occurred_at      TIMESTAMPTZ NOT NULL,
    recorded_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    source_topic     TEXT        NOT NULL,
    source_partition INTEGER     NOT NULL,
    source_offset    BIGINT      NOT NULL,
    CONSTRAINT uq_search_analytics_event UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_search_analytics_occurred_at ON search_analytics (occurred_at);
CREATE INDEX IF NOT EXISTS idx_search_analytics_query ON search_analytics (query);

CREATE TABLE IF NOT EXISTS page_view_analytics (
    id               BIGSERIAL PRIMARY KEY,
    event_id         TEXT        NOT NULL,
    path             TEXT        NOT NULL,
    occurred_at      TIMESTAMPTZ NOT NULL,
    recorded_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    source_topic     TEXT        NOT NULL,
    source_partition INTEGER     NOT NULL,
    source_offset    BIGINT      NOT NULL,
    CONSTRAINT uq_page_view_analytics_event UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_page_view_analytics_occurred_at ON page_view_analytics (occurred_at);
CREATE INDEX IF NOT EXISTS idx_page_view_analytics_path ON page_view_analytics (path);
