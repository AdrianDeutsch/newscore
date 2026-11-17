/**
 * Shape of the `newscore.search.events` payload produced by the api-gateway
 * (Avro record `de.newscore.kafka.avro.SearchExecutedEvent`, decoded via the Schema Registry).
 */
export interface SearchExecutedEvent {
  /** Unique id set by the producer; the idempotency key in PostgreSQL. */
  eventId: string
  query: string
  resultCount: number
  occurredAt: string
}

/**
 * Shape of the `newscore.user.events` payload (Avro record `de.newscore.kafka.avro.PageViewEvent`).
 */
export interface PageViewEvent {
  /** Unique id set by the producer; the idempotency key in PostgreSQL. */
  eventId: string
  path: string
  occurredAt: string
}

/**
 * Where a message came from (topic, partition, offset). Stored with each row for tracing a row back
 * to its record, and used as a fallback key for messages without eventId. Not the idempotency key
 * itself: offsets start again at 0 when a topic is recreated, e.g. after a broker without
 * persistent storage restarts (docs/defects/DEFECT-010).
 */
export interface MessageSource {
  topic: string
  partition: number
  offset: string
}

/**
 * What happened to a message: `recorded` (new row), `duplicate` (this record was stored before) or
 * `skipped` (the message can never be processed, so retrying it would be pointless).
 */
export type Outcome = 'recorded' | 'duplicate' | 'skipped'
