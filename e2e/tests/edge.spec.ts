import { expect, test } from '../support/fixtures'
import { cacheStatus, emitArticleEvent, stack } from '../support/stack'

/** Requests a path until Varnish answers it from the cache. */
const warm = async (path: string): Promise<void> => {
  await expect.poll(() => cacheStatus(path)).toBe('HIT')
}

/**
 * The edge in front of the frontend: Apache (headers, compression, allowed methods) and Varnish
 * (cache, invalidation). Needs the stack started with the `edge` profile and
 * NEWSCORE_VARNISH_PURGE_ENABLED=true.
 */
test.describe('edge cache', () => {
  test('a page is fetched once and then served from the cache', async () => {
    const path = `/article/3?cache=${Date.now()}`

    expect(await cacheStatus(path)).toBe('MISS')
    expect(await cacheStatus(path)).toBe('HIT')
  })

  test('an article event invalidates that article and the start page, including filtered views', async () => {
    await warm('/article/4')
    await warm('/')
    await warm('/?category=technik')
    await warm('/article/5')

    await emitArticleEvent('4', 'update')

    await expect.poll(() => cacheStatus('/article/4'), { timeout: 20_000 }).toBe('MISS')
    expect(await cacheStatus('/')).toBe('MISS')
    expect(await cacheStatus('/?category=technik')).toBe('MISS')
    // An article the event was not about stays cached.
    expect(await cacheStatus('/article/5')).toBe('HIT')
  })

  test('nobody outside can empty the cache', async () => {
    await warm('/article/7')

    const purge = await fetch(`${stack.edge}/article/7`, { method: 'PURGE' })

    expect(purge.status).toBe(403)
    expect(await cacheStatus('/article/7')).toBe('HIT')
  })

  test('a missing article is answered with 404 through the cache as well', async () => {
    const first = await fetch(`${stack.edge}/article/missing-${Date.now()}`)
    await first.arrayBuffer()

    expect(first.status).toBe(404)
  })

  test('responses carry the security headers and are compressed', async () => {
    const response = await fetch(`${stack.edge}/`, { headers: { 'accept-encoding': 'gzip' } })
    await response.arrayBuffer()

    expect(response.headers.get('strict-transport-security')).toBe('max-age=31536000; includeSubDomains')
    expect(response.headers.get('x-content-type-options')).toBe('nosniff')
    expect(response.headers.get('x-frame-options')).toBe('SAMEORIGIN')
    expect(response.headers.get('referrer-policy')).toBe('strict-origin-when-cross-origin')
    expect(response.headers.get('content-security-policy')).toContain("script-src 'self' 'sha256-")
    expect(response.headers.get('content-encoding')).toBe('gzip')
  })
})
