import { Kafka } from 'kafkajs'
import type { AnalyticsConfig } from './config'
import type { AnalyticsRepository } from './analyticsRepository'
import { handleSearchEvent } from './searchEventHandler'

/**
 * Starts a Kafka consumer that records search events into the analytics repository.
 *
 * @param config     runtime configuration
 * @param repository the analytics repository
 * @returns a stop function that disconnects the consumer
 */
export async function startSearchEventConsumer(
  config: AnalyticsConfig,
  repository: AnalyticsRepository,
): Promise<() => Promise<void>> {
  const kafka = new Kafka({ clientId: config.kafkaGroupId, brokers: config.kafkaBrokers })
  const consumer = kafka.consumer({ groupId: config.kafkaGroupId })

  await consumer.connect()
  await consumer.subscribe({ topic: config.kafkaTopic, fromBeginning: true })

  await consumer.run({
    eachMessage: async ({ message }) => {
      try {
        const recorded = await handleSearchEvent(message.value?.toString() ?? null, repository)
        if (!recorded) {
          console.warn('analytics-service: skipped unprocessable search event')
        }
      } catch (error) {
        // Never crash-loop on a single bad record; log and move on (a real DLQ would follow).
        console.error('analytics-service: failed to record search event, skipping', error)
      }
    },
  })

  return async () => {
    await consumer.disconnect()
  }
}
