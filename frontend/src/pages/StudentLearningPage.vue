<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ChevronLeft } from 'lucide-vue-next'
import Empty from '@/components/Empty.vue'
import { courseApi, type LearningRecord, type PageResult } from '@/lib/api'
import AppLayout from '@/components/layout/AppLayout.vue'

const page = ref<PageResult<LearningRecord>>({ records: [], total: 0, size: 10, current: 1 })
const loading = ref(false)
const error = ref('')

function formatTime(value?: string) {
  if (!value) return '暂无时间'
  return value.replace('T', ' ').slice(0, 16)
}

async function load(current = page.value.current) {
  loading.value = true; error.value = ''
  try { page.value = await courseApi.learningRecords({ current, size: page.value.size }) }
  catch (err) { error.value = err instanceof Error ? err.message : '学习记录加载失败' }
  finally { loading.value = false }
}

onMounted(() => load(1))
</script>

<template>
  <AppLayout>
    <div class="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <RouterLink to="/dashboard" class="mb-6 inline-flex items-center gap-1 text-sm text-neutral-500 hover:text-primary-600">
        <ChevronLeft class="h-4 w-4" />返回工作台
      </RouterLink>

      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">学习中心</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">我的学习</h1>
        <p class="mt-1 text-sm text-neutral-500">查看课程学习记录、最后观看章节与学习进度。</p>
      </div>

      <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

      <div v-if="loading" class="space-y-3">
        <div v-for="i in 4" :key="i" class="h-28 animate-pulse rounded-xl border border-border bg-white" />
      </div>

      <div v-else class="space-y-3">
        <article v-for="record in page.records" :key="`${record.courseId}-${record.chapterId}`" class="rounded-xl border border-border bg-white p-5 shadow-sm">
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="text-xs text-neutral-400">最后观看：{{ record.chapterTitle || '未知章节' }}</p>
              <h2 class="mt-1 font-semibold text-neutral-900">{{ record.courseTitle || '未知课程' }}</h2>
              <p class="mt-1 text-xs text-neutral-400">{{ formatTime(record.lastWatchTime) }}</p>
            </div>
            <RouterLink :to="`/courses/${record.courseId}`" class="flex-shrink-0 rounded-lg bg-primary-600 px-3 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-primary-700">
              继续学习
            </RouterLink>
          </div>
          <div class="mt-4">
            <div class="mb-1.5 flex items-center justify-between text-xs text-neutral-400">
              <span>学习进度</span>
              <span>{{ record.progress || 0 }}%</span>
            </div>
            <div class="h-2 overflow-hidden rounded-full bg-neutral-100">
              <div class="h-full rounded-full bg-primary-600 transition-all" :style="{ width: `${Math.min(100, Math.max(0, record.progress || 0))}%` }" />
            </div>
          </div>
        </article>

        <Empty v-if="page.records.length === 0" title="暂无学习记录，去课程中心开始第一节课吧" />
      </div>

      <div v-if="page.total > page.size" class="mt-6 flex items-center justify-center gap-3 text-sm">
        <button class="rounded-lg border border-border px-4 py-2 text-neutral-600 transition hover:bg-neutral-100 disabled:opacity-40" :disabled="page.current <= 1 || loading" @click="load(page.current - 1)">上一页</button>
        <span class="text-neutral-400">第 {{ page.current }} 页 / 共 {{ Math.ceil(page.total / page.size) }} 页</span>
        <button class="rounded-lg border border-border px-4 py-2 text-neutral-600 transition hover:bg-neutral-100 disabled:opacity-40" :disabled="page.current * page.size >= page.total || loading" @click="load(page.current + 1)">下一页</button>
      </div>
    </div>
  </AppLayout>
</template>
