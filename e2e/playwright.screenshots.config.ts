import { defineConfig, devices } from '@playwright/test'
import { stack } from './support/stack'

/**
 * Not a test run: regenerates the README images in docs/images from the running stack
 * (`npm run screenshots`), so they show the application as it is. Kept apart from the test config
 * because it records video and writes into the repository.
 */
export default defineConfig({
  testDir: './screenshots',
  workers: 1,
  reporter: [['list']],
  outputDir: './test-results/screenshots',
  use: {
    ...devices['Desktop Chrome'],
    baseURL: stack.frontend,
    viewport: { width: 1280, height: 800 },
    locale: 'de-DE',
    timezoneId: 'Europe/Berlin',
    video: { mode: 'on', size: { width: 1280, height: 800 } },
  },
})
