-- Schema for NewsCore analytics (Epic 4). Mounted into the postgres container's
-- docker-entrypoint-initdb.d so the table exists before the analytics-service connects.

CREATE TABLE IF NOT EXISTS search_analytics (
    id           BIGSERIAL PRIMARY KEY,
    query        TEXT        NOT NULL,
    result_count INTEGER     NOT NULL,
    occurred_at  TIMESTAMPTZ NOT NULL,
    recorded_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_search_analytics_occurred_at ON search_analytics (occurred_at);
CREATE INDEX IF NOT EXISTS idx_search_analytics_query ON search_analytics (query);
