import { fileURLToPath } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vitest/config'

const rootDir = fileURLToPath(new URL('./', import.meta.url))

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '~': rootDir,
      '@': rootDir,
    },
  },
  test: {
    environment: 'happy-dom',
    // Run in a zone that is neither the site's (Europe/Berlin) nor the usual CI default (UTC), so a
    // date that is formatted without an explicit time zone gives a visibly wrong result everywhere.
    env: { TZ: 'America/New_York' },
    globals: true,
    include: ['tests/unit/**/*.test.ts'],
    reporters: ['default', 'junit'],
    outputFile: { junit: 'test-results/junit.xml' },
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'cobertura'],
      reportsDirectory: 'coverage',
      // Scope coverage to the unit-testable layer (pure logic + presentational components).
      // Nuxt-context glue (plugins, composables, pages, server routes) is not unit-tested; it runs
      // in the browser-driven end-to-end suite (e2e/ at the repository root).
      include: ['components/**', 'services/**', 'server/utils/**', 'utils/**'],
      thresholds: {
        lines: 80,
        functions: 80,
        statements: 80,
        branches: 70,
      },
    },
  },
})
