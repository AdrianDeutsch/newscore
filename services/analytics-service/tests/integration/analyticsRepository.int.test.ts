import path from 'node:path'
import { PostgreSqlContainer, type StartedPostgreSqlContainer } from '@testcontainers/postgresql'
import { Pool } from 'pg'
import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest'
import { type AnalyticsRepository, createAnalyticsRepository } from '../../src/analyticsRepository'
import type { MessageSource } from '../../src/types'

/**
 * Runs the repository's SQL against a real PostgreSQL that was initialised with sql/init.sql — the
 * same file the compose stack mounts. The unit tests only check which statement is sent; whether
 * the statement and the schema fit together can only be seen here.
 */
describe('analytics repository against PostgreSQL', () => {
  let container: StartedPostgreSqlContainer
  let pool: Pool
  let repository: AnalyticsRepository

  const source = (offset: string, partition = 0): MessageSource => ({
    topic: 'newscore.search.events',
    partition,
    offset,
  })

  beforeAll(async () => {
    container = await new PostgreSqlContainer('postgres:16-alpine')
      .withCopyFilesToContainer([
        { source: path.resolve(__dirname, '../../sql/init.sql'), target: '/docker-entrypoint-initdb.d/init.sql' },
      ])
      .start()
    pool = new Pool({ connectionString: container.getConnectionUri() })
    repository = createAnalyticsRepository(pool)
  })

  afterAll(async () => {
    await pool?.end()
    await container?.stop()
  })

  beforeEach(async () => {
    await pool.query('TRUNCATE search_analytics, page_view_analytics')
  })

  it('stores a search event with its timestamp and origin', async () => {
    const inserted = await repository.insertSearchEvent(
      { query: 'klima', resultCount: 2, occurredAt: '2026-06-19T10:00:00Z' },
      source('5'),
    )

    const { rows } = await pool.query(
      'SELECT query, result_count, occurred_at, source_partition, source_offset FROM search_analytics',
    )
    expect(inserted).toBe(true)
    expect(rows).toHaveLength(1)
    expect(rows[0].query).toBe('klima')
    expect(rows[0].result_count).toBe(2)
    expect((rows[0].occurred_at as Date).toISOString()).toBe('2026-06-19T10:00:00.000Z')
    expect(rows[0].source_partition).toBe(0)
    expect(rows[0].source_offset).toBe('5')
  })

  it('stores the same Kafka record only once', async () => {
    const event = { query: 'klima', resultCount: 2, occurredAt: '2026-06-19T10:00:00Z' }

    expect(await repository.insertSearchEvent(event, source('5'))).toBe(true)
    expect(await repository.insertSearchEvent(event, source('5'))).toBe(false)

    const { rows } = await pool.query('SELECT count(*)::int AS n FROM search_analytics')
    expect(rows[0].n).toBe(1)
  })

  it('keeps two identical searches apart when they are different Kafka records', async () => {
    const event = { query: 'klima', resultCount: 2, occurredAt: '2026-06-19T10:00:00Z' }

    expect(await repository.insertSearchEvent(event, source('5'))).toBe(true)
    expect(await repository.insertSearchEvent(event, source('6'))).toBe(true)
    expect(await repository.insertSearchEvent(event, source('5', 1))).toBe(true)

    const { rows } = await pool.query('SELECT count(*)::int AS n FROM search_analytics')
    expect(rows[0].n).toBe(3)
  })

  it('stores a page view once per Kafka record', async () => {
    const view = { path: '/article/1', occurredAt: '2026-06-20T10:00:00Z' }
    const origin: MessageSource = { topic: 'newscore.user.events', partition: 0, offset: '1' }

    expect(await repository.insertPageView(view, origin)).toBe(true)
    expect(await repository.insertPageView(view, origin)).toBe(false)

    const { rows } = await pool.query('SELECT path FROM page_view_analytics')
    expect(rows).toEqual([{ path: '/article/1' }])
  })
})
