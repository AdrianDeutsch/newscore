import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import globals from 'globals'
import tseslint from 'typescript-eslint'

export default tseslint.config(
  {
    ignores: [
      '.nuxt/**',
      '.output/**',
      'dist/**',
      'node_modules/**',
      'coverage/**',
      'test-results/**',
    ],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  // Essential Vue rules only — bug-preventing checks without formatting ceremony.
  ...pluginVue.configs['flat/essential'],
  {
    files: ['**/*.vue'],
    languageOptions: {
      parserOptions: {
        parser: tseslint.parser,
      },
    },
  },
  {
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.node,
        // Nuxt auto-imported helpers used across the project.
        defineNuxtConfig: 'readonly',
        defineNuxtPlugin: 'readonly',
        useRuntimeConfig: 'readonly',
        useNuxtApp: 'readonly',
        useAsyncData: 'readonly',
        useRoute: 'readonly',
        useRouter: 'readonly',
        useHead: 'readonly',
        navigateTo: 'readonly',
      },
    },
    rules: {
      'vue/multi-word-component-names': 'off',
    },
  },
)
