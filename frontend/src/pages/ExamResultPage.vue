<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { CheckCircle2, CircleX, Sparkles } from 'lucide-vue-next'
import { examApi, type ExamSubmission } from '@/lib/api'

const route = useRoute()
const result = ref<ExamSubmission | null>(null)
const error = ref('')
const examId = computed(() => Number(route.params.id))

async function load() {
  error.value = ''
  try {
    result.value = await examApi.result(examId.value)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  }
}

function answerStatus(correct?: boolean) {
  if (correct === true) return '正确'
  if (correct === false) return '错误'
  return '待阅卷'
}

onMounted(load)
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-5xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/exams" class="text-sm text-cyan-200">返回考试中心</RouterLink>
        <RouterLink :to="`/exams/${examId}`" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">考试详情</RouterLink>
      </nav>

      <p v-if="error" class="rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>

      <section v-if="result" class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">{{ result.courseTitle || `课程 #${result.courseId}` }}</p>
        <h1 class="mt-2 text-4xl font-black">{{ result.examTitle }}</h1>
        <div class="mt-8 grid gap-4 md:grid-cols-3">
          <div class="rounded-3xl bg-cyan-300 p-5 text-slate-950">
            <p class="text-sm font-bold opacity-70">总分</p>
            <p class="mt-2 text-4xl font-black">{{ result.totalScore }}</p>
          </div>
          <div class="rounded-3xl border border-white/10 bg-slate-900 p-5">
            <p class="text-sm text-cyan-200">客观题</p>
            <p class="mt-2 text-3xl font-black">{{ result.objectiveScore }}</p>
          </div>
          <div class="rounded-3xl border border-white/10 bg-slate-900 p-5">
            <p class="text-sm text-cyan-200">简答题</p>
            <p class="mt-2 text-3xl font-black">{{ result.subjectiveScore }}</p>
          </div>
        </div>
        <div class="mt-6 rounded-3xl border border-white/10 bg-slate-900 p-5">
          <div class="flex items-center gap-3 text-cyan-200"><Sparkles class="h-5 w-5" />教师评语</div>
          <p class="mt-3 whitespace-pre-wrap text-slate-300">{{ result.comment || '暂无评语' }}</p>
        </div>
      </section>

      <section v-if="result" class="mt-8 space-y-5">
        <article v-for="(answer, index) in result.answers" :key="answer.id" class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p class="text-sm text-cyan-200">第 {{ index + 1 }} 题 · {{ answer.questionType }} · {{ answer.questionScore }} 分</p>
              <h2 class="mt-2 whitespace-pre-wrap text-xl font-black">{{ answer.questionContent }}</h2>
            </div>
            <span class="inline-flex items-center gap-2 rounded-full px-4 py-2 text-sm" :class="answer.correct === false ? 'bg-red-500/10 text-red-100' : 'bg-cyan-300/10 text-cyan-100'">
              <CircleX v-if="answer.correct === false" class="h-4 w-4" />
              <CheckCircle2 v-else class="h-4 w-4" />
              {{ answerStatus(answer.correct) }} · {{ answer.score }} 分
            </span>
          </div>
          <div class="mt-5 grid gap-4 md:grid-cols-2">
            <div class="rounded-2xl bg-slate-900 p-4 text-sm text-slate-300">
              <p class="text-cyan-200">我的答案</p>
              <p class="mt-2 whitespace-pre-wrap">{{ answer.answer || '未作答' }}</p>
            </div>
            <div class="rounded-2xl bg-slate-900 p-4 text-sm text-slate-300">
              <p class="text-cyan-200">参考答案 / 评语</p>
              <p class="mt-2 whitespace-pre-wrap">{{ answer.correctAnswer || answer.comment || '等待教师批改' }}</p>
            </div>
          </div>
        </article>
      </section>
    </div>
  </main>
</template>
