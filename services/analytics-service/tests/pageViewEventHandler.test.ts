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
    const recorded = await handlePageViewEvent(
      { path: '/article/1', occurredAt: '2026-06-20T10:00:00Z' },
      repository,
    )

    expect(recorded).toBe(true)
    expect(repository.insertPageView).toHaveBeenCalledWith({
      path: '/article/1',
      occurredAt: '2026-06-20T10:00:00Z',
    })
  })

  it('defaults occurredAt when it is missing', async () => {
    await handlePageViewEvent({ path: '/' }, repository)

    expect(repository.insertPageView).toHaveBeenCalledWith(
      expect.objectContaining({ path: '/', occurredAt: expect.any(String) }),
    )
  })

  it('skips null, non-object and structurally invalid payloads', async () => {
    expect(await handlePageViewEvent(null, repository)).toBe(false)
    expect(await handlePageViewEvent('nope', repository)).toBe(false)
    expect(await handlePageViewEvent({ foo: 'bar' }, repository)).toBe(false)
    expect(repository.insertPageView).not.toHaveBeenCalled()
  })
})
