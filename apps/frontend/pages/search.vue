<script setup lang="ts">
import { computed } from 'vue'
import { useSearch } from '~/composables/useSearch'

const route = useRoute()
const query = computed(() => (route.query.q as string) || '')

const { data: result, status } = await useSearch(() => query.value)

useHead(() => ({ title: query.value ? `Suche: ${query.value} · NewsCore` : 'Suche · NewsCore' }))
</script>

<template>
  <section>
    <h1 class="page-title">Suche</h1>

    <p v-if="!query" class="state">Bitte einen Suchbegriff eingeben.</p>
    <p v-else class="search-summary">
      Ergebnisse für „<strong>{{ query }}</strong>"<span v-if="result"> · {{ result.totalCount }} Treffer</span>
    </p>

    <p v-if="query && status === 'pending'" class="state">Sucht…</p>
    <div v-else-if="result && result.results.length" class="article-grid">
      <ArticleCard v-for="article in result.results" :key="article.id" :article="article" />
    </div>
    <p v-else-if="query" class="state">Keine Treffer.</p>
  </section>
</template>
