import type { AnalyticsRepository } from './analyticsRepository'
import type { PageViewEvent } from './types'

/**
 * Parses a raw Kafka message value and records the page view. Pure with respect to I/O (the
 * repository is injected). Unprocessable payloads are skipped rather than thrown.
 *
 * @param rawValue   the raw message value (or null)
 * @param repository the analytics repository
 * @returns true if recorded, false if skipped
 */
export async function handlePageViewEvent(
  rawValue: string | null,
  repository: AnalyticsRepository,
): Promise<boolean> {
  if (!rawValue) {
    return false
  }

  let parsed: Partial<PageViewEvent>
  try {
    parsed = JSON.parse(rawValue) as Partial<PageViewEvent>
  } catch {
    return false
  }

  if (typeof parsed.path !== 'string' || parsed.path.length === 0) {
    return false
  }

  await repository.insertPageView({
    path: parsed.path,
    occurredAt: parsed.occurredAt ?? new Date().toISOString(),
  })
  return true
}
