<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CheckCircle2, FileQuestion, Send } from 'lucide-vue-next'
import { examApi, formatDateTime, type Exam, type ExamQuestion } from '@/lib/api'

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
    questions.value.forEach((question) => {
      answers[question.id] = ''
    })
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
  message.value = ''
  error.value = ''
  try {
    await examApi.submit(examId.value, questions.value.map((question) => ({ questionId: question.id, answer: answers[question.id] || '' })))
    message.value = '答卷已提交'
    router.push(`/exams/${examId.value}/result`)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '提交失败'
  }
}

onMounted(load)
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-5xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/exams" class="text-sm text-cyan-200">返回考试中心</RouterLink>
        <RouterLink v-if="exam?.submissionId" :to="`/exams/${exam.id}/result`" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">查看结果</RouterLink>
      </nav>

      <section v-if="exam" class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">{{ exam.courseTitle || `课程 #${exam.courseId}` }} · {{ exam.teacherName || '教师' }}</p>
        <h1 class="mt-2 text-4xl font-black">{{ exam.title }}</h1>
        <p class="mt-4 whitespace-pre-wrap text-slate-300">{{ exam.description || '暂无考试说明' }}</p>
        <div class="mt-6 flex flex-wrap gap-3 text-sm">
          <span class="rounded-full bg-cyan-300/10 px-4 py-2 text-cyan-100">总分：{{ exam.totalScore }}</span>
          <span class="rounded-full border border-white/10 px-4 py-2 text-slate-300">时长：{{ exam.durationMinutes || '不限' }} 分钟</span>
          <span class="rounded-full border border-white/10 px-4 py-2 text-slate-300">结束：{{ exam.endTime ? formatDateTime(exam.endTime) : '不限' }}</span>
        </div>
      </section>

      <p v-if="message" class="mt-6 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
      <p v-if="error" class="mt-6 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>

      <form class="mt-8 space-y-5" @submit.prevent="submitExam">
        <article v-for="(question, index) in questions" :key="question.id" class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p class="text-sm text-cyan-200">第 {{ index + 1 }} 题 · {{ question.type }} · {{ question.score }} 分</p>
              <h2 class="mt-2 whitespace-pre-wrap text-xl font-black">{{ question.content }}</h2>
            </div>
            <FileQuestion class="h-6 w-6 text-cyan-200" />
          </div>

          <div v-if="question.type === 'SINGLE'" class="mt-5 grid gap-3">
            <label v-for="option in question.options" :key="option" class="flex cursor-pointer items-center gap-3 rounded-2xl border border-white/10 bg-slate-900 px-4 py-3">
              <input v-model="answers[question.id]" type="radio" :name="`q-${question.id}`" :value="option" />
              <span>{{ option }}</span>
            </label>
          </div>

          <div v-else-if="question.type === 'MULTIPLE'" class="mt-5 grid gap-3">
            <label v-for="option in question.options" :key="option" class="flex cursor-pointer items-center gap-3 rounded-2xl border border-white/10 bg-slate-900 px-4 py-3">
              <input type="checkbox" :checked="selected(question.id, option)" @change="toggleMultiple(question.id, option)" />
              <span>{{ option }}</span>
            </label>
          </div>

          <div v-else-if="question.type === 'JUDGE'" class="mt-5 flex flex-wrap gap-3">
            <label class="flex cursor-pointer items-center gap-3 rounded-2xl border border-white/10 bg-slate-900 px-5 py-3">
              <input v-model="answers[question.id]" type="radio" :name="`q-${question.id}`" value="正确" />正确
            </label>
            <label class="flex cursor-pointer items-center gap-3 rounded-2xl border border-white/10 bg-slate-900 px-5 py-3">
              <input v-model="answers[question.id]" type="radio" :name="`q-${question.id}`" value="错误" />错误
            </label>
          </div>

          <textarea v-else v-model="answers[question.id]" rows="6" class="mt-5 w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="输入简答题答案" />
        </article>

        <button v-if="questions.length" class="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-cyan-300 px-6 py-4 font-bold text-slate-950 hover:bg-cyan-200">
          <Send class="h-5 w-5" />提交答卷
        </button>
      </form>

      <div v-if="questions.length === 0" class="mt-12 rounded-[2rem] border border-white/10 p-10 text-center text-slate-300">
        <CheckCircle2 class="mx-auto mb-4 h-10 w-10 text-cyan-200" />
        暂无题目
      </div>
    </div>
  </main>
</template>
