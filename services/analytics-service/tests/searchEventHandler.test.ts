import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AnalyticsRepository } from '../src/analyticsRepository'
import { handleSearchEvent } from '../src/searchEventHandler'

function createRepository() {
  return {
    insertSearchEvent: vi.fn().mockResolvedValue(undefined),
    insertPageView: vi.fn().mockResolvedValue(undefined),
    close: vi.fn().mockResolvedValue(undefined),
  } satisfies AnalyticsRepository & { insertSearchEvent: ReturnType<typeof vi.fn> }
}

describe('handleSearchEvent', () => {
  let repository: ReturnType<typeof createRepository>

  beforeEach(() => {
    repository = createRepository()
  })

  it('records a valid event', async () => {
    const recorded = await handleSearchEvent(
      { query: 'klima', resultCount: 2, occurredAt: '2026-06-19T10:00:00Z' },
      repository,
    )

    expect(recorded).toBe(true)
    expect(repository.insertSearchEvent).toHaveBeenCalledWith({
      query: 'klima',
      resultCount: 2,
      occurredAt: '2026-06-19T10:00:00Z',
    })
  })

  it('defaults occurredAt when it is missing', async () => {
    await handleSearchEvent({ query: 'x', resultCount: 0 }, repository)

    expect(repository.insertSearchEvent).toHaveBeenCalledWith(
      expect.objectContaining({ query: 'x', resultCount: 0, occurredAt: expect.any(String) }),
    )
  })

  it('skips a null or non-object payload', async () => {
    expect(await handleSearchEvent(null, repository)).toBe(false)
    expect(await handleSearchEvent('nope', repository)).toBe(false)
    expect(repository.insertSearchEvent).not.toHaveBeenCalled()
  })

  it('skips a structurally invalid event', async () => {
    expect(await handleSearchEvent({ foo: 'bar' }, repository)).toBe(false)
    expect(await handleSearchEvent({ query: 'x', resultCount: 'two' }, repository)).toBe(false)
    expect(repository.insertSearchEvent).not.toHaveBeenCalled()
  })
})
