import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    environment: 'node',
    include: ['tests/**/*.test.ts'],
    // Integration tests need Docker and have their own config (npm run test:integration).
    exclude: ['tests/integration/**', 'node_modules/**'],
    reporters: ['default', 'junit'],
    outputFile: { junit: 'test-results/junit.xml' },
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'cobertura'],
      reportsDirectory: 'coverage',
      // Everything except the two wiring files that only connect kafkajs and pg (index.ts,
      // eventConsumer.ts). Those run for real in the end-to-end suite (e2e/).
      include: ['src/**/*.ts'],
      exclude: ['src/index.ts', 'src/eventConsumer.ts', 'src/types.ts'],
      thresholds: { lines: 80, functions: 80, statements: 80, branches: 70 },
    },
  },
})
