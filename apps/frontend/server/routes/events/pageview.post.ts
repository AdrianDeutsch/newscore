/**
 * Forwards the page-view beacon to the api-gateway (which validates it and emits the Kafka event).
 */
export default defineEventHandler((event) => {
  return proxyRequest(event, `${useRuntimeConfig(event).gatewayUrl}/events/pageview`)
})
