/**
 * Forwards the browser's GraphQL requests to the api-gateway. The browser only knows this site's
 * origin; where the gateway lives is server-side configuration (NUXT_GATEWAY_URL).
 */
export default defineEventHandler((event) => {
  return proxyRequest(event, `${useRuntimeConfig(event).gatewayUrl}/graphql`)
})
