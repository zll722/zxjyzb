<script setup lang="ts">
withDefaults(defineProps<{
  padding?: 'none' | 'sm' | 'md' | 'lg'
  hover?: boolean
  as?: string
}>(), {
  padding: 'md',
  hover: false,
  as: 'div',
})
</script>

<template>
  <component
    :is="as"
    :class="[
      'rounded-lg border border-border bg-white shadow-sm',
      hover && 'transition-all duration-200 hover:shadow-md hover:-translate-y-px cursor-pointer',
      padding === 'none' && 'p-0',
      padding === 'sm'   && 'p-4',
      padding === 'md'   && 'p-5',
      padding === 'lg'   && 'p-6',
    ]"
  >
    <slot />
  </component>
</template>
