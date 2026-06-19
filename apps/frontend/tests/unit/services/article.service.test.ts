import type { Client } from '@urql/core'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createArticleService } from '~/services/article.service'
import { ArticleFactory } from '../factories/article.factory'

describe('ArticleService', () => {
  const toPromise = vi.fn()
  const query = vi.fn(() => ({ toPromise }))
  const client = { query } as unknown as Client
  const service = createArticleService(client)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getArticle() returns the article on success', async () => {
    const article = ArticleFactory.build({ id: '1' })
    toPromise.mockResolvedValue({ data: { article } })

    await expect(service.getArticle('1')).resolves.toEqual(article)
    expect(query).toHaveBeenCalledWith(expect.any(String), { id: '1' })
  })

  it('getArticle() returns null when the article is missing', async () => {
    toPromise.mockResolvedValue({ data: { article: null } })

    await expect(service.getArticle('x')).resolves.toBeNull()
  })

  it('getArticle() throws when the query errors', async () => {
    toPromise.mockResolvedValue({ error: new Error('network down') })

    await expect(service.getArticle('1')).rejects.toThrow('network down')
  })

  it('getArticles() maps the connection and applies defaults', async () => {
    const connection = { nodes: [ArticleFactory.build()], totalCount: 1, hasNextPage: false }
    toPromise.mockResolvedValue({ data: { articles: connection } })

    await expect(service.getArticles()).resolves.toEqual(connection)
    expect(query).toHaveBeenCalledWith(expect.any(String), { category: null, limit: 20, offset: 0 })
  })

  it('getArticles() falls back to an empty connection when data is absent', async () => {
    toPromise.mockResolvedValue({ data: undefined })

    await expect(service.getArticles({ category: 'politik' })).resolves.toEqual({
      nodes: [],
      totalCount: 0,
      hasNextPage: false,
    })
  })

  it('search() returns the result and passes the limit', async () => {
    const result = { query: 'klima', totalCount: 1, results: [ArticleFactory.build()] }
    toPromise.mockResolvedValue({ data: { search: result } })

    await expect(service.search('klima', 5)).resolves.toEqual(result)
    expect(query).toHaveBeenCalledWith(expect.any(String), { query: 'klima', limit: 5 })
  })

  it('search() falls back to an empty result when data is absent', async () => {
    toPromise.mockResolvedValue({ data: undefined })

    await expect(service.search('leer')).resolves.toEqual({ query: 'leer', totalCount: 0, results: [] })
  })
})
