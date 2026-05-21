<script setup lang="ts">
withDefaults(defineProps<{
  tone?: 'default' | 'success' | 'warning' | 'danger' | 'info' | 'brand'
  dot?: boolean
}>(), {
  tone: 'default',
  dot: false,
})
</script>

<template>
  <span
    :class="[
      'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium',
      tone === 'default' && 'bg-neutral-100 text-neutral-600',
      tone === 'success' && 'bg-success-bg text-success',
      tone === 'warning' && 'bg-warning-bg text-warning',
      tone === 'danger'  && 'bg-danger-bg  text-danger',
      tone === 'info'    && 'bg-info-bg    text-info',
      tone === 'brand'   && 'bg-primary-50 text-primary-600',
    ]"
  >
    <span
      v-if="dot"
      class="h-1.5 w-1.5 rounded-full bg-current animate-live-pulse"
    />
    <slot />
  </span>
</template>
