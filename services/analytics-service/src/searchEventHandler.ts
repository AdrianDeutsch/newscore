import type { AnalyticsRepository } from './analyticsRepository'
import type { MessageSource, Outcome, SearchExecutedEvent } from './types'

/**
 * Records a search event from an Avro-decoded message. Pure with respect to I/O (the repository is
 * injected). A structurally invalid payload is skipped; a repository failure is not caught here, so
 * the caller can let Kafka redeliver the message.
 *
 * @param event      the decoded event (shape not yet validated)
 * @param source     the Kafka record the event came from
 * @param repository the analytics repository
 */
export async function handleSearchEvent(
  event: unknown,
  source: MessageSource,
  repository: AnalyticsRepository,
): Promise<Outcome> {
  if (!event || typeof event !== 'object') {
    return 'skipped'
  }

  const e = event as Partial<SearchExecutedEvent>
  if (typeof e.query !== 'string' || typeof e.resultCount !== 'number') {
    return 'skipped'
  }

  const inserted = await repository.insertSearchEvent(
    {
      query: e.query,
      resultCount: e.resultCount,
      occurredAt: e.occurredAt ?? new Date().toISOString(),
    },
    source,
  )
  return inserted ? 'recorded' : 'duplicate'
}
