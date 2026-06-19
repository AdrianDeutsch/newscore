<template>
  <div class="app">
    <header class="app__header">
      <div class="app__bar">
        <NuxtLink to="/" class="app__brand">News<span>Core</span></NuxtLink>
        <SearchBar class="app__search" />
      </div>
      <CategoryNav />
    </header>

    <main class="app__main">
      <NuxtPage />
    </main>

    <footer class="app__footer">
      <p>NewsCore · Demo-Portal · GraphQL BFF + Nuxt 3 SSR · urql</p>
    </footer>
  </div>
</template>
