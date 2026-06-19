<script setup lang="ts">
import { computed } from 'vue'
import { createArticleService } from '~/services/article.service'

const route = useRoute()
const { $urql } = useNuxtApp()
const service = createArticleService($urql)

const category = computed(() => (route.query.category as string) || undefined)

// SSR-fetched and re-fetched whenever the category filter changes.
const { data, status, error } = await useAsyncData(
  'index-articles',
  () => service.getArticles({ category: category.value, limit: 20 }),
  { watch: [category] },
)

const heading = computed(() =>
  category.value ? `Ressort: ${category.value}` : 'Aktuelle Meldungen',
)
</script>

<template>
  <section>
    <h1 class="page-title">{{ heading }}</h1>

    <p v-if="error" class="state state--error">Inhalte konnten nicht geladen werden.</p>
    <p v-else-if="status === 'pending'" class="state">Lädt…</p>
    <p v-else-if="!data || data.nodes.length === 0" class="state">Keine Artikel gefunden.</p>

    <div v-else class="article-grid">
      <ArticleCard v-for="article in data.nodes" :key="article.id" :article="article" />
    </div>
  </section>
</template>
