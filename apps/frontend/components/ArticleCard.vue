<script setup lang="ts">
import { computed } from 'vue'
import type { Article } from '~/types/article'

const props = defineProps<{ article: Article }>()

const PLACEHOLDER = '/images/placeholder.svg'

const imageSrc = computed(() => props.article.imageUrl || PLACEHOLDER)
const formattedDate = computed(() =>
  new Intl.DateTimeFormat('de-DE', { dateStyle: 'medium' }).format(new Date(props.article.publishedAt)),
)
</script>

<template>
  <article class="article-card">
    <NuxtLink :to="`/article/${article.id}`" class="article-card__media">
      <img
        :src="imageSrc"
        :alt="article.title"
        loading="lazy"
        width="800"
        height="450"
      >
    </NuxtLink>

    <div class="article-card__body">
      <span class="article-card__category" data-test="article-category">{{ article.category.name }}</span>
      <h2 class="article-card__title">
        <NuxtLink :to="`/article/${article.id}`" data-test="article-title">{{ article.title }}</NuxtLink>
      </h2>
      <p v-if="article.teaser" class="article-card__teaser" data-test="article-teaser">
        {{ article.teaser }}
      </p>
      <footer class="article-card__meta">
        <span data-test="article-author">{{ article.author.name }}</span>
        <time :datetime="article.publishedAt">{{ formattedDate }}</time>
      </footer>
    </div>
  </article>
</template>
