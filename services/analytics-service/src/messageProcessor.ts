import type { AnalyticsRepository } from './analyticsRepository'
import { handlePageViewEvent } from './pageViewEventHandler'
import { handleSearchEvent } from './searchEventHandler'
import type { MessageSource, Outcome } from './types'

/** First byte of every message written with a Confluent serializer, followed by a 4-byte schema id. */
const CONFLUENT_MAGIC_BYTE = 0
const CONFLUENT_HEADER_LENGTH = 5

export interface MessageProcessorDeps {
  /** Decodes a Confluent-framed Avro payload (Schema Registry lookup included). */
  decode: (value: Buffer) => Promise<unknown>
  repository: AnalyticsRepository
  pageViewTopic: string
  warn?: (message: string) => void
}

/**
 * Builds the function that handles one Kafka message.
 *
 * It separates two kinds of failure, because they need opposite reactions:
 *
 * - A message that can never succeed (no payload, not Confluent-framed, wrong shape) is skipped.
 *   Retrying would block the partition for good.
 * - Everything else — the Schema Registry or PostgreSQL being unreachable — is thrown. kafkajs then
 *   does not commit the offset and delivers the message again, so an outage delays analytics rows
 *   instead of losing them. The earlier version caught every error and moved on, which dropped all
 *   events for the length of a database outage (see docs/defects/DEFECT-003).
 */
export function createMessageProcessor(deps: MessageProcessorDeps) {
  const warn = deps.warn ?? ((message: string) => console.warn(message))

  return async function processMessage(source: MessageSource, value: Buffer | null): Promise<Outcome> {
    if (!value || value.length < CONFLUENT_HEADER_LENGTH || value[0] !== CONFLUENT_MAGIC_BYTE) {
      warn(`analytics-service: skipped a message without Avro framing (${describe(source)})`)
      return 'skipped'
    }

    const decoded = await deps.decode(value)
    const outcome =
      source.topic === deps.pageViewTopic
        ? await handlePageViewEvent(decoded, source, deps.repository)
        : await handleSearchEvent(decoded, source, deps.repository)

    if (outcome === 'skipped') {
      warn(`analytics-service: skipped a message with an unexpected shape (${describe(source)})`)
    }
    return outcome
  }
}

function describe(source: MessageSource): string {
  return `${source.topic}[${source.partition}]@${source.offset}`
}
