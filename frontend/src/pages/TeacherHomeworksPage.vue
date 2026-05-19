<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { BookOpenCheck, ClipboardList, Plus, Save, Trash2 } from 'lucide-vue-next'
import { courseApi, homeworkApi, type Course, type Homework } from '@/lib/api'

const homeworks = ref<Homework[]>([])
const courses = ref<Course[]>([])
const form = reactive({ courseId: 0, title: '', description: '', deadline: '' })
const editForms = reactive<Record<number, { courseId: number; title: string; description: string; deadline: string }>>({})
const message = ref('')
const error = ref('')

function toInputTime(value?: string) {
  return value ? value.replace(' ', 'T').slice(0, 16) : ''
}

function toPayloadTime(value: string) {
  return value.length === 16 ? `${value}:00` : value
}

async function load() {
  error.value = ''
  try {
    const [homeworkPage, coursePage] = await Promise.all([
      homeworkApi.mine({ current: 1, size: 50 }),
      courseApi.page({ current: 1, size: 100 }),
    ])
    homeworks.value = homeworkPage.records
    courses.value = coursePage.records
    if (!form.courseId && courses.value[0]) form.courseId = courses.value[0].id
    homeworks.value.forEach((homework) => {
      editForms[homework.id] = { courseId: homework.courseId, title: homework.title, description: homework.description || '', deadline: toInputTime(homework.deadline) }
    })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  }
}

async function createHomework() {
  message.value = ''
  error.value = ''
  try {
    await homeworkApi.create({ courseId: form.courseId, title: form.title, description: form.description, deadline: toPayloadTime(form.deadline) })
    form.title = ''
    form.description = ''
    form.deadline = ''
    message.value = '作业已创建'
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '创建失败'
  }
}

async function saveHomework(homework: Homework) {
  const item = editForms[homework.id]
  await homeworkApi.update(homework.id, { courseId: item.courseId, title: item.title, description: item.description, deadline: toPayloadTime(item.deadline) })
  message.value = '作业已保存'
  await load()
}

async function removeHomework(id: number) {
  await homeworkApi.remove(id)
  message.value = '作业已删除'
  await load()
}

onMounted(load)
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/dashboard" class="text-sm text-cyan-200">返回工作台</RouterLink>
        <RouterLink to="/homeworks" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">学生作业中心</RouterLink>
      </nav>
      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">教师作业管理</p>
        <h1 class="mt-2 text-4xl font-black">创建、编辑与发布课程作业</h1>
        <p v-if="message" class="mt-4 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
        <form class="mt-8 grid gap-4 lg:grid-cols-[180px_1fr_1fr_220px_120px]" @submit.prevent="createHomework">
          <select v-model.number="form.courseId" required class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
            <option v-for="course in courses" :key="course.id" :value="course.id">{{ course.title }}</option>
          </select>
          <input v-model="form.title" required class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="作业标题" />
          <input v-model="form.description" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="作业说明" />
          <input v-model="form.deadline" required type="datetime-local" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
          <button class="inline-flex items-center justify-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200"><Plus class="h-4 w-4" />创建</button>
        </form>
      </section>

      <section class="mt-8 space-y-5">
        <article v-for="homework in homeworks" :key="homework.id" class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p class="text-sm text-cyan-200">{{ homework.courseTitle || `课程 #${homework.courseId}` }}</p>
              <h2 class="mt-2 text-2xl font-black">{{ homework.title }}</h2>
              <p class="mt-2 text-sm text-slate-300">截止：{{ homework.deadline }}</p>
            </div>
            <div class="flex flex-wrap gap-3">
              <RouterLink :to="`/teacher/homeworks/${homework.id}/submissions`" class="inline-flex items-center gap-2 rounded-full bg-cyan-300 px-5 py-2 text-sm font-bold text-slate-950"><ClipboardList class="h-4 w-4" />提交列表</RouterLink>
              <button class="inline-flex items-center gap-2 rounded-full border border-red-300/30 px-5 py-2 text-sm text-red-100" @click="removeHomework(homework.id)"><Trash2 class="h-4 w-4" />删除</button>
            </div>
          </div>
          <div class="mt-6 grid gap-3 lg:grid-cols-[180px_1fr_1fr_220px_120px]">
            <select v-model.number="editForms[homework.id].courseId" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
              <option v-for="course in courses" :key="course.id" :value="course.id">{{ course.title }}</option>
            </select>
            <input v-model="editForms[homework.id].title" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
            <input v-model="editForms[homework.id].description" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
            <input v-model="editForms[homework.id].deadline" type="datetime-local" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
            <button class="inline-flex items-center justify-center gap-2 rounded-2xl bg-white px-4 py-3 font-bold text-slate-950" @click="saveHomework(homework)"><Save class="h-4 w-4" />保存</button>
          </div>
        </article>
      </section>

      <div v-if="homeworks.length === 0" class="mt-12 rounded-[2rem] border border-white/10 p-10 text-center text-slate-300">
        <BookOpenCheck class="mx-auto mb-4 h-10 w-10 text-cyan-200" />
        暂无作业
      </div>
    </div>
  </main>
</template>
