<script setup lang="ts">
import { computed } from 'vue'
import { useArticle } from '~/composables/useArticle'
import { formatDateTime } from '~/utils/formatDate'

const route = useRoute()
const id = route.params.id as string

const { data: article, error } = await useArticle(id)

// Answer with a real HTTP status instead of rendering "not found" into a 200 page: crawlers and
// the edge cache would otherwise treat the missing article as a valid page. A gateway failure is
// not "not found" either — it becomes a 502, which the edge cache does not store.
if (error.value) {
  throw createError({ statusCode: 502, statusMessage: 'Inhalte konnten nicht geladen werden', fatal: true })
}
if (!article.value) {
  throw createError({ statusCode: 404, statusMessage: 'Artikel nicht gefunden', fatal: true })
}

const formattedDate = computed(() => (article.value ? formatDateTime(article.value.publishedAt) : ''))

useHead(() => ({
  title: article.value ? `${article.value.title} · NewsCore` : 'NewsCore',
}))
</script>

<template>
  <article v-if="article" class="article-detail">
    <BreadcrumbNav
      :items="[
        { label: 'Start', to: '/' },
        { label: article.category.name, to: `/?category=${article.category.slug}` },
        { label: article.title },
      ]"
    />

    <span class="article-detail__category">{{ article.category.name }}</span>
    <h1 class="article-detail__title">{{ article.title }}</h1>
    <p v-if="article.teaser" class="article-detail__teaser">{{ article.teaser }}</p>

    <div class="article-detail__meta">
      <span>{{ article.author.name }}</span>
      <time :datetime="article.publishedAt">{{ formattedDate }}</time>
    </div>

    <img
      v-if="article.imageUrl"
      :src="article.imageUrl"
      :alt="article.title"
      class="article-detail__image"
      width="800"
      height="450"
    >

    <div class="article-detail__body">
      <p>{{ article.body }}</p>
    </div>

    <ul v-if="article.tags.length" class="article-detail__tags">
      <li v-for="tag in article.tags" :key="tag">#{{ tag }}</li>
    </ul>
  </article>
</template>
