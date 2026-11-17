import { describe, expect, it } from 'vitest'
import { resolveEventId } from '../src/eventId'

const SOURCE = { topic: 'newscore.search.events', partition: 2, offset: '17' }

describe('resolveEventId', () => {
  it('uses the producer\'s eventId', () => {
    expect(resolveEventId('0b5c7a1e-3f0e-4a8e-9c1d-2f6b7a9e4c11', SOURCE)).toBe('0b5c7a1e-3f0e-4a8e-9c1d-2f6b7a9e4c11')
  })

  it('falls back to the Kafka coordinates for messages without one (Avro default "")', () => {
    expect(resolveEventId('', SOURCE)).toBe('kafka:newscore.search.events:2:17')
    expect(resolveEventId(undefined, SOURCE)).toBe('kafka:newscore.search.events:2:17')
  })
})
