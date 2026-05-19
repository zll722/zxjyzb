<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ClipboardCheck, Save, Sparkles } from 'lucide-vue-next'
import { examApi, type Exam, type ExamSubmission } from '@/lib/api'

const route = useRoute()
const exam = ref<Exam | null>(null)
const submissions = ref<ExamSubmission[]>([])
const selected = ref<ExamSubmission | null>(null)
const grades = reactive<Record<number, { score: number; comment: string }>>({})
const comment = ref('')
const message = ref('')
const error = ref('')
const examId = computed(() => Number(route.params.id))

async function load() {
  error.value = ''
  try {
    exam.value = await examApi.detail(examId.value)
    const page = await examApi.submissions(examId.value, { current: 1, size: 50 })
    submissions.value = page.records
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  }
}

async function openSubmission(submission: ExamSubmission) {
  selected.value = await examApi.submissionDetail(examId.value, submission.id)
  comment.value = selected.value.comment || ''
  selected.value.answers?.forEach((answer) => {
    if (answer.questionType === 'SHORT') grades[answer.id] = { score: answer.score || 0, comment: answer.comment || '' }
  })
}

async function gradeSubmission() {
  if (!selected.value) return
  await examApi.grade(examId.value, selected.value.id, {
    comment: comment.value,
    answers: selected.value.answers?.filter((answer) => answer.questionType === 'SHORT').map((answer) => ({ answerId: answer.id, score: grades[answer.id]?.score || 0, comment: grades[answer.id]?.comment || '' })),
  })
  message.value = '阅卷已保存'
  await load()
  await openSubmission(selected.value)
}

onMounted(load)
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/teacher/exams" class="text-sm text-cyan-200">返回考试管理</RouterLink>
        <RouterLink v-if="exam" :to="`/teacher/exams/${exam.id}/questions`" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">题目管理</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">教师阅卷</p>
        <h1 class="mt-2 text-4xl font-black">{{ exam?.title || '考试提交' }}</h1>
        <p class="mt-4 text-slate-300">查看学生答卷、客观题得分与简答题人工评分。</p>
        <p v-if="message" class="mt-4 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <section class="mt-8 grid gap-6 lg:grid-cols-[360px_1fr]">
        <aside class="space-y-4">
          <button v-for="submission in submissions" :key="submission.id" class="w-full rounded-[2rem] border border-white/10 bg-white/8 p-5 text-left backdrop-blur hover:bg-white/10" @click="openSubmission(submission)">
            <div class="flex items-center justify-between gap-3">
              <p class="font-bold">{{ submission.studentName || `学生 #${submission.studentId}` }}</p>
              <span class="rounded-full bg-cyan-300/10 px-3 py-1 text-xs text-cyan-100">{{ submission.status }}</span>
            </div>
            <p class="mt-3 text-sm text-slate-300">总分：{{ submission.totalScore }} · 客观题：{{ submission.objectiveScore }}</p>
            <p class="mt-1 text-xs text-slate-400">{{ submission.submittedAt }}</p>
          </button>
          <div v-if="submissions.length === 0" class="rounded-[2rem] border border-white/10 p-8 text-center text-slate-300">
            <ClipboardCheck class="mx-auto mb-3 h-8 w-8 text-cyan-200" />暂无提交
          </div>
        </aside>

        <section v-if="selected" class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p class="text-sm text-cyan-200">{{ selected.studentName || `学生 #${selected.studentId}` }}</p>
              <h2 class="mt-2 text-3xl font-black">答题详情</h2>
            </div>
            <div class="rounded-3xl bg-cyan-300 px-6 py-4 text-slate-950">
              <p class="text-sm font-bold opacity-70">总分</p>
              <p class="text-3xl font-black">{{ selected.totalScore }}</p>
            </div>
          </div>

          <div class="mt-6 space-y-5">
            <article v-for="(answer, index) in selected.answers" :key="answer.id" class="rounded-3xl bg-slate-900 p-5">
              <p class="text-sm text-cyan-200">第 {{ index + 1 }} 题 · {{ answer.questionType }} · {{ answer.questionScore }} 分</p>
              <h3 class="mt-2 whitespace-pre-wrap text-lg font-bold">{{ answer.questionContent }}</h3>
              <div class="mt-4 grid gap-4 md:grid-cols-2">
                <div class="rounded-2xl bg-white/5 p-4 text-sm text-slate-300">
                  <p class="text-cyan-200">学生答案</p>
                  <p class="mt-2 whitespace-pre-wrap">{{ answer.answer || '未作答' }}</p>
                </div>
                <div class="rounded-2xl bg-white/5 p-4 text-sm text-slate-300">
                  <p class="text-cyan-200">参考答案</p>
                  <p class="mt-2 whitespace-pre-wrap">{{ answer.correctAnswer || '简答题' }}</p>
                </div>
              </div>
              <div v-if="answer.questionType === 'SHORT'" class="mt-4 grid gap-3 md:grid-cols-[140px_1fr]">
                <input v-model.number="grades[answer.id].score" type="number" min="0" :max="answer.questionScore" step="0.1" class="rounded-2xl border border-white/10 bg-slate-950 px-4 py-3 outline-none focus:border-cyan-300" />
                <input v-model="grades[answer.id].comment" class="rounded-2xl border border-white/10 bg-slate-950 px-4 py-3 outline-none focus:border-cyan-300" placeholder="题目评语" />
              </div>
              <p v-else class="mt-4 text-sm" :class="answer.correct ? 'text-cyan-200' : 'text-red-100'">自动判题：{{ answer.correct ? '正确' : '错误' }} · {{ answer.score }} 分</p>
            </article>
          </div>

          <div class="mt-6 rounded-3xl bg-slate-900 p-5">
            <div class="flex items-center gap-3 text-cyan-200"><Sparkles class="h-5 w-5" />总评</div>
            <textarea v-model="comment" rows="4" class="mt-4 w-full rounded-2xl border border-white/10 bg-slate-950 px-4 py-3 outline-none focus:border-cyan-300" placeholder="输入教师评语" />
            <button class="mt-4 inline-flex items-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950" @click="gradeSubmission"><Save class="h-4 w-4" />保存阅卷</button>
          </div>
        </section>

        <section v-else class="rounded-[2rem] border border-white/10 bg-white/8 p-10 text-center text-slate-300 backdrop-blur">
          请选择一份答卷查看详情
        </section>
      </section>
    </div>
  </main>
</template>
