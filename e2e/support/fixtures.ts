import { test as base, expect } from '@playwright/test'

/**
 * `page` with a tripwire: any uncaught exception or console error in the browser fails the test,
 * unless the test declares it as expected. This is what catches a Content-Security-Policy that
 * blocks the application's own scripts or requests — the page still looks rendered (SSR), but the
 * browser logs a violation and the app never becomes interactive.
 */
export const test = base.extend<{ allowedConsoleErrors: RegExp[] }>({
  allowedConsoleErrors: [[], { option: true }],

  page: async ({ page, allowedConsoleErrors }, use) => {
    const problems: string[] = []
    page.on('pageerror', (error) => problems.push(`uncaught: ${error.message}`))
    page.on('console', (message) => {
      if (message.type() === 'error' && !allowedConsoleErrors.some((allowed) => allowed.test(message.text()))) {
        problems.push(`console: ${message.text()}`)
      }
    })

    await use(page)

    expect(problems, 'browser console must stay free of errors').toEqual([])
  },
})

export { expect }
