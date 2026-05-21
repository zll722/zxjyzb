<script setup lang="ts">
withDefaults(defineProps<{
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger' | 'outline' | 'brand-outline'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  disabled?: boolean
  type?: 'button' | 'submit' | 'reset'
}>(), {
  variant: 'primary',
  size: 'md',
  type: 'button',
})
</script>

<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :class="[
      'inline-flex items-center justify-center gap-2 font-semibold rounded-md transition-all duration-200',
      'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 focus-visible:ring-offset-2',
      'disabled:cursor-not-allowed disabled:opacity-50',
      // min click target 40px enforced via min-h
      variant === 'primary'      && 'bg-primary-600 text-white shadow-sm hover:bg-primary-500 active:bg-primary-700',
      variant === 'secondary'    && 'bg-neutral-100 text-neutral-700 hover:bg-neutral-200 active:bg-neutral-300',
      variant === 'ghost'        && 'text-neutral-600 hover:bg-neutral-100 hover:text-neutral-900',
      variant === 'danger'       && 'bg-danger text-white shadow-sm hover:bg-red-500 active:bg-red-700',
      variant === 'outline'      && 'border border-border text-neutral-700 hover:bg-neutral-100',
      variant === 'brand-outline'&& 'border border-primary-300 text-primary-600 hover:bg-primary-50',
      size === 'sm' && 'min-h-[32px] px-3 text-xs',
      size === 'md' && 'min-h-[40px] px-4 text-sm',
      size === 'lg' && 'min-h-[44px] px-6 text-base',
    ]"
  >
    <span
      v-if="loading"
      class="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent"
    />
    <slot />
  </button>
</template>
