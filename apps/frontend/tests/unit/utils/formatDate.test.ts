import { describe, expect, it } from 'vitest'
import { formatDate, formatDateTime } from '~/utils/formatDate'

// The suite runs with TZ=America/New_York (vitest.config.ts): a formatter that falls back to the
// runtime's zone cannot produce the expected texts below.
describe('formatDate / formatDateTime', () => {
  it('runs in a zone other than the site\'s, so the assertions below mean something', () => {
    expect(new Intl.DateTimeFormat().resolvedOptions().timeZone).toBe('America/New_York')
  })

  it('shows the time in the site\'s zone (Europe/Berlin), in summer and in winter', () => {
    expect(formatDateTime('2026-06-18T07:15:00Z')).toBe('18. Juni 2026 um 09:15')
    expect(formatDateTime('2026-01-18T07:15:00Z')).toBe('18. Januar 2026 um 08:15')
  })

  it('moves the date across midnight with the site\'s zone, not with UTC', () => {
    // 22:30 UTC on the 17th is already the 18th in Berlin (and still the 17th in New York).
    expect(formatDate('2026-06-17T22:30:00Z')).toBe('18.06.2026')
    expect(formatDateTime('2026-06-17T22:30:00Z')).toBe('18. Juni 2026 um 00:30')
  })
})
