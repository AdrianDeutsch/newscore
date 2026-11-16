// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-06-01',
  devtools: { enabled: true },

  css: ['~/assets/css/main.css'],

  // Base URL of the api-gateway. Server-only: the browser never talks to the gateway directly. During
  // SSR the urql client calls it straight away; in the browser GraphQL requests and page-view beacons
  // go to this site's own /graphql and /events/pageview, which server/routes/ forwards to the gateway.
  // That keeps everything same-origin (no CORS, a strict Content-Security-Policy) and means the
  // gateway does not have to be reachable from outside. Override with NUXT_GATEWAY_URL.
  runtimeConfig: {
    gatewayUrl: 'http://localhost:8080',
  },

  app: {
    head: {
      htmlAttrs: { lang: 'de' },
      title: 'NewsCore',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: 'NewsCore — Demo-Nachrichtenportal: Nuxt SSR über ein GraphQL-Gateway.' },
      ],
    },
  },
})
