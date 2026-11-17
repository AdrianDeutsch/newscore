<script setup lang="ts">
import { computed } from 'vue'
import { useSearch } from '~/composables/useSearch'

const route = useRoute()
const query = computed(() => (route.query.q as string) || '')

const { data: result, status, error } = await useSearch(() => query.value)

// A failed request on the server becomes a 502 page, not a 200 page with an error text: the edge
// cache would store that and serve it in place of the last good copy (DEFECT-011). A failed
// re-fetch in the browser (filter change) shows the error text below instead.
if (import.meta.server && error.value) {
  throw createError({ statusCode: 502, statusMessage: 'Inhalte konnten nicht geladen werden', fatal: true })
}

useHead(() => ({ title: query.value ? `Suche: ${query.value} · NewsCore` : 'Suche · NewsCore' }))
</script>

<template>
  <section>
    <h1 class="page-title">Suche</h1>

    <p v-if="!query" class="state">Bitte einen Suchbegriff eingeben.</p>
    <p v-else class="search-summary">
      Ergebnisse für „<strong>{{ query }}</strong>"<span v-if="result && !error"> · {{ result.totalCount }} Treffer</span>
    </p>

    <p v-if="query && error" class="state state--error">Die Suche ist gerade nicht verfügbar.</p>
    <p v-else-if="query && status === 'pending'" class="state">Sucht…</p>
    <div v-else-if="result && result.results.length" class="article-grid">
      <ArticleCard v-for="article in result.results" :key="article.id" :article="article" />
    </div>
    <p v-else-if="query" class="state">Keine Treffer.</p>
  </section>
</template>
