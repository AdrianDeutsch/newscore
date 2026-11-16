import { buildContentSecurityPolicy } from '../utils/csp'

/**
 * Sends a Content-Security-Policy with every server-rendered page. It is computed here, from the
 * HTML that is about to be sent, because only the application knows which inline scripts belong to
 * the page (see server/utils/csp.ts). Not active in development: the Vite dev server relies on
 * inline scripts and eval for hot module replacement.
 */
export default defineNitroPlugin((nitroApp) => {
  if (import.meta.dev) {
    return
  }
  nitroApp.hooks.hook('render:html', (html, { event }) => {
    const fragments = [...html.head, ...html.bodyPrepend, ...html.body, ...html.bodyAppend]
    setResponseHeader(event, 'Content-Security-Policy', buildContentSecurityPolicy(fragments))
  })
})
