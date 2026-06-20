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

CREATE TABLE IF NOT EXISTS page_view_analytics (
    id          BIGSERIAL PRIMARY KEY,
    path        TEXT        NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_page_view_analytics_occurred_at ON page_view_analytics (occurred_at);
CREATE INDEX IF NOT EXISTS idx_page_view_analytics_path ON page_view_analytics (path);
