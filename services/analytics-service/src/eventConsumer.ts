import { Kafka } from 'kafkajs'
import type { AnalyticsConfig } from './config'
import type { AnalyticsRepository } from './analyticsRepository'
import { handlePageViewEvent } from './pageViewEventHandler'
import { handleSearchEvent } from './searchEventHandler'

/**
 * Starts a Kafka consumer that records search and page-view events into the analytics repository.
 * Messages are dispatched to the matching handler by topic; a single bad record is logged and
 * skipped (never crash-looped).
 *
 * @param config     runtime configuration
 * @param repository the analytics repository
 * @returns a stop function that disconnects the consumer
 */
export async function startEventConsumer(
  config: AnalyticsConfig,
  repository: AnalyticsRepository,
): Promise<() => Promise<void>> {
  const kafka = new Kafka({ clientId: config.kafkaGroupId, brokers: config.kafkaBrokers })
  const consumer = kafka.consumer({ groupId: config.kafkaGroupId })

  await consumer.connect()
  await consumer.subscribe({ topics: [config.kafkaSearchTopic, config.kafkaPageViewTopic], fromBeginning: true })

  await consumer.run({
    eachMessage: async ({ topic, message }) => {
      const value = message.value?.toString() ?? null
      try {
        const recorded =
          topic === config.kafkaPageViewTopic
            ? await handlePageViewEvent(value, repository)
            : await handleSearchEvent(value, repository)
        if (!recorded) {
          console.warn(`analytics-service: skipped unprocessable message on ${topic}`)
        }
      } catch (error) {
        console.error(`analytics-service: failed to record message on ${topic}, skipping`, error)
      }
    },
  })

  return async () => {
    await consumer.disconnect()
  }
}
