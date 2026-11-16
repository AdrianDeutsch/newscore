import { randomUUID } from 'node:crypto'
import { expect, test } from '../support/fixtures'
import { graphql, queryAnalytics, stack } from '../support/stack'

/**
 * The analytics chain end to end: gateway -> Kafka (Avro, Schema Registry) -> analytics-service ->
 * PostgreSQL. Each test puts a unique marker into the event and waits for exactly that row.
 */
test.describe('analytics events', () => {
  test('a search ends up as a row with the query and the number of hits', async () => {
    const marker = `e2e${randomUUID().replaceAll('-', '')}`
    const query = `klima ${marker}`

    const data = await graphql<{ search: { totalCount: number } }>(
      'query ($q: String!) { search(query: $q) { totalCount } }',
      { q: query },
    )
    expect(data.search.totalCount).toBe(2)

    await expect
      .poll(() => queryAnalytics('SELECT result_count, source_topic FROM search_analytics WHERE query = $1', [query]), {
        timeout: 20_000,
      })
      .toEqual([{ result_count: 2, source_topic: 'newscore.search.events' }])
  })

  test('opening a page in the browser ends up as a page-view row', async ({ page }) => {
    const marker = randomUUID()
    const path = `/article/2?e2e=${marker}`

    await page.goto(`${stack.frontend}${path}`)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Inflation sinkt den dritten Monat in Folge')

    await expect
      .poll(() => queryAnalytics('SELECT path FROM page_view_analytics WHERE path = $1', [path]), { timeout: 20_000 })
      .toEqual([{ path }])
  })

  test('a beacon that is not a path of this site is refused and stores nothing', async ({ request }) => {
    const marker = randomUUID()

    const response = await request.post(`${stack.frontend}/events/pageview`, {
      data: { path: `https://evil.example/${marker}` },
    })
    expect(response.status()).toBe(400)

    // Give the pipeline the time a valid event needs, then make sure nothing arrived.
    const valid = `/e2e-control-${marker}`
    expect((await request.post(`${stack.frontend}/events/pageview`, { data: { path: valid } })).status()).toBe(202)
    await expect
      .poll(() => queryAnalytics('SELECT path FROM page_view_analytics WHERE path = $1', [valid]), { timeout: 20_000 })
      .toHaveLength(1)
    expect(await queryAnalytics('SELECT path FROM page_view_analytics WHERE path LIKE $1', [`%evil.example/${marker}`])).toEqual([])
  })
})
