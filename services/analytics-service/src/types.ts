/**
 * Shape of the `newscore.search.events` payload produced by the api-gateway
 * (de.newscore.kafka.SearchExecutedEvent serialized as JSON).
 */
export interface SearchExecutedEvent {
  query: string
  resultCount: number
  occurredAt: string
}
