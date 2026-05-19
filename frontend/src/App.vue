<script setup lang="ts">
import { RouterView } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

useAuth().init()
</script>

<template>
  <RouterView />
</template>
