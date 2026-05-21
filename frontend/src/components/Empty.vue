<script setup lang="ts">
import { BookOpen } from 'lucide-vue-next'

withDefaults(defineProps<{
  title?: string
  description?: string
  icon?: object
}>(), {
  title: '暂无数据',
  description: '这里还没有内容',
})
</script>

<template>
  <div class="flex min-h-80 flex-col items-center justify-center gap-4 py-16 text-center">
    <!-- Illustration container -->
    <div class="grid h-20 w-20 place-items-center rounded-2xl bg-primary-50 text-primary-600">
      <component :is="icon || BookOpen" class="h-10 w-10" stroke-width="1.5" />
    </div>
    <!-- Primary text -->
    <div class="space-y-1.5">
      <p class="text-base font-semibold text-neutral-900">{{ title }}</p>
      <p class="text-sm text-neutral-500">{{ description }}</p>
    </div>
    <!-- Optional action slot -->
    <slot />
  </div>
</template>
