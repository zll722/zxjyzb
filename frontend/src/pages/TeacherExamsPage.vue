<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ClipboardList, FileQuestion, Pencil, Plus, Send, Trash2, X, XCircle } from 'lucide-vue-next'
import { useAuth } from '@/composables/useAuth'
import { courseApi, examApi, type Course, type Exam } from '@/lib/api'
import Empty from '@/components/Empty.vue'

const auth = useAuth()
const exams = ref<Exam[]>([])
const courses = ref<Course[]>([])
const message = ref('')
const error = ref('')

const modalOpen = ref(false)
const editingExam = ref<Exam | null>(null)
const modalForm = reactive({ courseId: 0, title: '', description: '', startTime: '', endTime: '', durationMinutes: 60 })

function toInputTime(value?: string) { return value ? value.replace(' ', 'T').slice(0, 16) : '' }
function toPayloadTime(value: string) { return value ? (value.length === 16 ? `${value}:00` : value) : undefined }

async function load() {
  error.value = ''
  try {
    if (!auth.state.user) await auth.init()
    const [examPage, coursePage] = await Promise.all([
      examApi.mine({ current: 1, size: 50 }),
      courseApi.page({ current: 1, size: 100, teacherId: auth.state.user?.id }),
    ])
    exams.value = examPage.records
    courses.value = coursePage.records.filter((c) => c.teacherId === auth.state.user?.id)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  }
}

function openCreateModal() {
  editingExam.value = null
  modalForm.courseId = courses.value[0]?.id ?? 0
  modalForm.title = ''
  modalForm.description = ''
  modalForm.startTime = ''
  modalForm.endTime = ''
  modalForm.durationMinutes = 60
  modalOpen.value = true
}

function openEditModal(exam: Exam) {
  editingExam.value = exam
  modalForm.courseId = exam.courseId
  modalForm.title = exam.title
  modalForm.description = exam.description || ''
  modalForm.startTime = toInputTime(exam.startTime)
  modalForm.endTime = toInputTime(exam.endTime)
  modalForm.durationMinutes = exam.durationMinutes ?? 60
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
  editingExam.value = null
}

async function submitModal() {
  message.value = ''; error.value = ''
  try {
    const payload = {
      courseId: modalForm.courseId,
      title: modalForm.title,
      description: modalForm.description,
      startTime: toPayloadTime(modalForm.startTime),
      endTime: toPayloadTime(modalForm.endTime),
      durationMinutes: modalForm.durationMinutes,
    }
    if (editingExam.value) {
      await examApi.update(editingExam.value.id, payload)
      message.value = '考试已保存'
    } else {
      await examApi.create(payload)
      message.value = '考试已创建'
    }
    closeModal()
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '操作失败'
  }
}

async function removeExam(id: number) { await examApi.remove(id); message.value = '考试已删除'; await load() }
async function publishExam(id: number) { await examApi.publish(id); message.value = '考试已发布'; await load() }
async function closeExam(id: number) { await examApi.close(id); message.value = '考试已关闭'; await load() }

function statusTone(status: string) {
  if (status === 'PUBLISHED') return 'bg-success-bg text-success'
  if (status === 'CLOSED') return 'bg-neutral-100 text-neutral-500'
  return 'bg-warning-bg text-warning'
}

onMounted(load)
</script>

<template>
  <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <p class="text-sm font-medium text-primary-600">教师考试管理</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">创建、发布与关闭课程考试</h1>
      </div>
      <button
        class="inline-flex items-center gap-2 rounded-xl bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700"
        @click="openCreateModal"
      >
        <Plus class="h-4 w-4" />新增考试
      </button>
    </div>

    <div v-if="message" class="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</div>
    <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

    <div class="space-y-4">
      <article v-for="exam in exams" :key="exam.id" class="rounded-xl border border-border bg-white p-5 shadow-sm">
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <span class="rounded-full px-2.5 py-0.5 text-xs font-medium" :class="statusTone(exam.status)">{{ exam.status }}</span>
              <span class="text-xs text-primary-600">{{ exam.courseTitle || `课程 #${exam.courseId}` }}</span>
            </div>
            <h2 class="mt-1.5 font-semibold text-neutral-900">{{ exam.title }}</h2>
            <p class="text-xs text-neutral-400">总分：{{ exam.totalScore }} · 截止：{{ exam.endTime || '不限' }}</p>
            <p v-if="exam.description" class="mt-1 text-sm text-neutral-500">{{ exam.description }}</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <button
              class="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-neutral-700 hover:bg-neutral-100"
              @click="openEditModal(exam)"
            >
              <Pencil class="h-3.5 w-3.5" />编辑
            </button>
            <RouterLink :to="`/teacher/exams/${exam.id}/questions`" class="inline-flex items-center gap-1.5 rounded-lg bg-primary-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm hover:bg-primary-700">
              <FileQuestion class="h-3.5 w-3.5" />题目
            </RouterLink>
            <RouterLink :to="`/teacher/exams/${exam.id}/submissions`" class="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-neutral-700 hover:bg-neutral-100">
              <ClipboardList class="h-3.5 w-3.5" />阅卷
            </RouterLink>
            <button v-if="exam.status !== 'PUBLISHED'" class="inline-flex items-center gap-1.5 rounded-lg border border-emerald-200 px-3 py-1.5 text-xs font-medium text-emerald-700 hover:bg-emerald-50" @click="publishExam(exam.id)">
              <Send class="h-3.5 w-3.5" />发布
            </button>
            <button v-if="exam.status === 'PUBLISHED'" class="inline-flex items-center gap-1.5 rounded-lg border border-amber-200 px-3 py-1.5 text-xs font-medium text-amber-700 hover:bg-amber-50" @click="closeExam(exam.id)">
              <XCircle class="h-3.5 w-3.5" />关闭
            </button>
            <button class="inline-flex items-center gap-1.5 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50" @click="removeExam(exam.id)">
              <Trash2 class="h-3.5 w-3.5" />删除
            </button>
          </div>
        </div>
      </article>
    </div>

    <Empty v-if="exams.length === 0" title="暂无考试" :icon="FileQuestion" />

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="modalOpen" class="fixed inset-0 z-[70] flex items-center justify-center p-4" @keydown.esc="closeModal">
          <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="closeModal" />
          <div class="modal-panel relative w-full max-w-lg rounded-2xl border border-border bg-white shadow-2xl">
            <div class="flex items-center justify-between border-b border-border px-6 py-4">
              <h2 class="text-base font-semibold text-neutral-900">{{ editingExam ? '编辑考试' : '新增考试' }}</h2>
              <button class="rounded-lg p-1.5 text-neutral-400 transition hover:bg-neutral-100" @click="closeModal"><X class="h-4 w-4" /></button>
            </div>
            <form class="space-y-4 p-6" @submit.prevent="submitModal">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-neutral-700">所属课程</label>
                <select v-model.number="modalForm.courseId" required class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600">
                  <option v-for="course in courses" :key="course.id" :value="course.id">{{ course.title }}</option>
                </select>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-neutral-700">考试标题 <span class="text-danger">*</span></label>
                <input v-model="modalForm.title" required placeholder="请输入考试标题" class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15" />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-neutral-700">考试说明</label>
                <input v-model="modalForm.description" placeholder="考试说明（可选）" class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15" />
              </div>
              <div class="grid gap-3 sm:grid-cols-2">
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">开始时间</label>
                  <input v-model="modalForm.startTime" type="datetime-local" class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600" />
                </div>
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">结束时间</label>
                  <input v-model="modalForm.endTime" type="datetime-local" class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600" />
                </div>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-neutral-700">时长（分钟）</label>
                <input v-model.number="modalForm.durationMinutes" type="number" min="1" placeholder="考试时长（分钟）" class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600" />
              </div>
              <div class="flex justify-end gap-3 pt-2">
                <button type="button" class="rounded-md border border-border px-4 py-2 text-sm font-medium text-neutral-700 transition hover:bg-neutral-50" @click="closeModal">取消</button>
                <button type="submit" class="rounded-md bg-primary-600 px-5 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700">{{ editingExam ? '保存' : '创建' }}</button>
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
