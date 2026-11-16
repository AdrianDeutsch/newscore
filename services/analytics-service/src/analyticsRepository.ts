import type { Pool } from 'pg'
import type { MessageSource, PageViewEvent, SearchExecutedEvent } from './types'

/**
 * Persistence boundary for analytics events. Kept behind an interface so the message handlers can be
 * unit-tested without a real PostgreSQL connection.
 *
 * Both inserts are idempotent per Kafka record: Kafka delivers at least once, so the same record can
 * arrive again after a crash or rebalance. The record's origin is stored with a unique constraint and
 * a second insert of the same record is ignored.
 */
export interface AnalyticsRepository {
  /** @returns true if a row was written, false if this record had been stored before */
  insertSearchEvent(event: SearchExecutedEvent, source: MessageSource): Promise<boolean>
  /** @returns true if a row was written, false if this record had been stored before */
  insertPageView(event: PageViewEvent, source: MessageSource): Promise<boolean>
  close(): Promise<void>
}

export const INSERT_SEARCH_EVENT =
  'INSERT INTO search_analytics (query, result_count, occurred_at, source_topic, source_partition, source_offset) '
  + 'VALUES ($1, $2, $3, $4, $5, $6) '
  + 'ON CONFLICT (source_topic, source_partition, source_offset) DO NOTHING'

export const INSERT_PAGE_VIEW =
  'INSERT INTO page_view_analytics (path, occurred_at, source_topic, source_partition, source_offset) '
  + 'VALUES ($1, $2, $3, $4, $5) '
  + 'ON CONFLICT (source_topic, source_partition, source_offset) DO NOTHING'

/**
 * Creates a PostgreSQL-backed {@link AnalyticsRepository}.
 *
 * @param pool a configured pg connection pool
 */
export function createAnalyticsRepository(pool: Pool): AnalyticsRepository {
  return {
    async insertSearchEvent(event: SearchExecutedEvent, source: MessageSource): Promise<boolean> {
      const result = await pool.query(INSERT_SEARCH_EVENT, [
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
