import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import CategoryNav from '~/components/CategoryNav.vue'

const global = {
  stubs: {
    NuxtLink: { template: '<a><slot /></a>' },
  },
}

describe('CategoryNav', () => {
  it('renders an "Alle" link plus one link per category', () => {
    const wrapper = mount(CategoryNav, { global })

    expect(wrapper.findAll('a')).toHaveLength(5)
    expect(wrapper.text()).toContain('Alle')
    expect(wrapper.text()).toContain('Politik')
    expect(wrapper.text()).toContain('Wirtschaft')
    expect(wrapper.text()).toContain('Kultur')
    expect(wrapper.text()).toContain('Technik')
  })
})
