<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { CheckCircle2, ChevronLeft, CircleX, Sparkles } from 'lucide-vue-next'
import { examApi, type ExamSubmission } from '@/lib/api'
import AppLayout from '@/components/layout/AppLayout.vue'

const route = useRoute()
const result = ref<ExamSubmission | null>(null)
const error = ref('')
const examId = computed(() => Number(route.params.id))

async function load() {
  error.value = ''
  try { result.value = await examApi.result(examId.value) }
  catch (e) { error.value = e instanceof Error ? e.message : '加载失败' }
}

function answerStatus(correct?: boolean) {
  if (correct === true) return { label: '正确', cls: 'bg-success-bg text-success' }
  if (correct === false) return { label: '错误', cls: 'bg-danger-bg text-danger' }
  return { label: '待阅卷', cls: 'bg-neutral-100 text-neutral-500' }
}

onMounted(load)
</script>

<template>
  <AppLayout>
    <div class="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <RouterLink to="/exams" class="mb-6 inline-flex items-center gap-1 text-sm text-neutral-500 hover:text-primary-600">
        <ChevronLeft class="h-4 w-4" />返回考试中心
      </RouterLink>

      <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

      <template v-if="result">
        <!-- Score summary -->
        <div class="mb-6 rounded-xl border border-border bg-white p-5 shadow-sm">
          <p class="text-xs text-primary-600">{{ result.courseTitle || `课程 #${result.courseId}` }}</p>
          <h1 class="mt-1 text-xl font-bold text-neutral-900">{{ result.examTitle }}</h1>
          <div class="mt-4 grid grid-cols-3 gap-3">
            <div class="rounded-xl bg-primary-600 p-4 text-white">
              <p class="text-xs font-medium opacity-80">总分</p>
              <p class="mt-1 text-3xl font-bold">{{ result.totalScore }}</p>
            </div>
            <div class="rounded-xl border border-border bg-neutral-100 p-4">
              <p class="text-xs text-neutral-400">客观题</p>
              <p class="mt-1 text-3xl font-bold text-neutral-900">{{ result.objectiveScore }}</p>
            </div>
            <div class="rounded-xl border border-border bg-neutral-100 p-4">
              <p class="text-xs text-neutral-400">简答题</p>
              <p class="mt-1 text-3xl font-bold text-neutral-900">{{ result.subjectiveScore }}</p>
            </div>
          </div>
          <div v-if="result.comment" class="mt-4 rounded-lg bg-neutral-100 p-4">
            <div class="mb-1.5 flex items-center gap-2 text-xs text-neutral-400">
              <Sparkles class="h-3.5 w-3.5" />教师评语
            </div>
            <p class="whitespace-pre-wrap text-sm text-neutral-700">{{ result.comment }}</p>
          </div>
        </div>

        <!-- Answer review -->
        <div class="space-y-4">
          <article v-for="(answer, index) in result.answers" :key="answer.id" class="rounded-xl border border-border bg-white p-5 shadow-sm">
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="text-xs text-neutral-400">第 {{ index + 1 }} 题 · {{ answer.questionType }} · {{ answer.questionScore }} 分</p>
                <h2 class="mt-1.5 whitespace-pre-wrap font-semibold text-neutral-900">{{ answer.questionContent }}</h2>
              </div>
              <span class="flex-shrink-0 inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium" :class="answerStatus(answer.correct).cls">
                <CircleX v-if="answer.correct === false" class="h-3.5 w-3.5" />
                <CheckCircle2 v-else class="h-3.5 w-3.5" />
                {{ answerStatus(answer.correct).label }} · {{ answer.score }} 分
              </span>
            </div>
            <div class="mt-4 grid gap-3 md:grid-cols-2">
              <div class="rounded-lg bg-neutral-100 px-4 py-3">
                <p class="text-xs text-neutral-400">我的答案</p>
                <p class="mt-1.5 whitespace-pre-wrap text-sm text-neutral-700">{{ answer.answer || '未作答' }}</p>
              </div>
              <div class="rounded-lg bg-neutral-100 px-4 py-3">
                <p class="text-xs text-neutral-400">参考答案 / 评语</p>
                <p class="mt-1.5 whitespace-pre-wrap text-sm text-neutral-700">{{ answer.correctAnswer || answer.comment || '等待教师批改' }}</p>
              </div>
            </div>
          </article>
        </div>
      </template>
    </div>
  </AppLayout>
</template>
