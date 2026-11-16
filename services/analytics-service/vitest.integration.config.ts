import { defineConfig } from 'vitest/config'

// Integration tests start a PostgreSQL container (Testcontainers) and therefore need Docker.
export default defineConfig({
  test: {
    environment: 'node',
    include: ['tests/integration/**/*.int.test.ts'],
    testTimeout: 30_000,
    hookTimeout: 120_000,
    reporters: ['default', 'junit'],
    outputFile: { junit: 'test-results/junit-integration.xml' },
  },
})
