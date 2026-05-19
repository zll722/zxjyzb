<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { BookCheck, Clock, FileText } from 'lucide-vue-next'
import { formatDeadline, formatDateTime, homeworkApi, type Homework } from '@/lib/api'

const homeworks = ref<Homework[]>([])
const loading = ref(false)
const error = ref('')

const statusText = computed(() => ({
  PENDING: '待提交',
  SUBMITTED: '已提交',
  REVIEWED: '已批改',
}))

async function load() {
  loading.value = true
  error.value = ''
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
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/dashboard" class="text-sm text-cyan-200">返回工作台</RouterLink>
        <RouterLink to="/courses" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">课程中心</RouterLink>
      </nav>
      <section class="relative overflow-hidden rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <div class="absolute inset-0 -z-0 bg-[radial-gradient(circle_at_20%_20%,rgba(34,211,238,0.16),transparent_30%),radial-gradient(circle_at_85%_25%,rgba(45,212,191,0.12),transparent_28%)]" />
        <div class="relative">
          <p class="text-sm text-cyan-200">学生作业中心</p>
          <h1 class="mt-2 text-4xl font-black">查看作业、提交附件并跟踪批改结果</h1>
          <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
        </div>
      </section>

      <section class="mt-8 grid gap-5 lg:grid-cols-2">
        <RouterLink v-for="homework in homeworks" :key="homework.id" :to="`/homeworks/${homework.id}`" class="group rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur transition hover:-translate-y-1 hover:border-cyan-300/50">
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="text-sm text-cyan-200">{{ homework.courseTitle || `课程 #${homework.courseId}` }}</p>
              <h2 class="mt-2 text-2xl font-black group-hover:text-cyan-100">{{ homework.title }}</h2>
            </div>
            <span class="rounded-full bg-cyan-300/10 px-4 py-2 text-xs font-bold text-cyan-100">{{ statusText[homework.submissionStatus || 'PENDING'] }}</span>
          </div>
          <p class="mt-4 line-clamp-2 text-sm text-slate-300">{{ homework.description || '暂无作业说明' }}</p>
          <div class="mt-6 flex flex-wrap gap-3 text-sm text-slate-300">
            <span
              class="inline-flex items-center gap-2 rounded-full border px-4 py-2"
              :class="homework.deadline && new Date(homework.deadline) < new Date() ? 'border-red-400/30 text-red-300' : 'border-white/10 text-slate-300'"
            >
              <Clock class="h-4 w-4 text-cyan-200" />截止：{{ formatDeadline(homework.deadline) }}
            </span>
            <span v-if="homework.submittedAt" class="inline-flex items-center gap-2 rounded-full border border-white/10 px-4 py-2 text-slate-300">
              <FileText class="h-4 w-4 text-cyan-200" />提交：{{ formatDateTime(homework.submittedAt) }}
            </span>
          </div>
        </RouterLink>
      </section>

      <div v-if="!loading && homeworks.length === 0" class="mt-12 rounded-[2rem] border border-white/10 p-10 text-center text-slate-300">
        <BookCheck class="mx-auto mb-4 h-10 w-10 text-cyan-200" />
        暂无作业
      </div>
    </div>
  </main>
</template>
