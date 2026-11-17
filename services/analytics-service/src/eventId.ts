import type { MessageSource } from './types'

/**
 * The idempotency key for a stored event: the producer's eventId, or — for messages written before
 * the schema had one (Avro default "") — a key derived from the Kafka coordinates.
 */
export function resolveEventId(eventId: unknown, source: MessageSource): string {
  if (typeof eventId === 'string' && eventId.length > 0) {
    return eventId
  }
  return `kafka:${source.topic}:${source.partition}:${source.offset}`
}
