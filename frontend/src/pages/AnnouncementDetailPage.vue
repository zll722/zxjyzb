<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { CalendarDays, ChevronLeft, Megaphone, UserRound } from 'lucide-vue-next'
import { announcementApi, type Announcement } from '@/lib/api'
import AppLayout from '@/components/layout/AppLayout.vue'

const route = useRoute()
const announcement = ref<Announcement | null>(null)
const error = ref('')
const announcementId = computed(() => Number(route.params.id))

const typeLabels: Record<string, string> = { SYSTEM: '系统公告', COURSE: '课程通知', LIVE: '直播通知', EXAM: '考试提醒' }
const typeTone: Record<string, string> = {
  SYSTEM: 'bg-neutral-100 text-neutral-600',
  COURSE: 'bg-primary-50 text-primary-700',
  LIVE: 'bg-success-bg text-success',
  EXAM: 'bg-warning-bg text-warning',
}

async function load() {
  error.value = ''
  try { announcement.value = await announcementApi.detail(announcementId.value) }
  catch (err) { error.value = err instanceof Error ? err.message : '公告加载失败' }
}

function formatTime(value?: string) { return value ? new Date(value).toLocaleString('zh-CN') : '-' }

onMounted(load)
</script>

<template>
  <AppLayout>
    <div class="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8">
      <RouterLink to="/announcements" class="mb-6 inline-flex items-center gap-1 text-sm text-neutral-500 hover:text-primary-600">
        <ChevronLeft class="h-4 w-4" />返回公告中心
      </RouterLink>

      <div v-if="error" class="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

      <article v-if="announcement" class="rounded-xl border border-border bg-white shadow-sm">
        <!-- Header -->
        <div class="border-b border-border p-6">
          <div class="flex items-start justify-between gap-4">
            <div>
              <span class="inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium" :class="typeTone[announcement.type || ''] || 'bg-neutral-100 text-neutral-600'">
                {{ typeLabels[announcement.type || ''] || '公告' }}
              </span>
              <h1 class="mt-3 text-xl font-bold text-neutral-900">{{ announcement.title }}</h1>
              <div class="mt-3 flex flex-wrap gap-4 text-xs text-neutral-400">
                <span class="inline-flex items-center gap-1">
                  <UserRound class="h-3.5 w-3.5" />{{ announcement.creatorName || '平台管理员' }}
                </span>
                <span class="inline-flex items-center gap-1">
                  <CalendarDays class="h-3.5 w-3.5" />{{ formatTime(announcement.publishTime || announcement.createdAt) }}
                </span>
              </div>
            </div>
            <div class="grid h-12 w-12 flex-shrink-0 place-items-center rounded-xl bg-primary-50 text-primary-600">
              <Megaphone class="h-5 w-5" />
            </div>
          </div>
        </div>

        <!-- Content -->
        <div class="p-6">
          <div class="whitespace-pre-wrap text-sm leading-7 text-neutral-700">{{ announcement.content }}</div>
        </div>
      </article>
    </div>
  </AppLayout>
</template>
