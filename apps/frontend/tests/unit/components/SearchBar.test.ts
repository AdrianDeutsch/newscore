import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SearchBar from '~/components/SearchBar.vue'

const push = vi.fn()

beforeEach(() => {
  push.mockClear()
  // useRouter is a Nuxt auto-import; expose it as a global for the isolated component test.
  vi.stubGlobal('useRouter', () => ({ push }))
})

describe('SearchBar', () => {
  it('navigates to the search page with the trimmed query on submit', async () => {
    const wrapper = mount(SearchBar)

    await wrapper.find('input').setValue('  klima  ')
    await wrapper.find('form').trigger('submit.prevent')

    expect(push).toHaveBeenCalledWith({ path: '/search', query: { q: 'klima' } })
  })

  it('does not navigate when the query is blank', async () => {
    const wrapper = mount(SearchBar)

    await wrapper.find('input').setValue('   ')
    await wrapper.find('form').trigger('submit.prevent')

    expect(push).not.toHaveBeenCalled()
  })
})
