<script setup lang="ts">
import { ref } from 'vue'

const router = useRouter()
const term = ref('')

function submit() {
  const query = term.value.trim()
  if (query) {
    router.push({ path: '/search', query: { q: query } })
  }
}
</script>

<template>
  <form class="search-bar" role="search" @submit.prevent="submit">
    <input
      v-model="term"
      type="search"
      name="q"
      class="search-bar__input"
      placeholder="Artikel suchen…"
      aria-label="Artikel suchen"
    >
    <button type="submit" class="search-bar__btn">Suchen</button>
  </form>
</template>
