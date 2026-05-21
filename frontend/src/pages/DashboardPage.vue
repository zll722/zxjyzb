<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BarChart3, Bell, BookMarked, BookOpen, ChevronRight, FileQuestion, Megaphone, NotebookPen, Radio, UserRound } from 'lucide-vue-next'
import { useAuth } from '@/composables/useAuth'
import { messageApi } from '@/lib/api'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseAvatar from '@/components/ui/BaseAvatar.vue'

const auth        = useAuth()
const router      = useRouter()
const unreadCount = ref(0)

onMounted(async () => {
  const role = auth.state.user?.role
  if (role === 'TEACHER') { router.replace('/teacher/courses'); return }
  if (role === 'ADMIN')   { router.replace('/admin'); return }
  if (auth.state.user) {
    try { unreadCount.value = await messageApi.unreadCount() } catch { /* ignore */ }
  }
})

const resources = [
  { to: '/courses',       icon: BookOpen,  title: '课程中心',  desc: '浏览并学习平台上发布的课程' },
  { to: '/live',          icon: Radio,     title: '直播中心',  desc: '进入直播课堂，参与聊天与互动' },
  { to: '/announcements', icon: Megaphone, title: '平台公告',  desc: '查看最新平台公告与通知' },
]

const myLearning = [
  { to: '/dashboard/my-learning',   icon: BookMarked,    title: '学习记录', desc: '已学课程' },
  { to: '/dashboard/my-favorites',  icon: BookOpen,      title: '我的收藏', desc: '收藏的课程' },
  { to: '/homeworks',               icon: NotebookPen,   title: '我的作业', desc: '查看作业进度' },
  { to: '/exams',                   icon: FileQuestion,  title: '我的考试', desc: '参加在线考试' },
]
</script>

<template>
  <AppLayout>
    <div class="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">

      <!-- Profile header card -->
      <div class="mb-8 flex flex-wrap items-center justify-between gap-4 rounded-xl border border-border bg-white p-5 shadow-sm">
        <div class="flex items-center gap-4">
          <RouterLink to="/profile">
            <BaseAvatar
              :src="auth.state.user?.avatar"
              :name="auth.state.user?.username"
              size="lg"
            />
          </RouterLink>
          <div>
            <h1 class="text-xl font-semibold text-neutral-900">
              {{ auth.state.user?.username || 'EduLive 用户' }}
            </h1>
            <p class="mt-0.5 text-sm text-neutral-500">
              学生 · {{ auth.state.user?.email }}
            </p>
          </div>
        </div>
        <div class="flex flex-wrap gap-2">
          <RouterLink
            to="/profile"
            class="inline-flex min-h-[40px] items-center gap-1.5 rounded-md border border-border px-4 text-sm font-medium text-neutral-700 transition-all duration-200 hover:bg-neutral-100"
          >
            <UserRound class="h-4 w-4" />个人中心
          </RouterLink>
          <RouterLink
            to="/messages"
            class="relative inline-flex min-h-[40px] items-center gap-1.5 rounded-md border border-border px-4 text-sm font-medium text-neutral-700 transition-all duration-200 hover:bg-neutral-100"
          >
            <Bell class="h-4 w-4" />消息
            <span
              v-if="unreadCount > 0"
              class="absolute -right-1 -top-1 flex h-4 w-4 items-center justify-center rounded-full bg-danger text-[10px] font-bold text-white"
            >{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </RouterLink>
        </div>
      </div>

      <!-- Learning resources — all icons unified primary -->
      <section class="mb-6">
        <h2 class="mb-3 text-xs font-semibold uppercase tracking-widest text-neutral-400">学习资源</h2>
        <div class="grid gap-4 sm:grid-cols-3">
          <RouterLink
            v-for="item in resources"
            :key="item.to"
            :to="item.to"
            class="group rounded-xl border border-border bg-white p-5 shadow-sm transition-all duration-200 hover:shadow-md hover:-translate-y-px"
          >
            <div class="mb-3 grid h-10 w-10 place-items-center rounded-lg bg-primary-50 text-primary-600">
              <component :is="item.icon" class="h-5 w-5" stroke-width="1.75" />
            </div>
            <h3 class="font-semibold text-neutral-900">{{ item.title }}</h3>
            <p class="mt-1 text-sm text-neutral-500">{{ item.desc }}</p>
          </RouterLink>
        </div>
      </section>

      <!-- My learning — icon list style with chevron -->
      <section class="mb-6">
        <h2 class="mb-3 text-xs font-semibold uppercase tracking-widest text-neutral-400">我的学习</h2>
        <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <RouterLink
            v-for="item in myLearning"
            :key="item.to"
            :to="item.to"
            class="group flex items-center gap-3 rounded-xl border border-border bg-white p-4 shadow-sm transition-all duration-200 hover:bg-primary-50 hover:border-primary-200 hover:shadow-md"
          >
            <div class="grid h-10 w-10 flex-shrink-0 place-items-center rounded-lg bg-primary-50 text-primary-600 transition-colors duration-200 group-hover:bg-primary-100">
              <component :is="item.icon" class="h-5 w-5" stroke-width="1.75" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-medium text-neutral-900">{{ item.title }}</p>
              <p class="text-xs text-neutral-500">{{ item.desc }}</p>
            </div>
            <ChevronRight class="h-4 w-4 flex-shrink-0 text-neutral-400 transition-transform duration-200 group-hover:translate-x-0.5" />
          </RouterLink>
        </div>
      </section>

      <!-- Stats shortcut -->
      <section>
        <RouterLink
          to="/dashboard/student-stats"
          class="group flex items-center justify-between rounded-xl border border-border bg-white p-5 shadow-sm transition-all duration-200 hover:shadow-md hover:-translate-y-px"
        >
          <div class="flex items-center gap-3">
            <div class="grid h-10 w-10 place-items-center rounded-lg bg-primary-50 text-primary-600">
              <BarChart3 class="h-5 w-5" stroke-width="1.75" />
            </div>
            <div>
              <p class="font-semibold text-neutral-900">学习统计</p>
              <p class="text-sm text-neutral-500">查看作业提交率、考试平均分等数据</p>
            </div>
          </div>
          <div class="flex items-center gap-1 text-sm font-medium text-primary-600 transition-transform duration-200 group-hover:translate-x-0.5">
            查看详情 <ChevronRight class="h-4 w-4" />
          </div>
        </RouterLink>
      </section>

    </div>
  </AppLayout>
</template>
