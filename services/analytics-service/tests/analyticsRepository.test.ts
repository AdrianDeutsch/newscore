import type { Pool } from 'pg'
import { describe, expect, it, vi } from 'vitest'
import { INSERT_PAGE_VIEW, INSERT_SEARCH_EVENT, createAnalyticsRepository } from '../src/analyticsRepository'
import type { MessageSource } from '../src/types'

const SOURCE: MessageSource = { topic: 'newscore.search.events', partition: 2, offset: '41' }

function createPool(rowCount = 1) {
  return {
    query: vi.fn().mockResolvedValue({ rowCount }),
    end: vi.fn().mockResolvedValue(undefined),
  }
}

describe('createAnalyticsRepository', () => {
  it('passes the search event, its eventId and its Kafka origin as parameters', async () => {
    const pool = createPool()
    const repository = createAnalyticsRepository(pool as unknown as Pool)

    const inserted = await repository.insertSearchEvent(
      { eventId: 'e-1', query: 'klima', resultCount: 2, occurredAt: '2026-06-19T10:00:00Z' },
      SOURCE,
    )

    expect(inserted).toBe(true)
    expect(pool.query).toHaveBeenCalledWith(INSERT_SEARCH_EVENT, [
      'e-1',
      'klima',
      2,
      '2026-06-19T10:00:00Z',
      'newscore.search.events',
      2,
      '41',
    ])
  })

  it('passes the page view, its eventId and its Kafka origin as parameters', async () => {
    const pool = createPool()
    const repository = createAnalyticsRepository(pool as unknown as Pool)

    const inserted = await repository.insertPageView(
      { eventId: 'e-2', path: '/article/1', occurredAt: '2026-06-20T10:00:00Z' },
      SOURCE,
    )

    expect(inserted).toBe(true)
    expect(pool.query).toHaveBeenCalledWith(INSERT_PAGE_VIEW, [
      'e-2',
      '/article/1',
      '2026-06-20T10:00:00Z',
      'newscore.search.events',
      2,
      '41',
    ])
  })

  it('reports false when the database ignored the insert as a duplicate event', async () => {
    const repository = createAnalyticsRepository(createPool(0) as unknown as Pool)

    expect(
      await repository.insertSearchEvent({ eventId: 'e-3', query: 'x', resultCount: 0, occurredAt: '2026-06-19T10:00:00Z' }, SOURCE),
    ).toBe(false)
    expect(await repository.insertPageView({ eventId: 'e-4', path: '/', occurredAt: '2026-06-19T10:00:00Z' }, SOURCE)).toBe(false)
  })

  it('closes the underlying pool', async () => {
    const pool = createPool()
    const repository = createAnalyticsRepository(pool as unknown as Pool)

    await repository.close()

    expect(pool.end).toHaveBeenCalledOnce()
  })
})
