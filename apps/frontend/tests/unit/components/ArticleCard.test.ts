import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ArticleCard from '~/components/ArticleCard.vue'
import { ArticleFactory } from '../factories/article.factory'

// NuxtLink is not registered outside the Nuxt runtime; stub it with a plain anchor.
const global = {
  stubs: {
    NuxtLink: { template: '<a><slot /></a>' },
  },
}

describe('ArticleCard', () => {
  it('renders the title and teaser', () => {
    const article = ArticleFactory.build({ title: 'Test-Artikel', teaser: 'Mein Teaser' })

    const wrapper = mount(ArticleCard, { props: { article }, global })

    expect(wrapper.find('[data-test="article-title"]').text()).toBe('Test-Artikel')
    expect(wrapper.find('[data-test="article-teaser"]').text()).toContain('Mein Teaser')
  })

  it('renders the article image when an imageUrl is present', () => {
    const article = ArticleFactory.build({ imageUrl: 'https://example.test/photo.jpg' })

    const wrapper = mount(ArticleCard, { props: { article }, global })

    expect(wrapper.find('img').attributes('src')).toBe('https://example.test/photo.jpg')
    expect(wrapper.find('img').attributes('loading')).toBe('lazy')
  })

  it('falls back to the placeholder image when imageUrl is null', () => {
    const article = ArticleFactory.build({ imageUrl: null })

    const wrapper = mount(ArticleCard, { props: { article }, global })

    expect(wrapper.find('img').attributes('src')).toBe('/images/placeholder.svg')
  })

  it('renders category and author metadata', () => {
    const article = ArticleFactory.build()

    const wrapper = mount(ArticleCard, { props: { article }, global })

    expect(wrapper.find('[data-test="article-category"]').text()).toBe('Politik')
    expect(wrapper.find('[data-test="article-author"]').text()).toBe('Lena Hoffmann')
  })

  it('omits the teaser element when no teaser is provided', () => {
    const article = ArticleFactory.build({ teaser: null })

    const wrapper = mount(ArticleCard, { props: { article }, global })

    expect(wrapper.find('[data-test="article-teaser"]').exists()).toBe(false)
  })
})
