<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ClipboardCheck, FileQuestion } from 'lucide-vue-next'
import { examApi, type Exam } from '@/lib/api'
import AppLayout from '@/components/layout/AppLayout.vue'
import Empty from '@/components/Empty.vue'

const exams = ref<Exam[]>([])
const error = ref('')

async function load() {
  error.value = ''
  try {
    const page = await examApi.page({ current: 1, size: 50 })
    exams.value = page.records
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  }
}

function statusInfo(exam: Exam): { label: string; cls: string } {
  if (exam.submissionStatus === 'REVIEWED') return { label: '已出分', cls: 'bg-success-bg text-success' }
  if (exam.submissionStatus === 'PENDING_REVIEW') return { label: '待阅卷', cls: 'bg-warning-bg text-warning' }
  if (exam.submissionStatus === 'SUBMITTED') return { label: '已提交', cls: 'bg-primary-50 text-primary-700' }
  return { label: '待答题', cls: 'bg-neutral-100 text-neutral-600' }
}

onMounted(load)
</script>

<template>
  <AppLayout>
    <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">考试中心</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">课程测验与自动判题</h1>
        <p class="mt-1 text-sm text-neutral-500">查看已发布考试，完成客观题自动判分，简答题等待教师阅卷。</p>
      </div>

      <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

      <div class="grid gap-4 lg:grid-cols-2">
        <article v-for="exam in exams" :key="exam.id" class="rounded-xl border border-border bg-white p-5 shadow-sm">
          <div class="flex items-start justify-between gap-3">
            <div>
              <p class="text-xs text-primary-600">{{ exam.courseTitle || `课程 #${exam.courseId}` }} · {{ exam.teacherName || '教师' }}</p>
              <h2 class="mt-1 font-semibold text-neutral-900">{{ exam.title }}</h2>
              <p class="mt-1.5 line-clamp-2 text-sm text-neutral-500">{{ exam.description || '暂无考试说明' }}</p>
            </div>
            <span class="flex-shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium" :class="statusInfo(exam).cls">
              {{ statusInfo(exam).label }}
            </span>
          </div>

          <div class="mt-4 flex flex-wrap gap-2 text-xs text-neutral-500">
            <span class="rounded-md bg-neutral-100 px-2.5 py-1">总分：{{ exam.totalScore }}</span>
            <span class="rounded-md bg-neutral-100 px-2.5 py-1">时长：{{ exam.durationMinutes || '不限' }} 分钟</span>
            <span class="rounded-md px-2.5 py-1" :class="exam.score != null ? 'bg-success-bg text-success' : 'bg-neutral-100'">
              得分：{{ exam.score ?? '未提交' }}
            </span>
          </div>

          <div class="mt-4 flex gap-2">
            <RouterLink
              v-if="!exam.submissionId"
              :to="`/exams/${exam.id}`"
              class="inline-flex items-center gap-1.5 rounded-lg bg-primary-600 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700"
            >
              <FileQuestion class="h-4 w-4" />开始答题
            </RouterLink>
            <RouterLink
              v-else
              :to="`/exams/${exam.id}/result`"
              class="inline-flex items-center gap-1.5 rounded-lg border border-border px-4 py-2 text-sm font-medium text-neutral-700 transition hover:bg-neutral-100"
            >
              <ClipboardCheck class="h-4 w-4" />查看结果
            </RouterLink>
          </div>
        </article>
      </div>

      <Empty v-if="exams.length === 0" title="暂无可参加考试" :icon="FileQuestion" />
    </div>
  </AppLayout>
</template>
