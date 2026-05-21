<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ChevronLeft, FileQuestion, Send } from 'lucide-vue-next'
import { examApi, formatDateTime, type Exam, type ExamQuestion } from '@/lib/api'
import AppLayout from '@/components/layout/AppLayout.vue'

const route = useRoute()
const router = useRouter()
const exam = ref<Exam | null>(null)
const questions = ref<ExamQuestion[]>([])
const answers = reactive<Record<number, string>>({})
const message = ref('')
const error = ref('')
const examId = computed(() => Number(route.params.id))

async function load() {
  error.value = ''
  try {
    exam.value = await examApi.detail(examId.value)
    questions.value = await examApi.questions(examId.value)
    questions.value.forEach((question) => { answers[question.id] = '' })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  }
}

function toggleMultiple(questionId: number, option: string) {
  const current = answers[questionId] ? answers[questionId].split(',').filter(Boolean) : []
  answers[questionId] = current.includes(option) ? current.filter((item) => item !== option).join(',') : [...current, option].join(',')
}

function selected(questionId: number, option: string) {
  return answers[questionId]?.split(',').includes(option)
}

async function submitExam() {
  message.value = ''; error.value = ''
  try {
    await examApi.submit(examId.value, questions.value.map((q) => ({ questionId: q.id, answer: answers[q.id] || '' })))
    router.push(`/exams/${examId.value}/result`)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '提交失败'
  }
}

onMounted(load)
</script>

<template>
  <AppLayout>
    <div class="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-6 flex items-center justify-between">
        <RouterLink to="/exams" class="inline-flex items-center gap-1 text-sm text-neutral-500 hover:text-primary-600">
          <ChevronLeft class="h-4 w-4" />返回考试中心
        </RouterLink>
        <RouterLink v-if="exam?.submissionId" :to="`/exams/${exam.id}/result`" class="rounded-lg border border-border px-3 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-100">
          查看结果
        </RouterLink>
      </div>

      <!-- Exam info -->
      <div v-if="exam" class="mb-6 rounded-xl border border-border bg-white p-5 shadow-sm">
        <p class="text-xs text-primary-600">{{ exam.courseTitle || `课程 #${exam.courseId}` }} · {{ exam.teacherName || '教师' }}</p>
        <h1 class="mt-1 text-xl font-bold text-neutral-900">{{ exam.title }}</h1>
        <p v-if="exam.description" class="mt-2 text-sm text-neutral-500">{{ exam.description }}</p>
        <div class="mt-3 flex flex-wrap gap-2 text-xs text-neutral-500">
          <span class="rounded-md bg-neutral-100 px-2.5 py-1">总分：{{ exam.totalScore }}</span>
          <span class="rounded-md bg-neutral-100 px-2.5 py-1">时长：{{ exam.durationMinutes || '不限' }} 分钟</span>
          <span v-if="exam.endTime" class="rounded-md bg-neutral-100 px-2.5 py-1">结束：{{ formatDateTime(exam.endTime) }}</span>
        </div>
      </div>

      <div v-if="message" class="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</div>
      <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

      <!-- Questions -->
      <form class="space-y-4" @submit.prevent="submitExam">
        <article v-for="(question, index) in questions" :key="question.id" class="rounded-xl border border-border bg-white p-5 shadow-sm">
          <div class="flex items-start justify-between gap-3">
            <div>
              <p class="text-xs text-neutral-400">第 {{ index + 1 }} 题 · {{ question.type }} · {{ question.score }} 分</p>
              <h2 class="mt-1.5 whitespace-pre-wrap font-semibold text-neutral-900">{{ question.content }}</h2>
            </div>
            <FileQuestion class="h-5 w-5 flex-shrink-0 text-neutral-300" />
          </div>

          <!-- Single choice -->
          <div v-if="question.type === 'SINGLE'" class="mt-4 space-y-2">
            <label
              v-for="option in question.options"
              :key="option"
              class="flex cursor-pointer items-center gap-3 rounded-lg border px-4 py-3 text-sm transition"
              :class="answers[question.id] === option ? 'border-primary-400 bg-primary-50' : 'border-border hover:bg-neutral-100'"
            >
              <input v-model="answers[question.id]" type="radio" :name="`q-${question.id}`" :value="option" class="accent-primary-600" />
              <span :class="answers[question.id] === option ? 'text-primary-700 font-medium' : 'text-neutral-700'">{{ option }}</span>
            </label>
          </div>

          <!-- Multiple choice -->
          <div v-else-if="question.type === 'MULTIPLE'" class="mt-4 space-y-2">
            <label
              v-for="option in question.options"
              :key="option"
              class="flex cursor-pointer items-center gap-3 rounded-lg border px-4 py-3 text-sm transition"
              :class="selected(question.id, option) ? 'border-primary-400 bg-primary-50' : 'border-border hover:bg-neutral-100'"
            >
              <input type="checkbox" :checked="selected(question.id, option)" class="accent-primary-600" @change="toggleMultiple(question.id, option)" />
              <span :class="selected(question.id, option) ? 'text-primary-700 font-medium' : 'text-neutral-700'">{{ option }}</span>
            </label>
          </div>

          <!-- Judge -->
          <div v-else-if="question.type === 'JUDGE'" class="mt-4 flex gap-3">
            <label
              v-for="val in ['正确', '错误']"
              :key="val"
              class="flex cursor-pointer items-center gap-2 rounded-lg border px-5 py-3 text-sm transition"
              :class="answers[question.id] === val ? 'border-primary-400 bg-primary-50 text-primary-700 font-medium' : 'border-border hover:bg-neutral-100 text-neutral-700'"
            >
              <input v-model="answers[question.id]" type="radio" :name="`q-${question.id}`" :value="val" class="accent-primary-600" />{{ val }}
            </label>
          </div>

          <!-- Essay -->
          <textarea
            v-else
            v-model="answers[question.id]"
            rows="5"
            placeholder="输入简答题答案..."
            class="mt-4 w-full rounded-lg border border-border bg-white px-3 py-2.5 text-sm text-neutral-900 outline-none transition placeholder:text-neutral-400 focus:border-primary-400 focus:ring-2 focus:ring-primary-100"
          />
        </article>

        <button
          v-if="questions.length"
          type="submit"
          class="w-full rounded-xl bg-primary-600 py-3 font-semibold text-white shadow-sm transition hover:bg-primary-700"
        >
          <span class="inline-flex items-center gap-2"><Send class="h-4 w-4" />提交答卷</span>
        </button>
      </form>

      <div v-if="questions.length === 0 && !error" class="mt-8 rounded-xl border border-border bg-white p-10 text-center text-sm text-neutral-400">暂无题目</div>
    </div>
  </AppLayout>
</template>
