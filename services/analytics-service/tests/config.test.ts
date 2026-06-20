import { describe, expect, it } from 'vitest'
import { loadConfig } from '../src/config'

describe('loadConfig', () => {
  it('falls back to local defaults for an empty environment', () => {
    const config = loadConfig({})

    expect(config.kafkaBrokers).toEqual(['localhost:9092'])
    expect(config.kafkaSearchTopic).toBe('newscore.search.events')
    expect(config.kafkaPageViewTopic).toBe('newscore.user.events')
    expect(config.postgres).toMatchObject({ host: 'localhost', port: 5432, database: 'newscore' })
  })

  it('reads and parses values from the environment', () => {
    const config = loadConfig({
      KAFKA_BROKERS: 'kafka:9092, broker2:9092',
      KAFKA_SEARCH_TOPIC: 'custom.search',
      KAFKA_PAGEVIEW_TOPIC: 'custom.pageview',
      KAFKA_GROUP_ID: 'group-x',
      PGHOST: 'db',
      PGPORT: '6543',
      PGUSER: 'u',
      PGPASSWORD: 'p',
      PGDATABASE: 'analytics',
    })

    expect(config.kafkaBrokers).toEqual(['kafka:9092', 'broker2:9092'])
    expect(config.kafkaSearchTopic).toBe('custom.search')
    expect(config.kafkaPageViewTopic).toBe('custom.pageview')
    expect(config.kafkaGroupId).toBe('group-x')
    expect(config.postgres).toEqual({
      host: 'db',
      port: 6543,
      user: 'u',
      password: 'p',
      database: 'analytics',
    })
  })
})
