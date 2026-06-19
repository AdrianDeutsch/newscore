<script setup lang="ts">
interface NavCategory {
  name: string
  slug: string
}

// Static ressorts mirroring the categories seeded in the api-gateway.
const categories: NavCategory[] = [
  { name: 'Politik', slug: 'politik' },
  { name: 'Wirtschaft', slug: 'wirtschaft' },
  { name: 'Kultur', slug: 'kultur' },
  { name: 'Technik', slug: 'technik' },
]
</script>

<template>
  <nav class="category-nav" aria-label="Ressorts">
    <NuxtLink to="/" class="category-nav__item">Alle</NuxtLink>
    <NuxtLink
      v-for="category in categories"
      :key="category.slug"
      :to="{ path: '/', query: { category: category.slug } }"
      class="category-nav__item"
    >
      {{ category.name }}
    </NuxtLink>
  </nav>
</template>
