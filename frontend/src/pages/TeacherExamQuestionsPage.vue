<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { FileQuestion, Plus, Save, Trash2 } from 'lucide-vue-next'
import { examApi, type Exam, type ExamQuestion } from '@/lib/api'
import Empty from '@/components/Empty.vue'

const route = useRoute()
const exam = ref<Exam | null>(null)
const questions = ref<ExamQuestion[]>([])
const form = reactive({ type: 'SINGLE', content: '', optionsText: '', answer: '', score: 5, sort: 0 })
const editForms = reactive<Record<number, { type: string; content: string; optionsText: string; answer: string; score: number; sort: number }>>({})
const message = ref('')
const error = ref('')
const examId = computed(() => Number(route.params.id))

function optionsFromText(text: string) { return text.split('\n').map((item) => item.trim()).filter(Boolean) }
function toPayload(item: typeof form) {
  return { type: item.type, content: item.content, options: item.type === 'SHORT' ? [] : optionsFromText(item.optionsText), answer: item.answer, score: item.score, sort: item.sort }
}

async function load() {
  error.value = ''
  try {
    exam.value = await examApi.detail(examId.value)
    questions.value = await examApi.questions(examId.value)
    questions.value.forEach((question) => {
      editForms[question.id] = { type: question.type, content: question.content, optionsText: question.options.join('\n'), answer: question.answer || '', score: question.score, sort: question.sort }
    })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  }
}

async function createQuestion() {
  message.value = ''; error.value = ''
  try {
    await examApi.createQuestion(examId.value, toPayload(form))
    form.content = ''; form.optionsText = ''; form.answer = ''; form.sort += 1
    message.value = '题目已创建'; await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '创建失败'
  }
}

async function saveQuestion(question: ExamQuestion) {
  await examApi.updateQuestion(examId.value, question.id, toPayload(editForms[question.id] as typeof form))
  message.value = '题目已保存'; await load()
}

async function removeQuestion(questionId: number) {
  await examApi.removeQuestion(examId.value, questionId)
  message.value = '题目已删除'; await load()
}

onMounted(load)
</script>

<template>
  <div class="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-6 flex items-center justify-end">
        <RouterLink v-if="exam" :to="`/teacher/exams/${exam.id}/submissions`" class="rounded-lg border border-border px-3 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-100">
          查看提交
        </RouterLink>
      </div>

      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">题目管理</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">{{ exam?.title || '考试题目' }}</h1>
        <p class="mt-1 text-sm text-neutral-400">支持单选、多选、判断与简答题；客观题答案按选项文本匹配。</p>
      </div>

      <div v-if="message" class="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</div>
      <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

      <!-- Add question form -->
      <form class="mb-8 rounded-xl border border-border bg-white p-5 shadow-sm" @submit.prevent="createQuestion">
        <h2 class="mb-4 font-semibold text-neutral-900">添加题目</h2>
        <div class="grid gap-3 md:grid-cols-[140px_1fr_100px_80px]">
          <select v-model="form.type" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400">
            <option value="SINGLE">单选题</option>
            <option value="MULTIPLE">多选题</option>
            <option value="JUDGE">判断题</option>
            <option value="SHORT">简答题</option>
          </select>
          <input v-model="form.content" required placeholder="题干内容" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
          <input v-model.number="form.score" required type="number" min="0.1" step="0.1" placeholder="分值" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400" />
          <input v-model.number="form.sort" type="number" min="0" placeholder="排序" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400" />
        </div>
        <div class="mt-3 grid gap-3 md:grid-cols-2">
          <div>
            <label class="block text-xs font-medium text-neutral-500 mb-1">选项（每行一个）</label>
            <textarea v-model="form.optionsText" rows="4" placeholder="选项，每行一个；判断题可填：正确/错误" class="w-full rounded-lg border border-border bg-white px-3 py-2 text-sm outline-none transition placeholder:text-neutral-400 focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
          </div>
          <div>
            <label class="block text-xs font-medium text-neutral-500 mb-1">答案</label>
            <textarea v-model="form.answer" required rows="4" placeholder="答案；多选用英文逗号分隔" class="w-full rounded-lg border border-border bg-white px-3 py-2 text-sm outline-none transition placeholder:text-neutral-400 focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
          </div>
        </div>
        <button type="submit" class="mt-4 inline-flex items-center gap-1.5 rounded-lg bg-primary-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700">
          <Plus class="h-4 w-4" />添加题目
        </button>
      </form>

      <!-- Question list -->
      <div class="space-y-4">
        <article v-for="(question, idx) in questions" :key="question.id" class="rounded-xl border border-border bg-white p-5 shadow-sm">
          <div class="mb-3 flex items-center justify-between">
            <span class="inline-flex items-center gap-1.5 text-xs text-neutral-400">
              <FileQuestion class="h-3.5 w-3.5" />第 {{ idx + 1 }} 题 · {{ question.type }} · {{ question.score }} 分
            </span>
            <button class="inline-flex items-center gap-1.5 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50" @click="removeQuestion(question.id)">
              <Trash2 class="h-3.5 w-3.5" />删除
            </button>
          </div>
          <div class="grid gap-3 md:grid-cols-[140px_1fr_100px_80px]">
            <select v-model="editForms[question.id].type" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400">
              <option value="SINGLE">单选题</option>
              <option value="MULTIPLE">多选题</option>
              <option value="JUDGE">判断题</option>
              <option value="SHORT">简答题</option>
            </select>
            <input v-model="editForms[question.id].content" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
            <input v-model.number="editForms[question.id].score" type="number" min="0.1" step="0.1" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400" />
            <input v-model.number="editForms[question.id].sort" type="number" min="0" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400" />
          </div>
          <div class="mt-3 grid gap-3 md:grid-cols-2">
            <textarea v-model="editForms[question.id].optionsText" rows="3" class="w-full rounded-lg border border-border bg-white px-3 py-2 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
            <textarea v-model="editForms[question.id].answer" rows="3" class="w-full rounded-lg border border-border bg-white px-3 py-2 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
          </div>
          <button class="mt-3 inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-100" @click="saveQuestion(question)">
            <Save class="h-4 w-4" />保存题目
          </button>
        </article>
      </div>

      <Empty v-if="questions.length === 0" title="暂无题目，使用上方表单添加第一道题" :icon="FileQuestion" />
  </div>
</template>
