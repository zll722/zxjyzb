<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { BookOpenCheck, ClipboardList, Pencil, Plus, Trash2, X } from 'lucide-vue-next'
import { courseApi, homeworkApi, type Course, type Homework } from '@/lib/api'
import Empty from '@/components/Empty.vue'

const homeworks = ref<Homework[]>([])
const courses = ref<Course[]>([])
const message = ref('')
const error = ref('')

const modalOpen = ref(false)
const editingHomework = ref<Homework | null>(null)
const modalForm = reactive({ courseId: 0, title: '', description: '', deadline: '' })

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
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  }
}

function openCreateModal() {
  editingHomework.value = null
  modalForm.courseId = courses.value[0]?.id ?? 0
  modalForm.title = ''
  modalForm.description = ''
  modalForm.deadline = ''
  modalOpen.value = true
}

function openEditModal(homework: Homework) {
  editingHomework.value = homework
  modalForm.courseId = homework.courseId
  modalForm.title = homework.title
  modalForm.description = homework.description || ''
  modalForm.deadline = toInputTime(homework.deadline)
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
  editingHomework.value = null
}

async function submitModal() {
  message.value = ''; error.value = ''
  try {
    if (editingHomework.value) {
      await homeworkApi.update(editingHomework.value.id, { courseId: modalForm.courseId, title: modalForm.title, description: modalForm.description, deadline: toPayloadTime(modalForm.deadline) })
      message.value = '作业已保存'
    } else {
      await homeworkApi.create({ courseId: modalForm.courseId, title: modalForm.title, description: modalForm.description, deadline: toPayloadTime(modalForm.deadline) })
      message.value = '作业已创建'
    }
    closeModal()
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '操作失败'
  }
}

async function removeHomework(id: number) {
  await homeworkApi.remove(id); message.value = '作业已删除'; await load()
}

onMounted(load)
</script>

<template>
  <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <p class="text-sm font-medium text-primary-600">教师作业管理</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">创建、编辑与发布课程作业</h1>
      </div>
      <button
        class="inline-flex items-center gap-2 rounded-xl bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700"
        @click="openCreateModal"
      >
        <Plus class="h-4 w-4" />新增作业
      </button>
    </div>

    <div v-if="message" class="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</div>
    <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

    <div class="space-y-4">
      <article v-for="homework in homeworks" :key="homework.id" class="rounded-xl border border-border bg-white p-5 shadow-sm">
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p class="text-xs text-primary-600">{{ homework.courseTitle || `课程 #${homework.courseId}` }}</p>
            <h2 class="mt-1 font-semibold text-neutral-900">{{ homework.title }}</h2>
            <p class="text-xs text-neutral-400">截止：{{ homework.deadline }}</p>
            <p v-if="homework.description" class="mt-1 text-sm text-neutral-500">{{ homework.description }}</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <button
              class="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-neutral-700 hover:bg-neutral-100"
              @click="openEditModal(homework)"
            >
              <Pencil class="h-3.5 w-3.5" />编辑
            </button>
            <RouterLink :to="`/teacher/homeworks/${homework.id}/submissions`" class="inline-flex items-center gap-1.5 rounded-lg bg-primary-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm hover:bg-primary-700">
              <ClipboardList class="h-3.5 w-3.5" />提交列表
            </RouterLink>
            <button class="inline-flex items-center gap-1.5 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50" @click="removeHomework(homework.id)">
              <Trash2 class="h-3.5 w-3.5" />删除
            </button>
          </div>
        </div>
      </article>
    </div>

    <Empty v-if="homeworks.length === 0" title="暂无作业" :icon="BookOpenCheck" />

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="modalOpen" class="fixed inset-0 z-[70] flex items-center justify-center p-4" @keydown.esc="closeModal">
          <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="closeModal" />
          <div class="modal-panel relative w-full max-w-lg rounded-2xl border border-border bg-white shadow-2xl">
            <div class="flex items-center justify-between border-b border-border px-6 py-4">
              <h2 class="text-base font-semibold text-neutral-900">{{ editingHomework ? '编辑作业' : '新增作业' }}</h2>
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
                <label class="mb-1.5 block text-sm font-medium text-neutral-700">作业标题 <span class="text-danger">*</span></label>
                <input v-model="modalForm.title" required placeholder="请输入作业标题" class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15" />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-neutral-700">作业说明</label>
                <input v-model="modalForm.description" placeholder="作业说明（可选）" class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15" />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-neutral-700">截止时间 <span class="text-danger">*</span></label>
                <input v-model="modalForm.deadline" required type="datetime-local" class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600" />
              </div>
              <div class="flex justify-end gap-3 pt-2">
                <button type="button" class="rounded-md border border-border px-4 py-2 text-sm font-medium text-neutral-700 transition hover:bg-neutral-50" @click="closeModal">取消</button>
                <button type="submit" class="rounded-md bg-primary-600 px-5 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700">{{ editingHomework ? '保存' : '创建' }}</button>
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
