<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppNav from './AppNav.vue'
import { useAuth } from '@/composables/useAuth'
import { messageApi } from '@/lib/api'

const auth = useAuth()
const unread = ref(0)

onMounted(async () => {
  if (auth.state.user) {
    try { unread.value = await messageApi.unreadCount() } catch { /* ignore */ }
  }
})
</script>

<template>
  <div class="min-h-screen bg-neutral-50">
    <AppNav :unread="unread" />
    <main>
      <slot />
    </main>
  </div>
</template>
