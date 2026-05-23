<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { FileQuestion, Pencil, Plus, Trash2, X } from 'lucide-vue-next'
import { examApi, type Exam, type ExamQuestion } from '@/lib/api'
import Empty from '@/components/Empty.vue'

const route = useRoute()
const exam = ref<Exam | null>(null)
const questions = ref<ExamQuestion[]>([])
const message = ref('')
const error = ref('')
const examId = computed(() => Number(route.params.id))

const modalOpen = ref(false)
const editingQuestion = ref<ExamQuestion | null>(null)
const modalForm = reactive({ type: 'SINGLE', content: '', optionsText: '', answer: '', score: 5, sort: 0 })

function optionsFromText(text: string) { return text.split('\n').map((item) => item.trim()).filter(Boolean) }
function toPayload(item: typeof modalForm) {
  return { type: item.type, content: item.content, options: item.type === 'SHORT' ? [] : optionsFromText(item.optionsText), answer: item.answer, score: item.score, sort: item.sort }
}

async function load() {
  error.value = ''
  try {
    exam.value = await examApi.detail(examId.value)
    questions.value = await examApi.questions(examId.value)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  }
}

function openCreateModal() {
  editingQuestion.value = null
  modalForm.type = 'SINGLE'
  modalForm.content = ''
  modalForm.optionsText = ''
  modalForm.answer = ''
  modalForm.score = 5
  modalForm.sort = (questions.value.length) + 1
  modalOpen.value = true
}

function openEditModal(question: ExamQuestion) {
  editingQuestion.value = question
  modalForm.type = question.type
  modalForm.content = question.content
  modalForm.optionsText = question.options.join('\n')
  modalForm.answer = question.answer || ''
  modalForm.score = question.score
  modalForm.sort = question.sort
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
  editingQuestion.value = null
}

async function submitModal() {
  message.value = ''; error.value = ''
  try {
    if (editingQuestion.value) {
      await examApi.updateQuestion(examId.value, editingQuestion.value.id, toPayload(modalForm))
      message.value = '题目已保存'
    } else {
      await examApi.createQuestion(examId.value, toPayload(modalForm))
      message.value = '题目已添加'
    }
    closeModal()
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '操作失败'
  }
}

async function removeQuestion(questionId: number) {
  await examApi.removeQuestion(examId.value, questionId)
  message.value = '题目已删除'; await load()
}

onMounted(load)
</script>

<template>
  <div class="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <p class="text-sm font-medium text-primary-600">题目管理</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">{{ exam?.title || '考试题目' }}</h1>
        <p class="mt-1 text-sm text-neutral-400">支持单选、多选、判断与简答题；客观题答案按选项文本匹配。</p>
      </div>
      <div class="flex items-center gap-2">
        <RouterLink v-if="exam" :to="`/teacher/exams/${exam.id}/submissions`" class="rounded-lg border border-border px-3 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-100">
          查看提交
        </RouterLink>
        <button
          class="inline-flex items-center gap-2 rounded-xl bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700"
          @click="openCreateModal"
        >
          <Plus class="h-4 w-4" />添加题目
        </button>
      </div>
    </div>

    <div v-if="message" class="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</div>
    <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

    <div class="space-y-4">
      <article v-for="(question, idx) in questions" :key="question.id" class="rounded-xl border border-border bg-white p-5 shadow-sm">
        <div class="flex items-start justify-between gap-3">
          <div class="flex-1">
            <div class="mb-2 flex items-center gap-2">
              <span class="inline-flex items-center gap-1.5 rounded-full bg-neutral-100 px-2.5 py-0.5 text-xs text-neutral-500">
                <FileQuestion class="h-3.5 w-3.5" />第 {{ idx + 1 }} 题 · {{ question.type }} · {{ question.score }} 分
              </span>
            </div>
            <p class="font-medium text-neutral-900">{{ question.content }}</p>
            <div v-if="question.options.length" class="mt-2 space-y-1">
              <p v-for="(opt, i) in question.options" :key="i" class="text-sm text-neutral-600">{{ String.fromCharCode(65 + i)}}.  {{ opt }}</p>
            </div>
            <p class="mt-2 text-xs text-neutral-400">答案：{{ question.answer }}</p>
          </div>
          <div class="flex shrink-0 gap-2">
            <button class="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-neutral-700 hover:bg-neutral-100" @click="openEditModal(question)">
              <Pencil class="h-3.5 w-3.5" />编辑
            </button>
            <button class="inline-flex items-center gap-1.5 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50" @click="removeQuestion(question.id)">
              <Trash2 class="h-3.5 w-3.5" />删除
            </button>
          </div>
        </div>
      </article>
    </div>

    <Empty v-if="questions.length === 0" title="暂无题目，点击右上角添加第一道题" :icon="FileQuestion" />

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="modalOpen" class="fixed inset-0 z-[70] flex items-center justify-center p-4" @keydown.esc="closeModal">
          <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="closeModal" />
          <div class="modal-panel relative w-full max-w-lg rounded-2xl border border-border bg-white shadow-2xl">
            <div class="flex items-center justify-between border-b border-border px-6 py-4">
              <h2 class="text-base font-semibold text-neutral-900">{{ editingQuestion ? '编辑题目' : '添加题目' }}</h2>
              <button class="rounded-lg p-1.5 text-neutral-400 transition hover:bg-neutral-100" @click="closeModal"><X class="h-4 w-4" /></button>
            </div>
            <form class="space-y-4 p-6" @submit.prevent="submitModal">
              <div class="grid gap-3 sm:grid-cols-[2fr_1fr_1fr]">
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">题型</label>
                  <select v-model="modalForm.type" class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600">
                    <option value="SINGLE">单选题</option>
                    <option value="MULTIPLE">多选题</option>
                    <option value="JUDGE">判断题</option>
                    <option value="SHORT">简答题</option>
                  </select>
                </div>
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">分值 <span class="text-danger">*</span></label>
                  <input v-model.number="modalForm.score" required type="number" min="0.1" step="0.1" class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600" />
                </div>
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">排序</label>
                  <input v-model.number="modalForm.sort" type="number" min="0" class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600" />
                </div>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-neutral-700">题干 <span class="text-danger">*</span></label>
                <input v-model="modalForm.content" required placeholder="请输入题干内容" class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15" />
              </div>
              <div class="grid gap-3 sm:grid-cols-2">
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">选项（每行一个）</label>
                  <textarea v-model="modalForm.optionsText" rows="4" placeholder="选项，每行一个；判断题可填：正确/错误" class="w-full resize-none rounded-md border border-border bg-white px-3 py-2 text-sm outline-none transition placeholder:text-neutral-400 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15" />
                </div>
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">答案 <span class="text-danger">*</span></label>
                  <textarea v-model="modalForm.answer" required rows="4" placeholder="答案；多选用英文逗号分隔" class="w-full resize-none rounded-md border border-border bg-white px-3 py-2 text-sm outline-none transition placeholder:text-neutral-400 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15" />
                </div>
              </div>
              <div class="flex justify-end gap-3 pt-2">
                <button type="button" class="rounded-md border border-border px-4 py-2 text-sm font-medium text-neutral-700 transition hover:bg-neutral-50" @click="closeModal">取消</button>
                <button type="submit" class="rounded-md bg-primary-600 px-5 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700">{{ editingQuestion ? '保存' : '添加' }}</button>
              </div>
            </form>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-active .modal-panel, .modal-leave-active .modal-panel { transition: transform 0.2s ease, opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .modal-panel, .modal-leave-to .modal-panel { transform: translateY(12px) scale(0.97); opacity: 0; }
</style>
