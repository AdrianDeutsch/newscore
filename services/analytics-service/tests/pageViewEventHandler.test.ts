import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AnalyticsRepository } from '../src/analyticsRepository'
import { handlePageViewEvent } from '../src/pageViewEventHandler'
import type { MessageSource } from '../src/types'

const SOURCE: MessageSource = { topic: 'newscore.user.events', partition: 1, offset: '12' }
const EVENT_ID = '0b5c7a1e-3f0e-4a8e-9c1d-2f6b7a9e4c11'

function createRepository() {
  return {
    insertSearchEvent: vi.fn().mockResolvedValue(true),
    insertPageView: vi.fn().mockResolvedValue(true),
    close: vi.fn().mockResolvedValue(undefined),
  } satisfies AnalyticsRepository
}

describe('handlePageViewEvent', () => {
  let repository: ReturnType<typeof createRepository>

  beforeEach(() => {
    repository = createRepository()
  })

  it('records a valid page view together with the Kafka record it came from', async () => {
    const outcome = await handlePageViewEvent(
      { eventId: EVENT_ID, path: '/article/1', occurredAt: '2026-06-20T10:00:00Z' },
      SOURCE,
      repository,
    )

    expect(outcome).toBe('recorded')
    expect(repository.insertPageView).toHaveBeenCalledWith(
      { eventId: EVENT_ID, path: '/article/1', occurredAt: '2026-06-20T10:00:00Z' },
      SOURCE,
    )
  })

  it('reports a duplicate when the repository had the record already', async () => {
    repository.insertPageView.mockResolvedValue(false)

    expect(await handlePageViewEvent({ path: '/' }, SOURCE, repository)).toBe('duplicate')
  })

  it('defaults occurredAt when it is missing', async () => {
    await handlePageViewEvent({ path: '/' }, SOURCE, repository)

    expect(repository.insertPageView).toHaveBeenCalledWith(
      expect.objectContaining({ path: '/', occurredAt: expect.any(String) }),
      SOURCE,
    )
  })

  it('skips payloads without a usable path', async () => {
    expect(await handlePageViewEvent(null, SOURCE, repository)).toBe('skipped')
    expect(await handlePageViewEvent({ path: '' }, SOURCE, repository)).toBe('skipped')
    expect(await handlePageViewEvent({ path: 42 }, SOURCE, repository)).toBe('skipped')
    expect(repository.insertPageView).not.toHaveBeenCalled()
  })
})
