<script setup lang="ts">
interface Crumb {
  label: string
  to?: string
}

defineProps<{ items: Crumb[] }>()
</script>

<template>
  <nav class="breadcrumb" aria-label="Brotkrumen-Navigation">
    <ol class="breadcrumb__list">
      <li v-for="(item, index) in items" :key="index" class="breadcrumb__item">
        <NuxtLink v-if="item.to" :to="item.to">{{ item.label }}</NuxtLink>
        <span v-else aria-current="page">{{ item.label }}</span>
      </li>
    </ol>
  </nav>
</template>
