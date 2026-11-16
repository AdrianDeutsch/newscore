/**
 * Shape of the `newscore.search.events` payload produced by the api-gateway
 * (Avro record `de.newscore.kafka.avro.SearchExecutedEvent`, decoded via the Schema Registry).
 */
export interface SearchExecutedEvent {
  query: string
  resultCount: number
  occurredAt: string
}

/**
 * Shape of the `newscore.user.events` payload (Avro record `de.newscore.kafka.avro.PageViewEvent`).
 */
export interface PageViewEvent {
  path: string
  occurredAt: string
}

/**
 * Where a message came from. Topic, partition and offset together identify one Kafka record, which
 * is what makes a second delivery of the same record recognisable.
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
