import { Pool } from 'pg'
import { createAnalyticsRepository } from './analyticsRepository'
import { loadConfig } from './config'
import { startSearchEventConsumer } from './searchEventConsumer'

/**
 * Wires PostgreSQL + Kafka and starts consuming search analytics events.
 */
async function main(): Promise<void> {
  const config = loadConfig()
  const pool = new Pool(config.postgres)
  const repository = createAnalyticsRepository(pool)

  const stop = await startSearchEventConsumer(config, repository)
  console.log(`analytics-service: consuming "${config.kafkaTopic}" -> PostgreSQL`)

  const shutdown = async (): Promise<void> => {
    console.log('analytics-service: shutting down')
    await stop()
    await repository.close()
    process.exit(0)
  }
  process.on('SIGTERM', shutdown)
  process.on('SIGINT', shutdown)
}

main().catch((error) => {
  console.error('analytics-service: failed to start', error)
  process.exit(1)
})
