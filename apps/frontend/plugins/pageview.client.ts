/**
 * Sends a fire-and-forget page-view beacon on every client-side navigation. It goes to this site's
 * own /events/pageview, which server/routes/events/pageview.post.ts forwards to the gateway.
 * Produces the `user.pageview` event (gateway → Kafka → analytics-service → PostgreSQL).
 * Analytics must never block or break navigation, so failures are ignored.
 */
export default defineNuxtPlugin(() => {
  const router = useRouter()

  router.afterEach((to) => {
    void $fetch('/events/pageview', {
      method: 'POST',
      body: { path: to.fullPath },
    }).catch(() => {
      // ignore — analytics is best-effort
    })
  })
})
