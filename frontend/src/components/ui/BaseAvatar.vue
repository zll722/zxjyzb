<script setup lang="ts">
withDefaults(defineProps<{
  src?: string
  name?: string
  size?: 'xs' | 'sm' | 'md' | 'lg'
}>(), {
  size: 'md',
})

function initial(name?: string) {
  return name ? name.slice(0, 1).toUpperCase() : '?'
}
</script>

<template>
  <span
    :class="[
      'inline-flex flex-shrink-0 items-center justify-center overflow-hidden rounded-full font-semibold ring-2 ring-white select-none',
      size === 'xs' && 'h-6 w-6 text-[10px]',
      size === 'sm' && 'h-8 w-8 text-xs',
      size === 'md' && 'h-10 w-10 text-sm',
      size === 'lg' && 'h-12 w-12 text-base',
      !src && 'bg-primary-100 text-primary-700',
    ]"
  >
    <img
      v-if="src"
      :src="src"
      :alt="name"
      class="h-full w-full object-cover"
      loading="lazy"
    />
    <template v-else>{{ initial(name) }}</template>
  </span>
</template>
