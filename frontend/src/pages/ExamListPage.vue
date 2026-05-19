<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ClipboardCheck, FileQuestion } from 'lucide-vue-next'
import { examApi, type Exam } from '@/lib/api'

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

function statusText(exam: Exam) {
  if (exam.submissionStatus === 'REVIEWED') return '已出分'
  if (exam.submissionStatus === 'PENDING_REVIEW') return '待阅卷'
  if (exam.submissionStatus === 'SUBMITTED') return '已提交'
  return '待答题'
}

onMounted(load)
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/dashboard" class="text-sm text-cyan-200">返回工作台</RouterLink>
        <RouterLink to="/homeworks" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">作业中心</RouterLink>
      </nav>
      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">考试中心</p>
        <h1 class="mt-2 text-4xl font-black">课程测验与自动判题</h1>
        <p class="mt-4 max-w-3xl text-slate-300">查看已发布考试，完成客观题自动判分，简答题等待教师阅卷。</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <section class="mt-8 grid gap-5 lg:grid-cols-2">
        <article v-for="exam in exams" :key="exam.id" class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="text-sm text-cyan-200">{{ exam.courseTitle || `课程 #${exam.courseId}` }} · {{ exam.teacherName || '教师' }}</p>
              <h2 class="mt-2 text-2xl font-black">{{ exam.title }}</h2>
              <p class="mt-3 line-clamp-2 text-sm text-slate-300">{{ exam.description || '暂无考试说明' }}</p>
            </div>
            <span class="rounded-full bg-cyan-300/10 px-4 py-2 text-sm text-cyan-100">{{ statusText(exam) }}</span>
          </div>
          <div class="mt-5 grid gap-3 text-sm text-slate-300 md:grid-cols-3">
            <span class="rounded-2xl bg-slate-900 px-4 py-3">总分：{{ exam.totalScore }}</span>
            <span class="rounded-2xl bg-slate-900 px-4 py-3">时长：{{ exam.durationMinutes || '不限' }} 分钟</span>
            <span class="rounded-2xl bg-slate-900 px-4 py-3">得分：{{ exam.score ?? '未提交' }}</span>
          </div>
          <div class="mt-6 flex flex-wrap gap-3">
            <RouterLink v-if="!exam.submissionId" :to="`/exams/${exam.id}`" class="inline-flex items-center gap-2 rounded-full bg-cyan-300 px-5 py-2 text-sm font-bold text-slate-950"><FileQuestion class="h-4 w-4" />开始答题</RouterLink>
            <RouterLink v-else :to="`/exams/${exam.id}/result`" class="inline-flex items-center gap-2 rounded-full bg-white px-5 py-2 text-sm font-bold text-slate-950"><ClipboardCheck class="h-4 w-4" />查看结果</RouterLink>
          </div>
        </article>
      </section>

      <div v-if="exams.length === 0" class="mt-12 rounded-[2rem] border border-white/10 p-10 text-center text-slate-300">
        <FileQuestion class="mx-auto mb-4 h-10 w-10 text-cyan-200" />
        暂无可参加考试
      </div>
    </div>
  </main>
</template>
