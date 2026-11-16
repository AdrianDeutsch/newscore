import { defineConfig, devices } from '@playwright/test'
import { stack } from './support/stack'

/**
 * Two browser projects run the same UI flows: once against the frontend directly and once through
 * the edge (Apache -> Varnish), where the security headers and the cache sit in between. The
 * non-browser specs (events, search index, edge cache) run once.
 *
 * Tests run serially: several of them change shared state on purpose (cache contents, the search
 * index) and assert on it.
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: 0,
  timeout: 30_000,
  expect: { timeout: 10_000 },
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }], ['github']] : [['list'], ['html', { open: 'never' }]],
  use: {
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    locale: 'de-DE',
    // Deliberately not the server's zone (UTC) and not the site's (Europe/Berlin): anything the
    // page renders from the runtime's clock or zone then differs between server and browser and
    // shows up as a hydration mismatch in the console, which fails the test.
    timezoneId: 'America/New_York',
  },
  projects: [
    {
      name: 'ui-direct',
      testMatch: /ui\.spec\.ts/,
      use: { ...devices['Desktop Chrome'], baseURL: stack.frontend },
    },
    {
      name: 'ui-edge',
      testMatch: /ui\.spec\.ts/,
      use: { ...devices['Desktop Chrome'], baseURL: stack.edge },
    },
    {
      name: 'stack',
      testIgnore: /ui\.spec\.ts/,
      use: { ...devices['Desktop Chrome'], baseURL: stack.frontend },
    },
  ],
})
