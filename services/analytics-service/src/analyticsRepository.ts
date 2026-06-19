import type { Pool } from 'pg'
import type { SearchExecutedEvent } from './types'

/**
 * Persistence boundary for analytics events. Kept behind an interface so the message handler can be
 * unit-tested without a real PostgreSQL connection.
 */
export interface AnalyticsRepository {
  insertSearchEvent(event: SearchExecutedEvent): Promise<void>
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
    async close(): Promise<void> {
      await pool.end()
    },
  }
}
