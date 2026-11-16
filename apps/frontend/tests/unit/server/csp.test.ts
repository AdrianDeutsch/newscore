import { createHash } from 'node:crypto'
import { describe, expect, it } from 'vitest'
import { buildContentSecurityPolicy, inlineScriptHashes } from '~/server/utils/csp'

const sha256 = (content: string) => `'sha256-${createHash('sha256').update(content).digest('base64')}'`

describe('inlineScriptHashes', () => {
  it('hashes the content of an executable inline script', () => {
    const config = 'window.__NUXT__={};window.__NUXT__.config={public:{}}'

    expect(inlineScriptHashes([`<script>${config}</script>`])).toEqual([sha256(config)])
  })

  it('ignores external scripts and JSON data blocks', () => {
    const fragments = [
      '<script type="module" src="/_nuxt/entry.js" crossorigin></script>',
      '<script type="application/json" id="__NUXT_DATA__">[{"a":1}]</script>',
      '<script type="application/ld+json">{"@type":"NewsArticle"}</script>',
    ]

    expect(inlineScriptHashes(fragments)).toEqual([])
  })

  it('finds scripts across fragments and lists identical ones once', () => {
    const fragments = ['<div><script>a()</script></div>', '<script type="module">b()</script><script>a()</script>']

    expect(inlineScriptHashes(fragments)).toEqual([sha256('a()'), sha256('b()')])
  })
})

describe('buildContentSecurityPolicy', () => {
  it('allows scripts from the own origin and the page\'s inline scripts by hash only', () => {
    const policy = buildContentSecurityPolicy(['<script>boot()</script>'])
    const scriptSrc = policy.split('; ').find((directive) => directive.startsWith('script-src'))

    expect(scriptSrc).toBe(`script-src 'self' ${sha256('boot()')}`)
  })

  it('never falls back to unsafe-inline or unsafe-eval for scripts', () => {
    const scriptSrc = buildContentSecurityPolicy([]).split('; ').find((d) => d.startsWith('script-src'))

    expect(scriptSrc).toBe("script-src 'self'")
  })

  it('keeps requests, forms and framing on the own origin', () => {
    const policy = buildContentSecurityPolicy([])

    expect(policy).toContain("default-src 'self'")
    expect(policy).toContain("connect-src 'self'")
    expect(policy).toContain("form-action 'self'")
    expect(policy).toContain("frame-ancestors 'self'")
    expect(policy).toContain("object-src 'none'")
  })
})
