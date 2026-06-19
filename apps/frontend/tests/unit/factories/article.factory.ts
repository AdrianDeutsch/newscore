import type { Article } from '~/types/article'

let sequence = 0

/**
 * Test data factory for {@link Article}. Produces fully populated articles with unique ids and
 * supports partial overrides per test.
 */
export const ArticleFactory = {
  build(overrides: Partial<Article> = {}): Article {
    sequence += 1
    return {
      id: String(sequence),
      title: 'Test-Artikel',
      teaser: 'Ein kurzer Teaser zum Test-Artikel.',
      body: 'Der vollständige Artikeltext für Testzwecke.',
      author: { id: 'a1', name: 'Lena Hoffmann', email: null, bio: null },
      category: { id: 'c1', name: 'Politik', slug: 'politik' },
      publishedAt: '2026-06-18T07:15:00.000Z',
      tags: ['klima', 'politik'],
      imageUrl: 'https://example.test/image.jpg',
      ...overrides,
    }
  },
}
