import { createHash } from 'node:crypto'

/** Inline `<script>` elements that the browser executes: no `src`, and not a JSON data block. */
const INLINE_SCRIPT = /<script(?![^>]*\ssrc=)(?![^>]*\stype="application\/(?:ld\+)?json")[^>]*>([\s\S]*?)<\/script>/g

/**
 * Returns the CSP source expressions (`'sha256-…'`) for every executable inline script in the
 * given HTML fragments.
 */
export function inlineScriptHashes(fragments: string[]): string[] {
  const hashes = new Set<string>()
  for (const fragment of fragments) {
    for (const match of fragment.matchAll(INLINE_SCRIPT)) {
      const content = match[1] ?? ''
      if (content.length > 0) {
        hashes.add(`'sha256-${createHash('sha256').update(content).digest('base64')}'`)
      }
    }
  }
  return [...hashes]
}

/**
 * Builds the Content-Security-Policy for a rendered page.
 *
 * Scripts may only come from this origin, plus exactly the inline scripts that are part of this
 * page (Nuxt writes its runtime config as one). They are allowed by hash rather than by
 * `'unsafe-inline'` or a nonce: a hash stays valid when the page is served from the edge cache,
 * and an injected script would have a different hash.
 */
export function buildContentSecurityPolicy(fragments: string[]): string {
  const scriptSources = ["'self'", ...inlineScriptHashes(fragments)]
  return [
    "default-src 'self'",
    `script-src ${scriptSources.join(' ')}`,
    "style-src 'self' 'unsafe-inline'",
    "img-src 'self' data:",
    "connect-src 'self'",
    "base-uri 'self'",
    "form-action 'self'",
    "frame-ancestors 'self'",
    "object-src 'none'",
  ].join('; ')
}
