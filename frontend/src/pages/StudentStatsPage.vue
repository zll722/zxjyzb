<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import Empty from '@/components/Empty.vue'
import { statsApi, type StudentStats } from '@/lib/api'
import AppLayout from '@/components/layout/AppLayout.vue'
import { ChevronLeft } from 'lucide-vue-next'

const stats = ref<StudentStats | null>(null)
const loading = ref(false)
const error = ref('')

const cards = [
  { key: 'learnedCourseCount', label: '学习课程', suffix: '门', color: 'bg-primary-50 text-primary-600' },
  { key: 'favoriteCount', label: '收藏课程', suffix: '门', color: 'bg-violet-50 text-violet-600' },
  { key: 'homeworkSubmissionCount', label: '作业提交', suffix: '次', color: 'bg-sky-50 text-sky-600' },
  { key: 'examParticipationCount', label: '考试参与', suffix: '次', color: 'bg-warning-bg text-warning' },
  { key: 'averageScore', label: '考试均分', suffix: '分', color: 'bg-success-bg text-success' },
] as const

function formatTime(value?: string) {
  if (!value) return '暂无时间'
  return value.replace('T', ' ').slice(0, 16)
}

async function loadStats() {
  loading.value = true; error.value = ''
  try { stats.value = await statsApi.student() }
  catch (err) { error.value = err instanceof Error ? err.message : '统计加载失败' }
  finally { loading.value = false }
}

onMounted(loadStats)
</script>

<template>
  <AppLayout>
    <div class="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <RouterLink to="/dashboard" class="mb-6 inline-flex items-center gap-1 text-sm text-neutral-500 hover:text-primary-600">
        <ChevronLeft class="h-4 w-4" />返回工作台
      </RouterLink>

      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">学生数据看板</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">我的学习数据</h1>
      </div>

      <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>
      <div v-if="loading" class="grid gap-4 md:grid-cols-5">
        <div v-for="i in 5" :key="i" class="h-24 animate-pulse rounded-xl border border-border bg-white" />
      </div>

      <template v-else-if="stats">
        <!-- Stats grid -->
        <div class="mb-8 grid gap-4 sm:grid-cols-3 lg:grid-cols-5">
          <div v-for="card in cards" :key="card.key" class="rounded-xl border border-border bg-white p-4 shadow-sm">
            <div class="mb-3 grid h-9 w-9 place-items-center rounded-lg text-lg font-bold" :class="card.color">
              {{ stats[card.key] ?? 0 }}
            </div>
            <p class="text-xs text-neutral-400">{{ card.label }}</p>
            <p class="text-2xl font-bold text-neutral-900">{{ stats[card.key] ?? 0 }}<span class="ml-0.5 text-sm font-normal text-neutral-400">{{ card.suffix }}</span></p>
          </div>
        </div>

        <!-- Recent records -->
        <div class="rounded-xl border border-border bg-white p-5 shadow-sm">
          <h2 class="mb-4 font-semibold text-neutral-900">近期学习轨迹</h2>
          <div v-if="stats.recentRecords.length" class="space-y-3">
            <div v-for="record in stats.recentRecords" :key="`${record.courseId}-${record.chapterId}`" class="flex items-center justify-between gap-4 rounded-lg border border-border p-3">
              <div class="min-w-0">
                <p class="font-medium text-neutral-900 truncate">{{ record.courseTitle || '未知课程' }}</p>
                <p class="text-xs text-neutral-400">{{ record.chapterTitle || '未知章节' }} · 最后学习：{{ formatTime(record.lastWatchTime) }}</p>
              </div>
              <span class="flex-shrink-0 rounded-full bg-primary-100 px-3 py-0.5 text-sm font-semibold text-primary-700">{{ record.progress }}%</span>
            </div>
          </div>
          <Empty v-else title="暂无学习记录，去课程中心开始第一节课吧" />
        </div>
      </template>
    </div>
  </AppLayout>
</template>
