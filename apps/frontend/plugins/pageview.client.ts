/**
 * Sends a fire-and-forget page-view beacon to the gateway on every client-side navigation.
 * Produces the `user.pageview` event (gateway → Kafka → analytics-service → PostgreSQL).
 * Analytics must never block or break navigation, so failures are ignored.
 */
export default defineNuxtPlugin(() => {
  const endpoint = useRuntimeConfig().public.pageviewEndpoint as string
  const router = useRouter()

  router.afterEach((to) => {
    void $fetch(endpoint, {
      method: 'POST',
      body: { path: to.fullPath },
    }).catch(() => {
      // ignore — analytics is best-effort
    })
  })
})
