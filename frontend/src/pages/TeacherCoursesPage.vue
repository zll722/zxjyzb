<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { BookOpen, ImagePlus, Pencil, Plus, Send, SquarePlus, Trash2, Upload, X } from 'lucide-vue-next'
import { RouterLink } from 'vue-router'
import { categoryApi, courseApi, formatCourseStatus, formatDuration, type Category, type Chapter, type Course } from '@/lib/api'
import { useAuth } from '@/composables/useAuth'
import Empty from '@/components/Empty.vue'

const auth = useAuth()
const categories = ref<Category[]>([])
const courses = ref<Course[]>([])
const message = ref('')
const messageType = ref<'success' | 'error'>('success')
const chapterForms = reactive<Record<number, { title: string; sort: number; duration: number }>>({})
const uploading = reactive<Record<number, boolean>>({})
const isTeacher = computed(() => auth.state.user?.role === 'TEACHER')

// Modal state
const modalOpen = ref(false)
const editingCourse = ref<Course | null>(null)
const modalForm = reactive({ title: '', intro: '', cover: '', categoryId: 0, price: 0 })
const modalCoverUploading = ref(false)
const modalCoverInputRef = ref<HTMLInputElement | null>(null)

async function load() {
  categories.value = await categoryApi.list()
  const userId = auth.state.user?.id
  const page = await courseApi.page({ current: 1, size: 50, teacherId: userId })
  courses.value = page.records
  courses.value.forEach((course) => {
    if (!chapterForms[course.id])
      chapterForms[course.id] = { title: '', sort: (course.chapters?.length ?? 0) + 1, duration: 0 }
  })
}

function openCreateModal() {
  editingCourse.value = null
  modalForm.title = ''
  modalForm.intro = ''
  modalForm.cover = ''
  modalForm.categoryId = categories.value[0]?.id ?? 0
  modalForm.price = 0
  modalOpen.value = true
}

function openEditModal(course: Course) {
  editingCourse.value = course
  modalForm.title = course.title
  modalForm.intro = course.intro ?? ''
  modalForm.cover = course.cover ?? ''
  modalForm.categoryId = course.categoryId
  modalForm.price = course.price
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
  editingCourse.value = null
}

async function handleModalCoverUpload(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  modalCoverUploading.value = true
  try {
    modalForm.cover = await courseApi.uploadCover(file)
  } catch (e) {
    message.value = e instanceof Error ? e.message : '封面上传失败'
    messageType.value = 'error'
  } finally {
    modalCoverUploading.value = false
    ;(event.target as HTMLInputElement).value = ''
  }
}

async function submitModal() {
  if (!isTeacher.value) { message.value = '请使用教师账号操作'; messageType.value = 'error'; return }
  try {
    if (editingCourse.value) {
      await courseApi.update(editingCourse.value.id, {
        title: modalForm.title,
        intro: modalForm.intro,
        cover: modalForm.cover || undefined,
        categoryId: modalForm.categoryId,
        price: modalForm.price,
      })
      message.value = '课程信息已更新'
    } else {
      await courseApi.create({
        title: modalForm.title,
        intro: modalForm.intro,
        cover: modalForm.cover || undefined,
        categoryId: modalForm.categoryId,
        price: modalForm.price,
      })
      message.value = '课程已创建，请在下方添加章节并提交审核'
    }
    messageType.value = 'success'
    closeModal()
    await load()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '操作失败'
    messageType.value = 'error'
  }
}

async function submitReview(id: number) {
  await courseApi.submitReview(id)
  message.value = '已提交审核，等待管理员审核'
  messageType.value = 'success'
  await load()
}

async function createChapter(courseId: number) {
  const item = chapterForms[courseId]
  await courseApi.createChapter(courseId, item)
  item.title = ''; item.sort += 1; item.duration = 0
  message.value = '章节已添加'
  messageType.value = 'success'
  await load()
}

async function uploadChapterVideo(courseId: number, chapter: Chapter, event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploading[chapter.id] = true
  try {
    const duration = await getVideoDuration(file)
    if (duration > 0) chapter.duration = Math.round(duration)
    const updated = await courseApi.uploadChapterVideo(courseId, chapter.id, file, duration > 0 ? duration : undefined)
    chapter.videoPath = updated.videoPath
    if (updated.duration > 0) chapter.duration = updated.duration
    message.value = duration > 0 ? `视频已上传，时长 ${formatDuration(Math.round(duration))}` : '视频已上传'
    messageType.value = 'success'
  } finally {
    uploading[chapter.id] = false
    input.value = ''
  }
}

function getVideoDuration(file: File): Promise<number> {
  return new Promise((resolve) => {
    const video = document.createElement('video')
    video.preload = 'metadata'
    const url = URL.createObjectURL(file)
    video.src = url
    video.onloadedmetadata = () => { URL.revokeObjectURL(url); resolve(isFinite(video.duration) ? video.duration : 0) }
    video.onerror = () => { URL.revokeObjectURL(url); resolve(0) }
  })
}

function statusTone(status: string) {
  if (status === 'PUBLISHED') return 'bg-success-bg text-success'
  if (status === 'REVIEWING') return 'bg-warning-bg text-warning'
  if (status === 'REJECTED')  return 'bg-danger-bg text-danger'
  return 'bg-neutral-100 text-neutral-600'
}

async function deleteCourse(id: number) {
  if (!confirm('确认删除该课程？课程下所有章节将一并删除，此操作不可撤销。')) return
  try {
    await courseApi.remove(id)
    message.value = '课程已删除'
    messageType.value = 'success'
    await load()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '删除失败'
    messageType.value = 'error'
  }
}

async function deleteChapter(courseId: number, chapterId: number) {
  if (!confirm('确认删除该章节？此操作不可撤销。')) return
  await courseApi.deleteChapter(courseId, chapterId)
  message.value = '章节已删除'
  messageType.value = 'success'
  await load()
}

onMounted(load)
</script>

<template>
  <div class="mx-auto max-w-4xl">

    <!-- Page header -->
    <div class="mb-6 flex items-center justify-between">
      <div>
        <p class="text-sm font-medium text-neutral-400">教师课程管理</p>
        <h1 class="mt-1 text-2xl font-semibold text-neutral-900">发布课程与章节</h1>
      </div>
      <button
        class="inline-flex items-center gap-2 rounded-xl bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500"
        @click="openCreateModal"
      >
        <Plus class="h-4 w-4" />新建课程
      </button>
    </div>

    <!-- Feedback message -->
    <div
      v-if="message"
      :class="[
        'mb-4 rounded-md px-4 py-3 text-sm',
        messageType === 'success' ? 'bg-success-bg text-success' : 'bg-danger-bg text-danger',
      ]"
    >{{ message }}</div>

    <!-- Course list -->
    <div class="space-y-4">
      <article
        v-for="course in courses"
        :key="course.id"
        class="overflow-hidden rounded-xl border border-border bg-white shadow-sm"
      >
        <!-- Course header -->
        <div class="flex flex-wrap items-start justify-between gap-4 p-5">
          <div class="flex gap-4">
            <div class="h-20 w-28 flex-shrink-0 overflow-hidden rounded-lg border border-border bg-neutral-50">
              <img v-if="course.cover" :src="course.cover" class="h-full w-full object-cover" alt="封面" />
              <div v-else class="flex h-full w-full items-center justify-center text-xs text-neutral-400">暂无封面</div>
            </div>
            <div>
              <div class="flex flex-wrap items-center gap-2">
                <span class="rounded-full px-2.5 py-0.5 text-xs font-medium" :class="statusTone(course.status)">
                  {{ formatCourseStatus(course.status) }}
                </span>
                <span class="text-xs text-neutral-400">{{ course.categoryName }}</span>
                <span class="rounded-md bg-primary-50 px-2 py-0.5 text-xs font-semibold text-primary-700">
                  {{ course.price > 0 ? `¥${course.price}` : '免费' }}
                </span>
              </div>
              <h2 class="mt-1.5 text-base font-semibold text-neutral-900">{{ course.title }}</h2>
              <p class="mt-1 text-sm text-neutral-500">{{ course.intro || '暂无简介' }}</p>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <button
              class="inline-flex min-h-[40px] items-center gap-1.5 rounded-md border border-border px-3 py-2 text-sm font-medium text-neutral-700 transition-all duration-200 hover:bg-neutral-50"
              @click="openEditModal(course)"
            >
              <Pencil class="h-4 w-4" />编辑
            </button>
            <button
              v-if="course.status === 'DRAFT'"
              class="inline-flex min-h-[40px] items-center gap-1.5 rounded-md border border-primary-300 px-3 py-2 text-sm font-medium text-primary-700 transition-all duration-200 hover:bg-primary-50"
              @click="submitReview(course.id)"
            >
              <Send class="h-4 w-4" />提交审核
            </button>
            <button
              class="inline-flex min-h-[40px] items-center gap-1.5 rounded-md border border-danger/30 px-3 py-2 text-sm font-medium text-danger transition-all duration-200 hover:bg-danger-bg"
              @click="deleteCourse(course.id)"
            >
              <Trash2 class="h-4 w-4" />删除
            </button>
          </div>
        </div>

        <!-- Add chapter form -->
        <div class="grid gap-2 border-t border-border bg-neutral-50 p-4 md:grid-cols-[1fr_80px_auto]">
          <input
            v-model="chapterForms[course.id].title"
            placeholder="新章节标题"
            class="h-10 rounded-md border border-border bg-white px-3 text-sm text-neutral-900 outline-none transition-all duration-200 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15"
          />
          <input
            v-model.number="chapterForms[course.id].sort"
            type="number"
            min="1"
            placeholder="排序"
            class="h-10 rounded-md border border-border bg-white px-3 text-sm text-neutral-900 outline-none transition-all duration-200 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15"
          />
          <button
            class="inline-flex min-h-[40px] items-center gap-1.5 rounded-md border border-border bg-white px-3 text-sm font-medium text-neutral-700 transition-all duration-200 hover:bg-neutral-100"
            @click="createChapter(course.id)"
          >
            <SquarePlus class="h-4 w-4" />添加章节
          </button>
        </div>

        <!-- Chapter list -->
        <div v-if="course.chapters?.length" class="space-y-px border-t border-border">
          <div
            v-for="(chapter, idx) in course.chapters"
            :key="chapter.id"
            class="flex items-center justify-between px-5 py-3 odd:bg-white even:bg-neutral-50"
          >
            <div>
              <p class="text-sm font-medium text-neutral-800">第{{ idx + 1 }}章 · {{ chapter.title }}</p>
              <p class="mt-0.5 text-xs text-neutral-400">
                {{ formatDuration(chapter.duration) }} ·
                <span :class="chapter.videoPath ? 'text-success' : 'text-neutral-400'">
                  {{ chapter.videoPath ? '已上传视频' : '未上传视频' }}
                </span>
              </p>
            </div>
            <div class="flex items-center gap-2">
              <label
                class="inline-flex min-h-[36px] cursor-pointer items-center gap-1.5 rounded-md border border-border bg-white px-3 text-xs font-medium text-neutral-700 transition-all duration-200 hover:bg-neutral-100"
              >
                <Upload class="h-3.5 w-3.5" />
                {{ uploading[chapter.id] ? '上传中…' : chapter.videoPath ? '重新上传' : '上传视频' }}
                <input
                  class="hidden"
                  type="file"
                  accept="video/mp4,video/webm,video/ogg,video/quicktime,video/x-msvideo"
                  :disabled="uploading[chapter.id]"
                  @change="uploadChapterVideo(course.id, chapter, $event)"
                />
              </label>
              <button
                class="inline-flex min-h-[36px] items-center gap-1.5 rounded-md border border-danger/30 bg-white px-3 text-xs font-medium text-danger transition-all duration-200 hover:bg-danger-bg"
                @click="deleteChapter(course.id, chapter.id)"
              >
                <Trash2 class="h-3.5 w-3.5" />删除
              </button>
            </div>
          </div>
        </div>
      </article>
    </div>

    <!-- Empty state -->
    <div v-if="courses.length === 0" class="mt-4 rounded-xl border border-border bg-white shadow-sm">
      <Empty
        :icon="BookOpen"
        title="还没有创建课程"
        description="点击右上角「新建课程」按钮创建你的第一门课程"
      />
    </div>

    <!-- Create / Edit Course Modal -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="modalOpen"
          class="fixed inset-0 z-[70] flex items-center justify-center p-4"
          @keydown.esc="closeModal"
        >
          <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="closeModal" />
          <div class="modal-panel relative w-full max-w-lg rounded-2xl border border-border bg-white shadow-2xl">
            <!-- Header -->
            <div class="flex items-center justify-between border-b border-border px-6 py-4">
              <h2 class="text-base font-semibold text-neutral-900">
                {{ editingCourse ? '编辑课程信息' : '新建课程' }}
              </h2>
              <button class="rounded-lg p-1.5 text-neutral-400 transition hover:bg-neutral-100" @click="closeModal">
                <X class="h-4 w-4" />
              </button>
            </div>
            <!-- Body -->
            <form class="space-y-4 p-6" @submit.prevent="submitModal">
              <!-- Cover -->
              <div>
                <p class="mb-2 text-sm font-medium text-neutral-700">课程封面</p>
                <div
                  class="relative flex h-32 cursor-pointer items-center justify-center overflow-hidden rounded-xl border-2 border-dashed border-border bg-neutral-50 transition hover:border-primary-400 hover:bg-primary-50"
                  @click="modalCoverInputRef?.click()"
                >
                  <img v-if="modalForm.cover" :src="modalForm.cover" alt="封面预览" class="h-full w-full object-cover" />
                  <div v-else class="flex flex-col items-center gap-2 text-neutral-400">
                    <ImagePlus class="h-7 w-7" />
                    <span class="text-xs">点击上传封面（可选）</span>
                  </div>
                  <div v-if="modalCoverUploading" class="absolute inset-0 flex items-center justify-center bg-white/70">
                    <span class="text-xs text-neutral-500">上传中...</span>
                  </div>
                  <button
                    v-if="modalForm.cover && !modalCoverUploading"
                    type="button"
                    class="absolute right-2 top-2 rounded-full bg-black/50 p-1 text-white hover:bg-black/70"
                    @click.stop="modalForm.cover = ''"
                  >
                    <X class="h-3 w-3" />
                  </button>
                </div>
                <input ref="modalCoverInputRef" type="file" accept="image/jpeg,image/png,image/webp,image/gif" class="hidden" @change="handleModalCoverUpload" />
              </div>
              <!-- Title + Category -->
              <div class="grid gap-3 sm:grid-cols-[3fr_2fr]">
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">课程名称 <span class="text-danger">*</span></label>
                  <input
                    v-model="modalForm.title"
                    required
                    placeholder="输入课程名称"
                    class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition-all duration-200 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15"
                  />
                </div>
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">所属分类</label>
                  <select
                    v-model="modalForm.categoryId"
                    class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition-all duration-200 focus:border-primary-600"
                  >
                    <option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option>
                  </select>
                </div>
              </div>
              <!-- Intro + Price -->
              <div class="grid gap-3 sm:grid-cols-[3fr_1fr]">
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">课程简介</label>
                  <input
                    v-model="modalForm.intro"
                    placeholder="用一句话介绍这门课程（可选）"
                    class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition-all duration-200 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15"
                  />
                </div>
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">价格（元）</label>
                  <div class="relative">
                    <span class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sm text-neutral-400">¥</span>
                    <input
                      v-model.number="modalForm.price"
                      type="number"
                      min="0"
                      step="0.01"
                      placeholder="0"
                      class="h-10 w-full rounded-md border border-border bg-white py-0 pl-7 pr-3 text-sm outline-none transition-all duration-200 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15"
                    />
                  </div>
                </div>
              </div>
              <p class="text-xs text-neutral-400">价格填 0 表示免费课程</p>
              <!-- Actions -->
              <div class="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  class="rounded-md border border-border px-4 py-2 text-sm font-medium text-neutral-700 transition hover:bg-neutral-50"
                  @click="closeModal"
                >取消</button>
                <button
                  type="submit"
                  :disabled="modalCoverUploading"
                  class="rounded-md bg-primary-600 px-5 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-500 disabled:opacity-50"
                >{{ editingCourse ? '保存' : '创建' }}</button>
              </div>
            </form>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-active .modal-panel,
.modal-leave-active .modal-panel {
  transition: transform 0.2s ease, opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-from .modal-panel,
.modal-leave-to .modal-panel {
  transform: translateY(12px) scale(0.97);
  opacity: 0;
}
</style>
