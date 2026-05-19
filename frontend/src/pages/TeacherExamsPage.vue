<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ClipboardList, FileQuestion, Plus, Save, Send, Trash2, XCircle } from 'lucide-vue-next'
import { useAuth } from '@/composables/useAuth'
import { courseApi, examApi, type Course, type Exam } from '@/lib/api'

const auth = useAuth()
const exams = ref<Exam[]>([])
const courses = ref<Course[]>([])
const form = reactive({ courseId: 0, title: '', description: '', startTime: '', endTime: '', durationMinutes: 60 })
const editForms = reactive<Record<number, { courseId: number; title: string; description: string; startTime: string; endTime: string; durationMinutes?: number }>>({})
const message = ref('')
const error = ref('')

function toInputTime(value?: string) {
  return value ? value.replace(' ', 'T').slice(0, 16) : ''
}

function toPayloadTime(value: string) {
  return value ? (value.length === 16 ? `${value}:00` : value) : undefined
}

async function load() {
  error.value = ''
  try {
    if (!auth.state.user) await auth.init()
    const [examPage, coursePage] = await Promise.all([
      examApi.mine({ current: 1, size: 50 }),
      courseApi.page({ current: 1, size: 100, teacherId: auth.state.user?.id }),
    ])
    exams.value = examPage.records
    courses.value = coursePage.records.filter((course) => course.teacherId === auth.state.user?.id)
    if (!form.courseId && courses.value[0]) form.courseId = courses.value[0].id
    exams.value.forEach((exam) => {
      editForms[exam.id] = { courseId: exam.courseId, title: exam.title, description: exam.description || '', startTime: toInputTime(exam.startTime), endTime: toInputTime(exam.endTime), durationMinutes: exam.durationMinutes }
    })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  }
}

async function createExam() {
  message.value = ''
  error.value = ''
  try {
    await examApi.create({ courseId: form.courseId, title: form.title, description: form.description, startTime: toPayloadTime(form.startTime), endTime: toPayloadTime(form.endTime), durationMinutes: form.durationMinutes })
    form.title = ''
    form.description = ''
    form.startTime = ''
    form.endTime = ''
    message.value = '考试已创建'
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '创建失败'
  }
}

async function saveExam(exam: Exam) {
  const item = editForms[exam.id]
  await examApi.update(exam.id, { courseId: item.courseId, title: item.title, description: item.description, startTime: toPayloadTime(item.startTime), endTime: toPayloadTime(item.endTime), durationMinutes: item.durationMinutes })
  message.value = '考试已保存'
  await load()
}

async function removeExam(id: number) {
  await examApi.remove(id)
  message.value = '考试已删除'
  await load()
}

async function publishExam(id: number) {
  await examApi.publish(id)
  message.value = '考试已发布'
  await load()
}

async function closeExam(id: number) {
  await examApi.close(id)
  message.value = '考试已关闭'
  await load()
}

onMounted(load)
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/dashboard" class="text-sm text-cyan-200">返回工作台</RouterLink>
        <RouterLink to="/exams" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">学生考试中心</RouterLink>
      </nav>
      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">教师考试管理</p>
        <h1 class="mt-2 text-4xl font-black">创建、发布与关闭课程考试</h1>
        <p v-if="message" class="mt-4 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
        <form class="mt-8 grid gap-4 lg:grid-cols-[170px_1fr_1fr_190px_190px_130px_110px]" @submit.prevent="createExam">
          <select v-model.number="form.courseId" required class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
            <option v-for="course in courses" :key="course.id" :value="course.id">{{ course.title }}</option>
          </select>
          <input v-model="form.title" required class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="考试标题" />
          <input v-model="form.description" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="考试说明" />
          <input v-model="form.startTime" type="datetime-local" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
          <input v-model="form.endTime" type="datetime-local" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
          <input v-model.number="form.durationMinutes" type="number" min="1" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="分钟" />
          <button class="inline-flex items-center justify-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200"><Plus class="h-4 w-4" />创建</button>
        </form>
      </section>

      <section class="mt-8 space-y-5">
        <article v-for="exam in exams" :key="exam.id" class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p class="text-sm text-cyan-200">{{ exam.courseTitle || `课程 #${exam.courseId}` }} · {{ exam.status }}</p>
              <h2 class="mt-2 text-2xl font-black">{{ exam.title }}</h2>
              <p class="mt-2 text-sm text-slate-300">总分：{{ exam.totalScore }} · 截止：{{ exam.endTime || '不限' }}</p>
            </div>
            <div class="flex flex-wrap gap-3">
              <RouterLink :to="`/teacher/exams/${exam.id}/questions`" class="inline-flex items-center gap-2 rounded-full bg-cyan-300 px-5 py-2 text-sm font-bold text-slate-950"><FileQuestion class="h-4 w-4" />题目</RouterLink>
              <RouterLink :to="`/teacher/exams/${exam.id}/submissions`" class="inline-flex items-center gap-2 rounded-full bg-white px-5 py-2 text-sm font-bold text-slate-950"><ClipboardList class="h-4 w-4" />阅卷</RouterLink>
              <button v-if="exam.status !== 'PUBLISHED'" class="inline-flex items-center gap-2 rounded-full border border-cyan-300/30 px-5 py-2 text-sm text-cyan-100" @click="publishExam(exam.id)"><Send class="h-4 w-4" />发布</button>
              <button v-if="exam.status === 'PUBLISHED'" class="inline-flex items-center gap-2 rounded-full border border-amber-300/30 px-5 py-2 text-sm text-amber-100" @click="closeExam(exam.id)"><XCircle class="h-4 w-4" />关闭</button>
              <button class="inline-flex items-center gap-2 rounded-full border border-red-300/30 px-5 py-2 text-sm text-red-100" @click="removeExam(exam.id)"><Trash2 class="h-4 w-4" />删除</button>
            </div>
          </div>
          <div class="mt-6 grid gap-3 lg:grid-cols-[170px_1fr_1fr_190px_190px_130px_110px]">
            <select v-model.number="editForms[exam.id].courseId" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
              <option v-for="course in courses" :key="course.id" :value="course.id">{{ course.title }}</option>
            </select>
            <input v-model="editForms[exam.id].title" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
            <input v-model="editForms[exam.id].description" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
            <input v-model="editForms[exam.id].startTime" type="datetime-local" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
            <input v-model="editForms[exam.id].endTime" type="datetime-local" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
            <input v-model.number="editForms[exam.id].durationMinutes" type="number" min="1" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
            <button class="inline-flex items-center justify-center gap-2 rounded-2xl bg-white px-4 py-3 font-bold text-slate-950" @click="saveExam(exam)"><Save class="h-4 w-4" />保存</button>
          </div>
        </article>
      </section>
    </div>
  </main>
</template>
