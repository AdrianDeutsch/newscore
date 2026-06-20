import type { Pool } from 'pg'
import { describe, expect, it, vi } from 'vitest'
import { createAnalyticsRepository } from '../src/analyticsRepository'

function createPool() {
  return {
    query: vi.fn().mockResolvedValue({ rowCount: 1 }),
    end: vi.fn().mockResolvedValue(undefined),
  }
}

describe('createAnalyticsRepository', () => {
  it('inserts a search event with the expected SQL and parameters', async () => {
    const pool = createPool()
    const repository = createAnalyticsRepository(pool as unknown as Pool)

    await repository.insertSearchEvent({ query: 'klima', resultCount: 2, occurredAt: '2026-06-19T10:00:00Z' })

    expect(pool.query).toHaveBeenCalledWith(
      'INSERT INTO search_analytics (query, result_count, occurred_at) VALUES ($1, $2, $3)',
      ['klima', 2, '2026-06-19T10:00:00Z'],
    )
  })

  it('inserts a page view with the expected SQL and parameters', async () => {
    const pool = createPool()
    const repository = createAnalyticsRepository(pool as unknown as Pool)

    await repository.insertPageView({ path: '/article/1', occurredAt: '2026-06-20T10:00:00Z' })

    expect(pool.query).toHaveBeenCalledWith(
      'INSERT INTO page_view_analytics (path, occurred_at) VALUES ($1, $2)',
      ['/article/1', '2026-06-20T10:00:00Z'],
    )
  })

  it('closes the underlying pool', async () => {
    const pool = createPool()
    const repository = createAnalyticsRepository(pool as unknown as Pool)

    await repository.close()

    expect(pool.end).toHaveBeenCalledOnce()
  })
})
