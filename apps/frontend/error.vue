<script setup lang="ts">
import { computed } from 'vue'

// Rendered by Nuxt for every error thrown with createError (and for unknown routes); the HTTP
// status of the response is the error's statusCode.
const props = defineProps<{ error: { statusCode?: number, statusMessage?: string } }>()

const notFound = computed(() => props.error.statusCode === 404)
const heading = computed(() => (notFound.value ? 'Seite nicht gefunden' : 'Inhalte konnten nicht geladen werden'))

useHead(() => ({ title: `${heading.value} · NewsCore` }))

function backToStart() {
  clearError({ redirect: '/' })
}
</script>

<template>
  <div class="app">
    <header class="app__header">
      <div class="app__bar">
        <a href="/" class="app__brand" @click.prevent="backToStart">News<span>Core</span></a>
      </div>
    </header>

    <main class="app__main">
      <section class="state state--error" data-test="error-page">
        <h1 class="page-title">{{ heading }}</h1>
        <p v-if="notFound">Unter dieser Adresse gibt es keinen Inhalt (mehr).</p>
        <p v-else>Bitte später erneut versuchen.</p>
        <p><a href="/" @click.prevent="backToStart">Zur Startseite</a></p>
      </section>
    </main>
  </div>
</template>
