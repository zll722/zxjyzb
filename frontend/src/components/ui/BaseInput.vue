<script setup lang="ts">
withDefaults(defineProps<{
  modelValue?: string | number
  type?: string
  placeholder?: string
  disabled?: boolean
  error?: boolean
  label?: string
  hint?: string
  prefix?: string
  id?: string
}>(), {
  type: 'text',
})

defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <div class="flex flex-col gap-1.5">
    <label
      v-if="label"
      :for="id"
      class="text-sm font-medium text-neutral-700"
    >{{ label }}</label>

    <div class="relative flex items-center">
      <span
        v-if="prefix"
        class="pointer-events-none absolute left-3 select-none text-sm text-neutral-400"
      >{{ prefix }}</span>

      <input
        :id="id"
        :type="type"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :class="[
          'h-10 w-full rounded-md border bg-white text-sm text-neutral-900 outline-none',
          'transition-all duration-200',
          'placeholder:text-neutral-400',
          prefix ? 'pl-7 pr-3' : 'px-3',
          error
            ? 'border-danger focus:border-danger focus:ring-2 focus:ring-danger/20'
            : 'border-border focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15',
          disabled && 'cursor-not-allowed bg-neutral-100 opacity-60',
        ]"
        @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      />
    </div>

    <p v-if="hint" class="text-xs text-neutral-400">{{ hint }}</p>
  </div>
</template>
