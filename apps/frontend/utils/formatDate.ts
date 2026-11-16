/**
 * The time zone all dates on the site are shown in. It is fixed rather than taken from the runtime:
 * the server renders in the container's zone (UTC) and the browser hydrates in the reader's, and a
 * page whose text differs between the two no longer hydrates cleanly — and shows a different time
 * depending on where it was rendered.
 */
export const SITE_TIME_ZONE = 'Europe/Berlin'

const dateFormat = new Intl.DateTimeFormat('de-DE', { dateStyle: 'medium', timeZone: SITE_TIME_ZONE })
const dateTimeFormat = new Intl.DateTimeFormat('de-DE', {
  dateStyle: 'long',
  timeStyle: 'short',
  timeZone: SITE_TIME_ZONE,
})

/** Formats an ISO-8601 instant as a date in the site's time zone, e.g. "18.06.2026". */
export function formatDate(isoDateTime: string): string {
  return dateFormat.format(new Date(isoDateTime))
}

/** Formats an ISO-8601 instant as date and time in the site's time zone, e.g. "18. Juni 2026 um 09:15". */
export function formatDateTime(isoDateTime: string): string {
  return dateTimeFormat.format(new Date(isoDateTime))
}
