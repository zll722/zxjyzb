<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { BookOpenCheck, ClipboardList, Plus, Save, Trash2 } from 'lucide-vue-next'
import { courseApi, homeworkApi, type Course, type Homework } from '@/lib/api'
import Empty from '@/components/Empty.vue'

const homeworks = ref<Homework[]>([])
const courses = ref<Course[]>([])
const form = reactive({ courseId: 0, title: '', description: '', deadline: '' })
const editForms = reactive<Record<number, { courseId: number; title: string; description: string; deadline: string }>>({})
const message = ref('')
const error = ref('')

function toInputTime(value?: string) { return value ? value.replace(' ', 'T').slice(0, 16) : '' }
function toPayloadTime(value: string) { return value.length === 16 ? `${value}:00` : value }

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
  message.value = ''; error.value = ''
  try {
    await homeworkApi.create({ courseId: form.courseId, title: form.title, description: form.description, deadline: toPayloadTime(form.deadline) })
    form.title = ''; form.description = ''; form.deadline = ''
    message.value = '作业已创建'; await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '创建失败'
  }
}

async function saveHomework(homework: Homework) {
  const item = editForms[homework.id]
  await homeworkApi.update(homework.id, { courseId: item.courseId, title: item.title, description: item.description, deadline: toPayloadTime(item.deadline) })
  message.value = '作业已保存'; await load()
}

async function removeHomework(id: number) {
  await homeworkApi.remove(id); message.value = '作业已删除'; await load()
}

onMounted(load)
</script>

<template>
  <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">教师作业管理</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">创建、编辑与发布课程作业</h1>
      </div>

      <!-- Create form -->
      <div class="mb-8 rounded-xl border border-border bg-white p-5 shadow-sm">
        <h2 class="mb-4 font-semibold text-neutral-900">新增作业</h2>
        <div v-if="message" class="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</div>
        <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>
        <form class="grid gap-3 lg:grid-cols-[160px_1fr_1fr_200px_auto]" @submit.prevent="createHomework">
          <select v-model.number="form.courseId" required class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400">
            <option v-for="course in courses" :key="course.id" :value="course.id">{{ course.title }}</option>
          </select>
          <input v-model="form.title" required placeholder="作业标题" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
          <input v-model="form.description" placeholder="作业说明（可选）" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
          <input v-model="form.deadline" required type="datetime-local" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400" />
          <button type="submit" class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700">
            <Plus class="h-4 w-4" />创建
          </button>
        </form>
      </div>

      <!-- Homework list -->
      <div class="space-y-4">
        <article v-for="homework in homeworks" :key="homework.id" class="rounded-xl border border-border bg-white p-5 shadow-sm">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p class="text-xs text-primary-600">{{ homework.courseTitle || `课程 #${homework.courseId}` }}</p>
              <h2 class="mt-1 font-semibold text-neutral-900">{{ homework.title }}</h2>
              <p class="text-xs text-neutral-400">截止：{{ homework.deadline }}</p>
            </div>
            <div class="flex gap-2">
              <RouterLink :to="`/teacher/homeworks/${homework.id}/submissions`" class="inline-flex items-center gap-1.5 rounded-lg bg-primary-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm hover:bg-primary-700">
                <ClipboardList class="h-3.5 w-3.5" />提交列表
              </RouterLink>
              <button class="inline-flex items-center gap-1.5 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50" @click="removeHomework(homework.id)">
                <Trash2 class="h-3.5 w-3.5" />删除
              </button>
            </div>
          </div>
          <div class="mt-4 grid gap-2 border-t border-border pt-4 lg:grid-cols-[160px_1fr_1fr_200px_auto]">
            <select v-model.number="editForms[homework.id].courseId" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400">
              <option v-for="course in courses" :key="course.id" :value="course.id">{{ course.title }}</option>
            </select>
            <input v-model="editForms[homework.id].title" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
            <input v-model="editForms[homework.id].description" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
            <input v-model="editForms[homework.id].deadline" type="datetime-local" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400" />
            <button class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-border px-3 text-sm font-medium text-neutral-700 hover:bg-neutral-100" @click="saveHomework(homework)">
              <Save class="h-4 w-4" />保存
            </button>
          </div>
        </article>
      </div>

      <Empty v-if="homeworks.length === 0" title="暂无作业" :icon="BookOpenCheck" />
  </div>
</template>
