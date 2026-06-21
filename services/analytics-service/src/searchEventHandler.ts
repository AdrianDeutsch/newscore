import type { AnalyticsRepository } from './analyticsRepository'
import type { SearchExecutedEvent } from './types'

/**
 * Records a search event from an Avro-decoded message. Pure with respect to I/O (the repository is
 * injected). Structurally invalid payloads are skipped rather than thrown.
 *
 * @param event      the decoded event (shape not yet validated)
 * @param repository the analytics repository
 * @returns true if recorded, false if skipped
 */
export async function handleSearchEvent(
  event: unknown,
  repository: AnalyticsRepository,
): Promise<boolean> {
  if (!event || typeof event !== 'object') {
    return false
  }
  const e = event as Partial<SearchExecutedEvent>
  if (typeof e.query !== 'string' || typeof e.resultCount !== 'number') {
    return false
  }

  await repository.insertSearchEvent({
    query: e.query,
    resultCount: e.resultCount,
    occurredAt: e.occurredAt ?? new Date().toISOString(),
  })
  return true
}
