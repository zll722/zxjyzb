<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
  links: Array<{ to: string; label: string; icon?: object; exact?: boolean }>
  title?: string
}>()
</script>

<template>
  <aside class="flex flex-col gap-0.5 py-1">
    <p
      v-if="title"
      class="mb-2 px-3 text-xs font-semibold uppercase tracking-widest text-neutral-400"
    >{{ title }}</p>

    <RouterLink
      v-for="link in links"
      :key="link.to"
      :to="link.to"
      class="group relative flex min-h-[40px] items-center gap-2.5 rounded-md px-3 py-2.5 text-sm font-medium text-neutral-600 transition-colors duration-200 hover:bg-neutral-100 hover:text-neutral-900"
      :active-class="link.exact ? '' : 'bg-primary-50 text-primary-700 font-semibold'"
      :exact-active-class="'bg-primary-50 text-primary-700 font-semibold'"
    >
      <!-- 3px active indicator bar -->
      <span
        class="absolute left-0 top-1/2 -translate-y-1/2 h-5 w-0.5 rounded-full bg-primary-600 opacity-0 transition-opacity duration-200 group-[.router-link-active]:opacity-100"
      />
      <component
        :is="link.icon"
        v-if="link.icon"
        class="h-4 w-4 flex-shrink-0 text-current"
        stroke-width="1.75"
      />
      {{ link.label }}
    </RouterLink>
  </aside>
</template>
