import type { AnalyticsRepository } from './analyticsRepository'
import type { PageViewEvent } from './types'

/**
 * Records a page-view event from an Avro-decoded message. Pure with respect to I/O. Structurally
 * invalid payloads are skipped rather than thrown.
 *
 * @param event      the decoded event (shape not yet validated)
 * @param repository the analytics repository
 * @returns true if recorded, false if skipped
 */
export async function handlePageViewEvent(
  event: unknown,
  repository: AnalyticsRepository,
): Promise<boolean> {
  if (!event || typeof event !== 'object') {
    return false
  }
  const e = event as Partial<PageViewEvent>
  if (typeof e.path !== 'string' || e.path.length === 0) {
    return false
  }

  await repository.insertPageView({
    path: e.path,
    occurredAt: e.occurredAt ?? new Date().toISOString(),
  })
  return true
}
