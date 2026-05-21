<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ClipboardCheck, Save, Sparkles } from 'lucide-vue-next'
import { examApi, type Exam, type ExamSubmission } from '@/lib/api'
import Empty from '@/components/Empty.vue'

const route = useRoute()
const exam = ref<Exam | null>(null)
const submissions = ref<ExamSubmission[]>([])
const selectedSub = ref<ExamSubmission | null>(null)
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
  selectedSub.value = await examApi.submissionDetail(examId.value, submission.id)
  comment.value = selectedSub.value.comment || ''
  selectedSub.value.answers?.forEach((answer) => {
    if (answer.questionType === 'SHORT') grades[answer.id] = { score: answer.score || 0, comment: answer.comment || '' }
  })
}

async function gradeSubmission() {
  if (!selectedSub.value) return
  await examApi.grade(examId.value, selectedSub.value.id, {
    comment: comment.value,
    answers: selectedSub.value.answers?.filter((a) => a.questionType === 'SHORT').map((a) => ({ answerId: a.id, score: grades[a.id]?.score || 0, comment: grades[a.id]?.comment || '' })),
  })
  message.value = '阅卷已保存'
  await load()
  await openSubmission(selectedSub.value)
}

onMounted(load)
</script>

<template>
  <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-6 flex items-center justify-end">
        <RouterLink v-if="exam" :to="`/teacher/exams/${exam.id}/questions`" class="rounded-lg border border-border px-3 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-100">
          题目管理
        </RouterLink>
      </div>

      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">教师阅卷</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">{{ exam?.title || '考试提交' }}</h1>
      </div>

      <div v-if="message" class="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</div>
      <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

      <div class="grid gap-6 lg:grid-cols-[300px_1fr]">
        <!-- Submission list -->
        <aside>
          <div class="space-y-2">
            <button
              v-for="submission in submissions"
              :key="submission.id"
              class="w-full rounded-xl border p-4 text-left transition"
              :class="selectedSub?.id === submission.id ? 'border-primary-400 bg-primary-50' : 'border-border bg-white hover:bg-neutral-100 shadow-sm'"
              @click="openSubmission(submission)"
            >
              <div class="flex items-center justify-between gap-2">
                <p class="font-medium text-neutral-900">{{ submission.studentName || `学生 #${submission.studentId}` }}</p>
                <span class="rounded-full bg-neutral-100 px-2.5 py-0.5 text-xs text-neutral-500">{{ submission.status }}</span>
              </div>
              <p class="mt-1.5 text-xs text-neutral-400">总分：{{ submission.totalScore }} · 客观：{{ submission.objectiveScore }}</p>
              <p class="text-xs text-neutral-400">{{ submission.submittedAt }}</p>
            </button>
          </div>
          <Empty v-if="submissions.length === 0" title="暂无提交" :icon="ClipboardCheck" />
        </aside>

        <!-- Detail panel -->
        <section v-if="selectedSub" class="rounded-xl border border-border bg-white p-5 shadow-sm">
          <div class="mb-5 flex items-center justify-between">
            <div>
              <p class="text-xs text-primary-600">{{ selectedSub.studentName || `学生 #${selectedSub.studentId}` }}</p>
              <h2 class="font-semibold text-neutral-900">答题详情</h2>
            </div>
            <div class="rounded-xl bg-primary-600 px-5 py-3 text-white">
              <p class="text-xs opacity-75">总分</p>
              <p class="text-2xl font-bold">{{ selectedSub.totalScore }}</p>
            </div>
          </div>

          <div class="space-y-4">
            <article v-for="(answer, index) in selectedSub.answers" :key="answer.id" class="rounded-lg border border-border bg-neutral-100 p-4">
              <p class="text-xs text-neutral-400">第 {{ index + 1 }} 题 · {{ answer.questionType }} · {{ answer.questionScore }} 分</p>
              <h3 class="mt-1.5 whitespace-pre-wrap font-medium text-neutral-900">{{ answer.questionContent }}</h3>
              <div class="mt-3 grid gap-3 md:grid-cols-2">
                <div class="rounded-lg bg-white p-3 text-sm">
                  <p class="text-xs text-neutral-400">学生答案</p>
                  <p class="mt-1 whitespace-pre-wrap text-neutral-700">{{ answer.answer || '未作答' }}</p>
                </div>
                <div class="rounded-lg bg-white p-3 text-sm">
                  <p class="text-xs text-neutral-400">参考答案</p>
                  <p class="mt-1 whitespace-pre-wrap text-neutral-700">{{ answer.correctAnswer || '简答题' }}</p>
                </div>
              </div>
              <div v-if="answer.questionType === 'SHORT'" class="mt-3 grid gap-2 md:grid-cols-[120px_1fr]">
                <input v-model.number="grades[answer.id].score" type="number" min="0" :max="answer.questionScore" step="0.1" placeholder="分数" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400" />
                <input v-model="grades[answer.id].comment" placeholder="题目评语" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
              </div>
              <p v-else class="mt-2 text-xs" :class="answer.correct ? 'text-emerald-600' : 'text-red-600'">
                自动判题：{{ answer.correct ? '正确' : '错误' }} · {{ answer.score }} 分
              </p>
            </article>
          </div>

          <div class="mt-5 rounded-xl border border-border p-4">
            <div class="mb-3 flex items-center gap-2 text-sm font-medium text-neutral-700">
              <Sparkles class="h-4 w-4 text-primary-600" />总评语
            </div>
            <textarea v-model="comment" rows="3" placeholder="输入教师总评语" class="w-full rounded-lg border border-border bg-white px-3 py-2 text-sm outline-none transition placeholder:text-neutral-400 focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
            <button class="mt-3 inline-flex items-center gap-2 rounded-lg bg-primary-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700" @click="gradeSubmission">
              <Save class="h-4 w-4" />保存阅卷
            </button>
          </div>
        </section>

        <section v-else class="rounded-xl border border-dashed border-border p-10 text-center text-sm text-neutral-400">
          请从左侧选择一份答卷查看详情
        </section>
      </div>
  </div>
</template>
