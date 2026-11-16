import { expect, test } from '../support/fixtures'
import { emitArticleEvent, graphql, stack } from '../support/stack'

const hits = async (term: string): Promise<number> => {
  const data = await graphql<{ search: { totalCount: number } }>(
    'query ($q: String!) { search(query: $q) { totalCount } }',
    { q: term },
  )
  return data.search.totalCount
}

/**
 * The search index follows article events: gateway -> Kafka -> gateway consumer -> ElasticSearch.
 * The demo articles themselves never change, so the tests change the index instead and check that
 * an event brings it back in line.
 */
test.describe('search index and article events', () => {
  test.afterAll(async () => {
    // Leave the index complete for whoever uses the stack next.
    await emitArticleEvent('6', 'publish')
    await emitArticleEvent('8', 'publish')
    await expect.poll(() => hits('klima'), { timeout: 20_000 }).toBe(2)
    await expect.poll(() => hits('Objektspeicher'), { timeout: 20_000 }).toBe(1)
  })

  test('an update event re-indexes an article that is missing from the index', async () => {
    await expect.poll(() => hits('klima')).toBe(2)

    // Take article 6 out of the index behind the gateway's back.
    const removed = await fetch(`${stack.elasticsearch}/articles/_doc/6?refresh=true`, { method: 'DELETE' })
    expect(removed.status).toBe(200)
    expect(await hits('klima')).toBe(1)

    await emitArticleEvent('6', 'update')

    await expect.poll(() => hits('klima'), { timeout: 20_000 }).toBe(2)
  })

  test('a delete event removes the article from the index, a publish event adds it again', async () => {
    await expect.poll(() => hits('Objektspeicher')).toBe(1)

    await emitArticleEvent('8', 'delete')
    await expect.poll(() => hits('Objektspeicher'), { timeout: 20_000 }).toBe(0)

    await emitArticleEvent('8', 'publish')
    await expect.poll(() => hits('Objektspeicher'), { timeout: 20_000 }).toBe(1)
  })
})
