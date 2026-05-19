<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { CalendarDays, Megaphone, UserRound } from 'lucide-vue-next'
import { announcementApi, type Announcement } from '@/lib/api'

const route = useRoute()
const announcement = ref<Announcement | null>(null)
const error = ref('')
const announcementId = computed(() => Number(route.params.id))

const typeLabels: Record<string, string> = { SYSTEM: '系统公告', COURSE: '课程通知', LIVE: '直播通知', EXAM: '考试提醒' }

async function load() {
  error.value = ''
  try {
    announcement.value = await announcementApi.detail(announcementId.value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '公告加载失败'
  }
}

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

onMounted(load)
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-5xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/announcements" class="text-sm text-cyan-200">返回公告中心</RouterLink>
        <RouterLink to="/dashboard" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">工作台</RouterLink>
      </nav>

      <p v-if="error" class="rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>

      <article v-if="announcement" class="overflow-hidden rounded-[2rem] border border-white/10 bg-white/8 backdrop-blur">
        <section class="relative p-8">
          <div class="absolute inset-0 -z-0 bg-[radial-gradient(circle_at_18%_20%,rgba(34,211,238,0.16),transparent_30%),radial-gradient(circle_at_88%_18%,rgba(20,184,166,0.12),transparent_26%)]" />
          <div class="relative flex flex-wrap items-start justify-between gap-6">
            <div>
              <span class="inline-flex rounded-full bg-cyan-300/15 px-3 py-1 text-xs font-bold text-cyan-100">{{ typeLabels[announcement.type || ''] || announcement.type || '公告' }}</span>
              <h1 class="mt-5 text-4xl font-black leading-tight">{{ announcement.title }}</h1>
              <div class="mt-5 flex flex-wrap gap-4 text-sm text-slate-300">
                <span class="inline-flex items-center gap-2"><UserRound class="h-4 w-4 text-cyan-200" />{{ announcement.creatorName || '平台管理员' }}</span>
                <span class="inline-flex items-center gap-2"><CalendarDays class="h-4 w-4 text-cyan-200" />{{ formatTime(announcement.publishTime || announcement.createdAt) }}</span>
              </div>
            </div>
            <div class="rounded-[2rem] border border-cyan-300/30 bg-cyan-300/10 p-5 text-cyan-100">
              <Megaphone class="h-10 w-10" />
            </div>
          </div>
        </section>

        <section class="border-t border-white/10 p-8">
          <div class="whitespace-pre-wrap text-base leading-8 text-slate-200">{{ announcement.content }}</div>
        </section>
      </article>
    </div>
  </main>
</template>
