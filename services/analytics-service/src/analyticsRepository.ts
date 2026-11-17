import type { Pool } from 'pg'
import type { MessageSource, PageViewEvent, SearchExecutedEvent } from './types'

/**
 * Persistence boundary for analytics events. Kept behind an interface so the message handlers can be
 * unit-tested without a real PostgreSQL connection.
 *
 * Both inserts are idempotent per event: Kafka delivers at least once, so the same event can arrive
 * again after a crash or rebalance. Its eventId is stored under a unique constraint and a second
 * insert of the same event is ignored. The Kafka coordinates are stored alongside, for tracing only.
 */
export interface AnalyticsRepository {
  /** @returns true if a row was written, false if this event had been stored before */
  insertSearchEvent(event: SearchExecutedEvent, source: MessageSource): Promise<boolean>
  /** @returns true if a row was written, false if this event had been stored before */
  insertPageView(event: PageViewEvent, source: MessageSource): Promise<boolean>
  close(): Promise<void>
}

export const INSERT_SEARCH_EVENT =
  'INSERT INTO search_analytics (event_id, query, result_count, occurred_at, source_topic, source_partition, source_offset) '
  + 'VALUES ($1, $2, $3, $4, $5, $6, $7) '
  + 'ON CONFLICT (event_id) DO NOTHING'

export const INSERT_PAGE_VIEW =
  'INSERT INTO page_view_analytics (event_id, path, occurred_at, source_topic, source_partition, source_offset) '
  + 'VALUES ($1, $2, $3, $4, $5, $6) '
  + 'ON CONFLICT (event_id) DO NOTHING'

/**
 * Creates a PostgreSQL-backed {@link AnalyticsRepository}.
 *
 * @param pool a configured pg connection pool
 */
export function createAnalyticsRepository(pool: Pool): AnalyticsRepository {
  return {
    async insertSearchEvent(event: SearchExecutedEvent, source: MessageSource): Promise<boolean> {
      const result = await pool.query(INSERT_SEARCH_EVENT, [
        event.eventId,
        event.query,
        event.resultCount,
        event.occurredAt,
        source.topic,
        source.partition,
        source.offset,
      ])
      return result.rowCount === 1
    },

    async insertPageView(event: PageViewEvent, source: MessageSource): Promise<boolean> {
      const result = await pool.query(INSERT_PAGE_VIEW, [
        event.eventId,
        event.path,
        event.occurredAt,
        source.topic,
        source.partition,
        source.offset,
      ])
      return result.rowCount === 1
    },

    async close(): Promise<void> {
      await pool.end()
    },
  }
}
