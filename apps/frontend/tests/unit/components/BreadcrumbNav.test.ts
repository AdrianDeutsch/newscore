import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import BreadcrumbNav from '~/components/BreadcrumbNav.vue'

const global = {
  stubs: {
    NuxtLink: { template: '<a><slot /></a>' },
  },
}

describe('BreadcrumbNav', () => {
  it('renders a link for items with a target and plain text for the current item', () => {
    const wrapper = mount(BreadcrumbNav, {
      props: {
        items: [
          { label: 'Start', to: '/' },
          { label: 'Politik', to: '/?category=politik' },
          { label: 'Aktueller Artikel' },
        ],
      },
      global,
    })

    const items = wrapper.findAll('.breadcrumb__item')
    expect(items).toHaveLength(3)
    expect(items[0].find('a').exists()).toBe(true)
    expect(items[2].find('a').exists()).toBe(false)
    expect(items[2].find('[aria-current="page"]').text()).toBe('Aktueller Artikel')
  })
})
