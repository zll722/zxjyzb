<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { BookCheck, Clock, FileText } from 'lucide-vue-next'
import { formatDeadline, formatDateTime, homeworkApi, type Homework } from '@/lib/api'
import AppLayout from '@/components/layout/AppLayout.vue'
import Empty from '@/components/Empty.vue'

const homeworks = ref<Homework[]>([])
const loading = ref(false)
const error = ref('')

const statusMap: Record<string, { label: string; cls: string }> = {
  PENDING:  { label: '待提交', cls: 'bg-neutral-100 text-neutral-600' },
  SUBMITTED:{ label: '已提交', cls: 'bg-primary-50 text-primary-700' },
  REVIEWED: { label: '已批改', cls: 'bg-success-bg text-success' },
}

async function load() {
  loading.value = true; error.value = ''
  try {
    const page = await homeworkApi.page({ current: 1, size: 50 })
    homeworks.value = page.records
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <AppLayout>
    <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">学生作业中心</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">我的作业</h1>
        <p class="mt-1 text-sm text-neutral-500">查看作业、提交附件并跟踪批改结果。</p>
      </div>

      <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>
      <div v-if="loading" class="grid gap-4 lg:grid-cols-2">
        <div v-for="i in 4" :key="i" class="h-40 animate-pulse rounded-xl border border-border bg-white" />
      </div>

      <div v-else-if="homeworks.length > 0" class="grid gap-4 lg:grid-cols-2">
        <RouterLink
          v-for="homework in homeworks"
          :key="homework.id"
          :to="`/homeworks/${homework.id}`"
          class="group rounded-xl border border-border bg-white p-5 shadow-sm transition hover:shadow-md"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <p class="text-xs text-primary-600">{{ homework.courseTitle || `课程 #${homework.courseId}` }}</p>
              <h2 class="mt-1 truncate font-semibold text-neutral-900 group-hover:text-primary-600">{{ homework.title }}</h2>
            </div>
            <span
              class="flex-shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium"
              :class="statusMap[homework.submissionStatus || 'PENDING']?.cls || 'bg-neutral-100 text-neutral-600'"
            >
              {{ statusMap[homework.submissionStatus || 'PENDING']?.label || '待提交' }}
            </span>
          </div>

          <p class="mt-2 line-clamp-2 text-sm text-neutral-500">{{ homework.description || '暂无作业说明' }}</p>

          <div class="mt-4 flex flex-wrap gap-2 text-xs text-neutral-400">
            <span
              class="inline-flex items-center gap-1 rounded-md px-2.5 py-1"
              :class="homework.deadline && new Date(homework.deadline) < new Date()
                ? 'bg-danger-bg text-danger'
                : 'bg-neutral-100 text-neutral-500'"
            >
              <Clock class="h-3.5 w-3.5" />截止：{{ formatDeadline(homework.deadline) }}
            </span>
            <span v-if="homework.submittedAt" class="inline-flex items-center gap-1 rounded-md bg-neutral-100 px-2.5 py-1">
              <FileText class="h-3.5 w-3.5" />提交：{{ formatDateTime(homework.submittedAt) }}
            </span>
          </div>
        </RouterLink>
      </div>

      <Empty v-else title="暂无作业" :icon="BookCheck" />
    </div>
  </AppLayout>
</template>
