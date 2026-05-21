<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterView } from 'vue-router'
import { BarChart3, BookCheck, FolderTree, Images, Megaphone, MessageSquareWarning, Radio, Users } from 'lucide-vue-next'
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
  { to: '/admin', label: '管理概览', icon: BarChart3, exact: true },
  { to: '/admin/users', label: '用户管理', icon: Users },
  { to: '/admin/courses', label: '课程审核', icon: BookCheck },
  { to: '/admin/categories', label: '分类管理', icon: FolderTree },
  { to: '/admin/lives', label: '直播管理', icon: Radio },
  { to: '/admin/barrages', label: '弹幕管理', icon: MessageSquareWarning },
  { to: '/admin/banners', label: '轮播管理', icon: Images },
  { to: '/admin/announcements', label: '公告管理', icon: Megaphone },
  { to: '/admin/stats', label: '平台统计', icon: BarChart3 },
]
</script>

<template>
  <div class="min-h-screen bg-bg">
    <AppNav :unread="unread" />
    <div class="flex">
      <!-- Sidebar: pure white, contrasts with neutral-50 content area -->
      <aside class="hidden w-56 flex-shrink-0 border-r border-border bg-white lg:block">
        <div class="sticky top-16 px-2 py-4">
          <AppSidebar :links="sidebarLinks" title="管理后台" />
        </div>
      </aside>
      <main class="min-w-0 flex-1 bg-neutral-50 px-4 py-8 sm:px-6 lg:px-8">
        <RouterView />
      </main>
    </div>
  </div>
</template>
