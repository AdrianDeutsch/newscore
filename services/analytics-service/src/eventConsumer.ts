import { SchemaRegistry } from '@kafkajs/confluent-schema-registry'
import { Kafka } from 'kafkajs'
import type { AnalyticsConfig } from './config'
import type { AnalyticsRepository } from './analyticsRepository'
import { createMessageProcessor } from './messageProcessor'

/**
 * Starts a Kafka consumer that records search and page-view events into the analytics repository.
 * Messages are Avro, decoded via the Confluent Schema Registry (ADR-011). What happens to a message
 * that cannot be processed is decided in {@link createMessageProcessor}; an error thrown from there
 * makes kafkajs retry the message instead of committing its offset.
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
  const registry = new SchemaRegistry({ host: config.schemaRegistryUrl })
  // Never let this consumer create the topics as a side effect of subscribing. The gateway declares
  // them (with their partition count); a topic auto-created here first would start with the broker
  // default of one partition, this consumer would be assigned only that one, and after the gateway
  // raised the count the events on the new partitions would sit unread until the next metadata
  // refresh minutes later (docs/defects/DEFECT-005). Subscribing fails instead until the topics exist.
  const consumer = kafka.consumer({ groupId: config.kafkaGroupId, allowAutoTopicCreation: false })
  const processMessage = createMessageProcessor({
    decode: (value) => registry.decode(value),
    repository,
    pageViewTopic: config.kafkaPageViewTopic,
  })

  await consumer.connect()
  await consumer.subscribe({ topics: [config.kafkaSearchTopic, config.kafkaPageViewTopic], fromBeginning: true })

  await consumer.run({
    eachMessage: async ({ topic, partition, message }) => {
      await processMessage({ topic, partition, offset: message.offset }, message.value)
    },
  })

  return async () => {
    await consumer.disconnect()
  }
}
