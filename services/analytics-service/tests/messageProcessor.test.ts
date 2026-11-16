import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AnalyticsRepository } from '../src/analyticsRepository'
import { createMessageProcessor } from '../src/messageProcessor'
import type { MessageSource } from '../src/types'

const SEARCH: MessageSource = { topic: 'newscore.search.events', partition: 0, offset: '3' }
const PAGE_VIEW: MessageSource = { topic: 'newscore.user.events', partition: 2, offset: '9' }

/** A Confluent-framed payload: magic byte 0, 4-byte schema id, then the Avro body. */
const FRAMED = Buffer.from([0, 0, 0, 0, 1, 42])

function setup(decoded: unknown = { query: 'klima', resultCount: 2, occurredAt: '2026-06-19T10:00:00Z' }) {
  const repository = {
    insertSearchEvent: vi.fn().mockResolvedValue(true),
    insertPageView: vi.fn().mockResolvedValue(true),
    close: vi.fn().mockResolvedValue(undefined),
  } satisfies AnalyticsRepository
  const decode = vi.fn().mockResolvedValue(decoded)
  const warn = vi.fn()
  const processMessage = createMessageProcessor({
    decode,
    repository,
    pageViewTopic: 'newscore.user.events',
    warn,
  })
  return { repository, decode, warn, processMessage }
}

describe('createMessageProcessor', () => {
  let context: ReturnType<typeof setup>

  beforeEach(() => {
    context = setup()
  })

  it('records a search event from the search topic', async () => {
    expect(await context.processMessage(SEARCH, FRAMED)).toBe('recorded')
    expect(context.repository.insertSearchEvent).toHaveBeenCalledWith(
      { query: 'klima', resultCount: 2, occurredAt: '2026-06-19T10:00:00Z' },
      SEARCH,
    )
    expect(context.repository.insertPageView).not.toHaveBeenCalled()
  })

  it('routes a message from the page-view topic to the page-view table', async () => {
    context = setup({ path: '/article/1', occurredAt: '2026-06-20T10:00:00Z' })

    expect(await context.processMessage(PAGE_VIEW, FRAMED)).toBe('recorded')
    expect(context.repository.insertPageView).toHaveBeenCalledWith(
      { path: '/article/1', occurredAt: '2026-06-20T10:00:00Z' },
      PAGE_VIEW,
    )
    expect(context.repository.insertSearchEvent).not.toHaveBeenCalled()
  })

  it('reports a redelivered record as a duplicate', async () => {
    context.repository.insertSearchEvent.mockResolvedValue(false)

    expect(await context.processMessage(SEARCH, FRAMED)).toBe('duplicate')
  })

  it('skips a message without payload or without Avro framing, without asking the registry', async () => {
    expect(await context.processMessage(SEARCH, null)).toBe('skipped')
    expect(await context.processMessage(SEARCH, Buffer.from('not-avro'))).toBe('skipped')
    expect(await context.processMessage(SEARCH, Buffer.from([0, 0, 0]))).toBe('skipped')

    expect(context.decode).not.toHaveBeenCalled()
    expect(context.repository.insertSearchEvent).not.toHaveBeenCalled()
    expect(context.warn).toHaveBeenCalledTimes(3)
  })

  it('skips a decoded message with an unexpected shape', async () => {
    context = setup({ unexpected: true })

    expect(await context.processMessage(SEARCH, FRAMED)).toBe('skipped')
    expect(context.warn).toHaveBeenCalledWith(expect.stringContaining('newscore.search.events[0]@3'))
  })

  it('throws when PostgreSQL fails, so the message is redelivered instead of lost', async () => {
    context.repository.insertSearchEvent.mockRejectedValue(new Error('connection refused'))

    await expect(context.processMessage(SEARCH, FRAMED)).rejects.toThrow('connection refused')
  })

  it('throws when the Schema Registry fails, so the message is redelivered instead of lost', async () => {
    context.decode.mockRejectedValue(new Error('registry unreachable'))

    await expect(context.processMessage(SEARCH, FRAMED)).rejects.toThrow('registry unreachable')
    expect(context.repository.insertSearchEvent).not.toHaveBeenCalled()
  })
})
