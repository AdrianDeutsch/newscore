/**
 * Runtime configuration for the analytics-service, resolved from environment variables with
 * sensible local defaults.
 */
export interface AnalyticsConfig {
  kafkaBrokers: string[]
  kafkaSearchTopic: string
  kafkaPageViewTopic: string
  kafkaGroupId: string
  postgres: {
    host: string
    port: number
    user: string
    password: string
    database: string
  }
}

/**
 * Builds the configuration from the given environment (defaults to `process.env`).
 *
 * @param env environment variables
 */
export function loadConfig(env: NodeJS.ProcessEnv = process.env): AnalyticsConfig {
  return {
    kafkaBrokers: (env.KAFKA_BROKERS ?? 'localhost:9092').split(',').map((broker) => broker.trim()),
    kafkaSearchTopic: env.KAFKA_SEARCH_TOPIC ?? 'newscore.search.events',
    kafkaPageViewTopic: env.KAFKA_PAGEVIEW_TOPIC ?? 'newscore.user.events',
    kafkaGroupId: env.KAFKA_GROUP_ID ?? 'analytics-service',
    postgres: {
      host: env.PGHOST ?? 'localhost',
      port: Number(env.PGPORT ?? '5432'),
      user: env.PGUSER ?? 'newscore',
      password: env.PGPASSWORD ?? 'newscore',
      database: env.PGDATABASE ?? 'newscore',
    },
  }
}
