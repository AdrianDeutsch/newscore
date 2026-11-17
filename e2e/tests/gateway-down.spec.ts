import { execFileSync } from 'node:child_process'
import { expect, test } from '../support/fixtures'

/**
 * What the frontend answers when the gateway is unreachable. A second frontend container from the
 * image the compose stack built, pointed at an address where nothing listens, so the running stack
 * stays untouched. A 200 page with an error text would be stored by the edge cache and served in
 * place of the last good copy; a 502 is not stored (infrastructure/varnish/tests/caching.vtc).
 */
const name = `newscore-e2e-frontend-no-gateway-${process.pid}`
const port = 3099
const base = `http://localhost:${port}`

test.describe('frontend without a reachable gateway', () => {
  test.beforeAll(async () => {
    execFileSync('docker', [
      'run', '--detach', '--rm', '--name', name, '--publish', `${port}:3000`,
      '--env', 'NUXT_GATEWAY_URL=http://127.0.0.1:9',
      process.env.E2E_FRONTEND_IMAGE ?? 'newscore/frontend:dev',
    ])
    await expect
      .poll(async () => {
        try {
          return (await fetch(`${base}/`)).status
        } catch {
          return 0
        }
      }, { timeout: 30_000 })
      .not.toBe(0)
  })

  test.afterAll(() => {
    execFileSync('docker', ['stop', name])
  })

  for (const path of ['/', '/?category=politik', '/search?q=klima', '/article/1']) {
    test(`${path} answers 502 with the error page`, async ({ request }) => {
      const response = await request.get(`${base}${path}`)

      expect(response.status()).toBe(502)
      const html = await response.text()
      expect(html).toContain('Inhalte konnten nicht geladen werden')
      expect(html).not.toContain('Keine Treffer')
    })
  }
})
