import type { AnalyticsRepository } from './analyticsRepository'
import { resolveEventId } from './eventId'
import type { MessageSource, Outcome, PageViewEvent } from './types'

/**
 * Records a page-view event from an Avro-decoded message. Pure with respect to I/O. A structurally
 * invalid payload is skipped; a repository failure is not caught here, so the caller can let Kafka
 * redeliver the message.
 *
 * @param event      the decoded event (shape not yet validated)
 * @param source     the Kafka record the event came from
 * @param repository the analytics repository
 */
export async function handlePageViewEvent(
  event: unknown,
  source: MessageSource,
  repository: AnalyticsRepository,
): Promise<Outcome> {
  if (!event || typeof event !== 'object') {
    return 'skipped'
  }

  const e = event as Partial<PageViewEvent>
  if (typeof e.path !== 'string' || e.path.length === 0) {
    return 'skipped'
  }

  const inserted = await repository.insertPageView(
    {
      eventId: resolveEventId(e.eventId, source),
      path: e.path,
      occurredAt: e.occurredAt ?? new Date().toISOString(),
    },
    source,
  )
  return inserted ? 'recorded' : 'duplicate'
}
