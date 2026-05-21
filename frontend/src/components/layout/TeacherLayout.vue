<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterView } from 'vue-router'
import { BarChart3, BookOpen, Clapperboard, FileQuestion, NotebookPen, Radio } from 'lucide-vue-next'
import AppNav from './AppNav.vue'
import AppSidebar from './AppSidebar.vue'
import { useAuth } from '@/composables/useAuth'
import { messageApi } from '@/lib/api'

const auth = useAuth()
const unread = ref(0)

onMounted(async () => {
  if (auth.state.user) {
    try { unread.value = await messageApi.unreadCount() } catch { /* ignore */ }
  }
})

const sidebarLinks = [
  { to: '/teacher/courses', label: '课程管理', icon: BookOpen },
  { to: '/teacher/live', label: '直播管理', icon: Radio },
  { to: '/teacher/replays', label: '回放管理', icon: Clapperboard },
  { to: '/teacher/homeworks', label: '作业管理', icon: NotebookPen },
  { to: '/teacher/exams', label: '考试管理', icon: FileQuestion },
  { to: '/teacher/stats', label: '教学统计', icon: BarChart3 },
]
</script>

<template>
  <div class="min-h-screen bg-bg">
    <AppNav :unread="unread" />
    <div class="flex">
      <!-- Sidebar: pure white, contrasts with neutral-50 content area -->
      <aside class="hidden w-56 flex-shrink-0 border-r border-border bg-white lg:block">
        <div class="sticky top-16 px-2 py-4">
          <AppSidebar :links="sidebarLinks" title="教师工作台" />
        </div>
      </aside>
      <main class="min-w-0 flex-1 bg-neutral-50 px-4 py-8 sm:px-6 lg:px-8">
        <RouterView />
      </main>
    </div>
  </div>
</template>
