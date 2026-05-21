<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ClipboardList, FileQuestion, Plus, Save, Send, Trash2, XCircle } from 'lucide-vue-next'
import { useAuth } from '@/composables/useAuth'
import { courseApi, examApi, type Course, type Exam } from '@/lib/api'
import Empty from '@/components/Empty.vue'

const auth = useAuth()
const exams = ref<Exam[]>([])
const courses = ref<Course[]>([])
const form = reactive({ courseId: 0, title: '', description: '', startTime: '', endTime: '', durationMinutes: 60 })
const editForms = reactive<Record<number, { courseId: number; title: string; description: string; startTime: string; endTime: string; durationMinutes?: number }>>({})
const message = ref('')
const error = ref('')

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
    if (!form.courseId && courses.value[0]) form.courseId = courses.value[0].id
    exams.value.forEach((exam) => {
      editForms[exam.id] = { courseId: exam.courseId, title: exam.title, description: exam.description || '', startTime: toInputTime(exam.startTime), endTime: toInputTime(exam.endTime), durationMinutes: exam.durationMinutes }
    })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  }
}

async function createExam() {
  message.value = ''; error.value = ''
  try {
    await examApi.create({ courseId: form.courseId, title: form.title, description: form.description, startTime: toPayloadTime(form.startTime), endTime: toPayloadTime(form.endTime), durationMinutes: form.durationMinutes })
    form.title = ''; form.description = ''; form.startTime = ''; form.endTime = ''
    message.value = '考试已创建'; await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '创建失败'
  }
}

async function saveExam(exam: Exam) {
  const item = editForms[exam.id]
  await examApi.update(exam.id, { courseId: item.courseId, title: item.title, description: item.description, startTime: toPayloadTime(item.startTime), endTime: toPayloadTime(item.endTime), durationMinutes: item.durationMinutes })
  message.value = '考试已保存'; await load()
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
      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">教师考试管理</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">创建、发布与关闭课程考试</h1>
      </div>

      <!-- Create form -->
      <div class="mb-8 rounded-xl border border-border bg-white p-5 shadow-sm">
        <h2 class="mb-4 font-semibold text-neutral-900">新增考试</h2>
        <div v-if="message" class="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</div>
        <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>
        <form class="grid gap-3 md:grid-cols-2 lg:grid-cols-4" @submit.prevent="createExam">
          <select v-model.number="form.courseId" required class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400">
            <option v-for="course in courses" :key="course.id" :value="course.id">{{ course.title }}</option>
          </select>
          <input v-model="form.title" required placeholder="考试标题" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
          <input v-model="form.startTime" type="datetime-local" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400" />
          <input v-model="form.endTime" type="datetime-local" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400" />
          <input v-model="form.description" placeholder="考试说明（可选）" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100 md:col-span-2" />
          <div class="flex items-center gap-2">
            <input v-model.number="form.durationMinutes" type="number" min="1" placeholder="时长(分钟)" class="h-9 flex-1 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400" />
            <button type="submit" class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700">
              <Plus class="h-4 w-4" />创建
            </button>
          </div>
        </form>
      </div>

      <!-- Exam list -->
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
            </div>
            <div class="flex flex-wrap gap-2">
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
          <div class="mt-4 grid gap-2 border-t border-border pt-4 md:grid-cols-2 lg:grid-cols-4">
            <select v-model.number="editForms[exam.id].courseId" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400">
              <option v-for="course in courses" :key="course.id" :value="course.id">{{ course.title }}</option>
            </select>
            <input v-model="editForms[exam.id].title" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
            <input v-model="editForms[exam.id].startTime" type="datetime-local" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400" />
            <div class="flex gap-2">
              <input v-model="editForms[exam.id].endTime" type="datetime-local" class="h-9 flex-1 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400" />
              <button class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-border px-3 text-sm font-medium text-neutral-700 hover:bg-neutral-100" @click="saveExam(exam)">
                <Save class="h-4 w-4" />保存
              </button>
            </div>
          </div>
        </article>
      </div>

      <Empty v-if="exams.length === 0" title="暂无考试" :icon="FileQuestion" />
  </div>
</template>
