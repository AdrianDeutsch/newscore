import type { Pool } from 'pg'
import type { PageViewEvent, SearchExecutedEvent } from './types'

/**
 * Persistence boundary for analytics events. Kept behind an interface so the message handlers can be
 * unit-tested without a real PostgreSQL connection.
 */
export interface AnalyticsRepository {
  insertSearchEvent(event: SearchExecutedEvent): Promise<void>
  insertPageView(event: PageViewEvent): Promise<void>
  close(): Promise<void>
}

/**
 * Creates a PostgreSQL-backed {@link AnalyticsRepository}.
 *
 * @param pool a configured pg connection pool
 */
export function createAnalyticsRepository(pool: Pool): AnalyticsRepository {
  return {
    async insertSearchEvent(event: SearchExecutedEvent): Promise<void> {
      await pool.query(
        'INSERT INTO search_analytics (query, result_count, occurred_at) VALUES ($1, $2, $3)',
        [event.query, event.resultCount, event.occurredAt],
      )
    },
    async insertPageView(event: PageViewEvent): Promise<void> {
      await pool.query(
        'INSERT INTO page_view_analytics (path, occurred_at) VALUES ($1, $2)',
        [event.path, event.occurredAt],
      )
    },
    async close(): Promise<void> {
      await pool.end()
    },
  }
}
