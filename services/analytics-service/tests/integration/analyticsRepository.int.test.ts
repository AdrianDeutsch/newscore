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
  const search = (eventId: string) => ({ eventId, query: 'klima', resultCount: 2, occurredAt: '2026-06-19T10:00:00Z' })
  const count = async (table: string): Promise<number> =>
    (await pool.query(`SELECT count(*)::int AS n FROM ${table}`)).rows[0].n

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

  it('stores a search event with its id, timestamp and origin', async () => {
    const inserted = await repository.insertSearchEvent(search('e-1'), source('5'))

    const { rows } = await pool.query(
      'SELECT event_id, query, result_count, occurred_at, source_partition, source_offset FROM search_analytics',
    )
    expect(inserted).toBe(true)
    expect(rows).toHaveLength(1)
    expect(rows[0].event_id).toBe('e-1')
    expect(rows[0].query).toBe('klima')
    expect(rows[0].result_count).toBe(2)
    expect((rows[0].occurred_at as Date).toISOString()).toBe('2026-06-19T10:00:00.000Z')
    expect(rows[0].source_partition).toBe(0)
    expect(rows[0].source_offset).toBe('5')
  })

  it('stores a redelivered event only once', async () => {
    expect(await repository.insertSearchEvent(search('e-1'), source('5'))).toBe(true)
    expect(await repository.insertSearchEvent(search('e-1'), source('5'))).toBe(false)

    expect(await count('search_analytics')).toBe(1)
  })

  it('keeps two identical searches apart: they are two events', async () => {
    expect(await repository.insertSearchEvent(search('e-1'), source('5'))).toBe(true)
    expect(await repository.insertSearchEvent(search('e-2'), source('6'))).toBe(true)

    expect(await count('search_analytics')).toBe(2)
  })

  it('stores a new event that arrives under the coordinates of an old one (topic recreated, offsets from 0)', async () => {
    // Before a broker restart without persistent storage ...
    expect(await repository.insertSearchEvent(search('before-restart'), source('0'))).toBe(true)
    // ... and after it: same topic, partition and offset, different event.
    expect(await repository.insertSearchEvent(search('after-restart'), source('0'))).toBe(true)

    expect(await count('search_analytics')).toBe(2)
  })

  it('stores a page view once per event', async () => {
    const view = { eventId: 'p-1', path: '/article/1', occurredAt: '2026-06-20T10:00:00Z' }
    const origin: MessageSource = { topic: 'newscore.user.events', partition: 0, offset: '1' }

    expect(await repository.insertPageView(view, origin)).toBe(true)
    expect(await repository.insertPageView(view, origin)).toBe(false)
    expect(await repository.insertPageView({ ...view, eventId: 'p-2' }, origin)).toBe(true)

    const { rows } = await pool.query('SELECT event_id FROM page_view_analytics ORDER BY id')
    expect(rows).toEqual([{ event_id: 'p-1' }, { event_id: 'p-2' }])
  })
})
