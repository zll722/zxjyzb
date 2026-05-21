<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Megaphone, Search } from 'lucide-vue-next'
import { announcementApi, type Announcement, type PageResult } from '@/lib/api'
import AppLayout from '@/components/layout/AppLayout.vue'
import Empty from '@/components/Empty.vue'

const filters = reactive({ keyword: '', type: '' })
const page = ref<PageResult<Announcement>>({ records: [], total: 0, size: 10, current: 1 })
const loading = ref(false)
const error = ref('')

const typeLabels: Record<string, string> = { SYSTEM: '系统公告', COURSE: '课程通知', LIVE: '直播通知', EXAM: '考试提醒' }
const typeTone: Record<string, string> = {
  SYSTEM: 'bg-neutral-100 text-neutral-600',
  COURSE: 'bg-primary-50 text-primary-700',
  LIVE: 'bg-success-bg text-success',
  EXAM: 'bg-warning-bg text-warning',
}

async function load(current = 1) {
  loading.value = true; error.value = ''
  try {
    page.value = await announcementApi.page({ current, size: page.value.size, keyword: filters.keyword, type: filters.type })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '公告加载失败'
  } finally {
    loading.value = false
  }
}

function formatTime(value?: string) { return value ? new Date(value).toLocaleString('zh-CN') : '-' }

onMounted(() => load())
</script>

<template>
  <AppLayout>
    <div class="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">公告中心</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">平台公告与教学通知</h1>
        <p class="mt-1 text-sm text-neutral-500">集中查看系统公告、课程安排、直播与考试提醒。</p>
      </div>

      <!-- Filter bar -->
      <form class="mb-6 flex flex-wrap gap-2" @submit.prevent="load(1)">
        <div class="relative flex-1 min-w-48">
          <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-neutral-400" />
          <input
            v-model="filters.keyword"
            placeholder="搜索公告标题"
            class="h-9 w-full rounded-lg border border-border bg-white pl-9 pr-3 text-sm text-neutral-900 outline-none transition placeholder:text-neutral-400 focus:border-primary-400 focus:ring-2 focus:ring-primary-100"
          />
        </div>
        <select
          v-model="filters.type"
          class="h-9 rounded-lg border border-border bg-white px-3 text-sm text-neutral-700 outline-none transition focus:border-primary-400"
        >
          <option value="">全部类型</option>
          <option value="SYSTEM">系统公告</option>
          <option value="COURSE">课程通知</option>
          <option value="LIVE">直播通知</option>
          <option value="EXAM">考试提醒</option>
        </select>
        <button type="submit" class="h-9 rounded-lg bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700">
          筛选
        </button>
      </form>

      <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

      <!-- List -->
      <div v-if="loading" class="space-y-3">
        <div v-for="i in 4" :key="i" class="h-24 animate-pulse rounded-xl border border-border bg-white" />
      </div>
      <div v-else-if="page.records.length" class="space-y-3">
        <RouterLink
          v-for="item in page.records"
          :key="item.id"
          :to="`/announcements/${item.id}`"
          class="group flex items-start justify-between gap-4 rounded-xl border border-border bg-white p-4 shadow-sm transition hover:shadow-md"
        >
          <div class="min-w-0">
            <span class="inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium" :class="typeTone[item.type || ''] || 'bg-neutral-100 text-neutral-600'">
              {{ typeLabels[item.type || ''] || item.type || '公告' }}
            </span>
            <h2 class="mt-1.5 font-semibold text-neutral-900 group-hover:text-primary-600">{{ item.title }}</h2>
            <p class="mt-1 line-clamp-1 text-sm text-neutral-500">{{ item.content }}</p>
          </div>
          <p class="flex-shrink-0 text-xs text-neutral-400">{{ formatTime(item.publishTime || item.createdAt) }}</p>
        </RouterLink>
      </div>
      <Empty v-else title="暂无公告" :icon="Megaphone" />

      <!-- Pagination -->
      <div v-if="page.total > page.size" class="mt-6 flex items-center justify-between text-sm text-neutral-500">
        <span>共 {{ page.total }} 条</span>
        <div class="flex gap-1.5">
          <button :disabled="page.current <= 1" class="rounded-lg border border-border px-3 py-1.5 transition hover:bg-neutral-100 disabled:opacity-40" @click="load(page.current - 1)">上一页</button>
          <button :disabled="page.current * page.size >= page.total" class="rounded-lg border border-border px-3 py-1.5 transition hover:bg-neutral-100 disabled:opacity-40" @click="load(page.current + 1)">下一页</button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
