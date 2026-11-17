import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AnalyticsRepository } from '../src/analyticsRepository'
import { handleSearchEvent } from '../src/searchEventHandler'
import type { MessageSource } from '../src/types'

const SOURCE: MessageSource = { topic: 'newscore.search.events', partition: 0, offset: '7' }
const EVENT_ID = '0b5c7a1e-3f0e-4a8e-9c1d-2f6b7a9e4c11'

function createRepository() {
  return {
    insertSearchEvent: vi.fn().mockResolvedValue(true),
    insertPageView: vi.fn().mockResolvedValue(true),
    close: vi.fn().mockResolvedValue(undefined),
  } satisfies AnalyticsRepository
}

describe('handleSearchEvent', () => {
  let repository: ReturnType<typeof createRepository>

  beforeEach(() => {
    repository = createRepository()
  })

  it('records a valid event with its eventId and the Kafka record it came from', async () => {
    const outcome = await handleSearchEvent(
      { eventId: EVENT_ID, query: 'klima', resultCount: 2, occurredAt: '2026-06-19T10:00:00Z' },
      SOURCE,
      repository,
    )

    expect(outcome).toBe('recorded')
    expect(repository.insertSearchEvent).toHaveBeenCalledWith(
      { eventId: EVENT_ID, query: 'klima', resultCount: 2, occurredAt: '2026-06-19T10:00:00Z' },
      SOURCE,
    )
  })

  it('uses the Kafka coordinates as key for an event without eventId', async () => {
    await handleSearchEvent({ eventId: '', query: 'klima', resultCount: 2 }, SOURCE, repository)

    expect(repository.insertSearchEvent).toHaveBeenCalledWith(
      expect.objectContaining({ eventId: 'kafka:newscore.search.events:0:7' }),
      SOURCE,
    )
  })

  it('reports a duplicate when the repository had the record already', async () => {
    repository.insertSearchEvent.mockResolvedValue(false)

    expect(await handleSearchEvent({ query: 'klima', resultCount: 2 }, SOURCE, repository)).toBe('duplicate')
  })

  it('defaults occurredAt when it is missing', async () => {
    await handleSearchEvent({ query: 'x', resultCount: 0 }, SOURCE, repository)

    expect(repository.insertSearchEvent).toHaveBeenCalledWith(
      expect.objectContaining({ query: 'x', resultCount: 0, occurredAt: expect.any(String) }),
      SOURCE,
    )
  })

  it('skips a null or non-object payload', async () => {
    expect(await handleSearchEvent(null, SOURCE, repository)).toBe('skipped')
    expect(await handleSearchEvent('nope', SOURCE, repository)).toBe('skipped')
    expect(repository.insertSearchEvent).not.toHaveBeenCalled()
  })

  it('skips a structurally invalid event', async () => {
    expect(await handleSearchEvent({ foo: 'bar' }, SOURCE, repository)).toBe('skipped')
    expect(await handleSearchEvent({ query: 'x', resultCount: 'two' }, SOURCE, repository)).toBe('skipped')
    expect(repository.insertSearchEvent).not.toHaveBeenCalled()
  })

  it('does not swallow a repository failure', async () => {
    repository.insertSearchEvent.mockRejectedValue(new Error('connection refused'))

    await expect(handleSearchEvent({ query: 'x', resultCount: 1 }, SOURCE, repository)).rejects.toThrow(
      'connection refused',
    )
  })
})
