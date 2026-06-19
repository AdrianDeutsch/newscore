import type { AnalyticsRepository } from './analyticsRepository'
import type { SearchExecutedEvent } from './types'

/**
 * Parses a raw Kafka message value and records the search event.
 *
 * <p>Pure with respect to I/O (the repository is injected), so it is fully unit-testable. Returns
 * whether the event was recorded; unprocessable payloads (empty, non-JSON, structurally invalid)
 * are skipped rather than throwing, so a single bad message does not crash the consumer.</p>
 *
 * @param rawValue   the raw message value (or null)
 * @param repository the analytics repository
 * @returns true if the event was recorded, false if it was skipped
 */
export async function handleSearchEvent(
  rawValue: string | null,
  repository: AnalyticsRepository,
): Promise<boolean> {
  if (!rawValue) {
    return false
  }

  let parsed: Partial<SearchExecutedEvent>
  try {
    parsed = JSON.parse(rawValue) as Partial<SearchExecutedEvent>
  } catch {
    return false
  }

  if (typeof parsed.query !== 'string' || typeof parsed.resultCount !== 'number') {
    return false
  }

  await repository.insertSearchEvent({
    query: parsed.query,
    resultCount: parsed.resultCount,
    occurredAt: parsed.occurredAt ?? new Date().toISOString(),
  })
  return true
}
