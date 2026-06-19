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
    globals: true,
    include: ['tests/unit/**/*.test.ts'],
    reporters: ['default', 'junit'],
    outputFile: { junit: 'test-results/junit.xml' },
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'cobertura'],
      reportsDirectory: 'coverage',
      // Scope coverage to the unit-testable layer (pure logic + presentational components).
      // Nuxt-context glue (plugins, composables, pages) is covered by integration/E2E later.
      include: ['components/**', 'services/**'],
      thresholds: {
        lines: 80,
        functions: 80,
        statements: 80,
        branches: 70,
      },
    },
  },
})
