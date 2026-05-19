<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { FileQuestion, Plus, Save, Trash2 } from 'lucide-vue-next'
import { examApi, type Exam, type ExamQuestion } from '@/lib/api'

const route = useRoute()
const exam = ref<Exam | null>(null)
const questions = ref<ExamQuestion[]>([])
const form = reactive({ type: 'SINGLE', content: '', optionsText: '', answer: '', score: 5, sort: 0 })
const editForms = reactive<Record<number, { type: string; content: string; optionsText: string; answer: string; score: number; sort: number }>>({})
const message = ref('')
const error = ref('')
const examId = computed(() => Number(route.params.id))

function optionsFromText(text: string) {
  return text.split('\n').map((item) => item.trim()).filter(Boolean)
}

function toPayload(item: { type: string; content: string; optionsText: string; answer: string; score: number; sort: number }) {
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
  message.value = ''
  error.value = ''
  try {
    await examApi.createQuestion(examId.value, toPayload(form))
    form.content = ''
    form.optionsText = ''
    form.answer = ''
    form.sort += 1
    message.value = '题目已创建'
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '创建失败'
  }
}

async function saveQuestion(question: ExamQuestion) {
  await examApi.updateQuestion(examId.value, question.id, toPayload(editForms[question.id]))
  message.value = '题目已保存'
  await load()
}

async function removeQuestion(questionId: number) {
  await examApi.removeQuestion(examId.value, questionId)
  message.value = '题目已删除'
  await load()
}

onMounted(load)
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-6xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/teacher/exams" class="text-sm text-cyan-200">返回考试管理</RouterLink>
        <RouterLink v-if="exam" :to="`/teacher/exams/${exam.id}/submissions`" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">查看提交</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">题目管理</p>
        <h1 class="mt-2 text-4xl font-black">{{ exam?.title || '考试题目' }}</h1>
        <p class="mt-4 text-slate-300">支持单选、多选、判断与简答题；客观题答案按选项文本匹配。</p>
        <p v-if="message" class="mt-4 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <form class="mt-8 rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur" @submit.prevent="createQuestion">
        <div class="grid gap-4 md:grid-cols-[160px_1fr_120px_120px]">
          <select v-model="form.type" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
            <option value="SINGLE">单选题</option>
            <option value="MULTIPLE">多选题</option>
            <option value="JUDGE">判断题</option>
            <option value="SHORT">简答题</option>
          </select>
          <input v-model="form.content" required class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="题干" />
          <input v-model.number="form.score" required type="number" min="0.1" step="0.1" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="分值" />
          <input v-model.number="form.sort" type="number" min="0" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="排序" />
        </div>
        <div class="mt-4 grid gap-4 md:grid-cols-2">
          <textarea v-model="form.optionsText" rows="5" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="选项，每行一个；判断题可填：正确/错误" />
          <textarea v-model="form.answer" required rows="5" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="答案；多选用英文逗号分隔" />
        </div>
        <button class="mt-4 inline-flex items-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950"><Plus class="h-4 w-4" />添加题目</button>
      </form>

      <section class="mt-8 space-y-5">
        <article v-for="question in questions" :key="question.id" class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="flex items-center justify-between gap-4">
            <p class="inline-flex items-center gap-2 text-sm text-cyan-200"><FileQuestion class="h-4 w-4" />{{ question.type }} · {{ question.score }} 分</p>
            <button class="inline-flex items-center gap-2 rounded-full border border-red-300/30 px-5 py-2 text-sm text-red-100" @click="removeQuestion(question.id)"><Trash2 class="h-4 w-4" />删除</button>
          </div>
          <div class="mt-5 grid gap-4 md:grid-cols-[160px_1fr_120px_120px]">
            <select v-model="editForms[question.id].type" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
              <option value="SINGLE">单选题</option>
              <option value="MULTIPLE">多选题</option>
              <option value="JUDGE">判断题</option>
              <option value="SHORT">简答题</option>
            </select>
            <input v-model="editForms[question.id].content" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
            <input v-model.number="editForms[question.id].score" type="number" min="0.1" step="0.1" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
            <input v-model.number="editForms[question.id].sort" type="number" min="0" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
          </div>
          <div class="mt-4 grid gap-4 md:grid-cols-2">
            <textarea v-model="editForms[question.id].optionsText" rows="4" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
            <textarea v-model="editForms[question.id].answer" rows="4" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
          </div>
          <button class="mt-4 inline-flex items-center gap-2 rounded-2xl bg-white px-5 py-3 font-bold text-slate-950" @click="saveQuestion(question)"><Save class="h-4 w-4" />保存题目</button>
        </article>
      </section>
    </div>
  </main>
</template>
