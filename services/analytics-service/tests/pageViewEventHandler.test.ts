import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AnalyticsRepository } from '../src/analyticsRepository'
import { handlePageViewEvent } from '../src/pageViewEventHandler'

function createRepository() {
  return {
    insertSearchEvent: vi.fn().mockResolvedValue(undefined),
    insertPageView: vi.fn().mockResolvedValue(undefined),
    close: vi.fn().mockResolvedValue(undefined),
  } satisfies AnalyticsRepository & { insertPageView: ReturnType<typeof vi.fn> }
}

describe('handlePageViewEvent', () => {
  let repository: ReturnType<typeof createRepository>

  beforeEach(() => {
    repository = createRepository()
  })

  it('records a valid page view', async () => {
    const value = JSON.stringify({ path: '/article/1', occurredAt: '2026-06-20T10:00:00Z' })

    const recorded = await handlePageViewEvent(value, repository)

    expect(recorded).toBe(true)
    expect(repository.insertPageView).toHaveBeenCalledWith({
      path: '/article/1',
      occurredAt: '2026-06-20T10:00:00Z',
    })
  })

  it('defaults occurredAt when it is missing', async () => {
    await handlePageViewEvent(JSON.stringify({ path: '/' }), repository)

    expect(repository.insertPageView).toHaveBeenCalledWith(
      expect.objectContaining({ path: '/', occurredAt: expect.any(String) }),
    )
  })

  it('skips a null payload, invalid JSON and a missing path', async () => {
    expect(await handlePageViewEvent(null, repository)).toBe(false)
    expect(await handlePageViewEvent('not-json', repository)).toBe(false)
    expect(await handlePageViewEvent(JSON.stringify({ foo: 'bar' }), repository)).toBe(false)
    expect(repository.insertPageView).not.toHaveBeenCalled()
  })
})
