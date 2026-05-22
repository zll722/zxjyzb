<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { BookOpen, ImagePlus, Plus, Send, SquarePlus, Trash2, Upload } from 'lucide-vue-next'
import { RouterLink } from 'vue-router'
import { categoryApi, courseApi, formatCourseStatus, formatDuration, type Category, type Chapter, type Course } from '@/lib/api'
import { useAuth } from '@/composables/useAuth'
import Empty from '@/components/Empty.vue'

const auth = useAuth()
const categories = ref<Category[]>([])
const courses = ref<Course[]>([])
const message = ref('')
const messageType = ref<'success' | 'error'>('success')
const form = reactive({ title: '', intro: '', cover: '', categoryId: 0, price: 0 })
const coverUploading = ref(false)
const chapterForms = reactive<Record<number, { title: string; sort: number; duration: number }>>({})
const uploading = reactive<Record<number, boolean>>({})
const coverUploadingMap = reactive<Record<number, boolean>>({})
const isTeacher = computed(() => auth.state.user?.role === 'TEACHER')

async function load() {
  categories.value = await categoryApi.list()
  if (!form.categoryId && categories.value[0]) form.categoryId = categories.value[0].id
  const userId = auth.state.user?.id
  const page = await courseApi.page({ current: 1, size: 50, teacherId: userId })
  courses.value = page.records
  courses.value.forEach((course) => {
    if (!chapterForms[course.id])
      chapterForms[course.id] = { title: '', sort: (course.chapters?.length ?? 0) + 1, duration: 0 }
  })
}

async function handleCoverUpload(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  coverUploading.value = true
  try {
    form.cover = await courseApi.uploadCover(file)
  } catch (e) {
    message.value = e instanceof Error ? e.message : '封面上传失败'
    messageType.value = 'error'
  } finally {
    coverUploading.value = false
    ;(event.target as HTMLInputElement).value = ''
  }
}

async function createCourse() {
  if (!isTeacher.value) { message.value = '请使用教师账号创建课程'; messageType.value = 'error'; return }
  await courseApi.create({ title: form.title, intro: form.intro, cover: form.cover || undefined, categoryId: form.categoryId, price: form.price })
  form.title = ''; form.intro = ''; form.cover = ''; form.price = 0
  message.value = '课程已创建，请在下方添加章节并提交审核'
  messageType.value = 'success'
  await load()
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

async function uploadCourseCover(course: Course, event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  coverUploadingMap[course.id] = true
  try {
    const coverUrl = await courseApi.uploadCover(file)
    await courseApi.update(course.id, {
      title: course.title,
      cover: coverUrl,
      intro: course.intro,
      categoryId: course.categoryId,
      price: course.price,
    })
    message.value = '课程封面已更新'
    messageType.value = 'success'
    await load()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '封面上传失败'
    messageType.value = 'error'
  } finally {
    coverUploadingMap[course.id] = false
    ;(event.target as HTMLInputElement).value = ''
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
    <div class="mb-6">
      <p class="text-sm font-medium text-neutral-400">教师课程管理</p>
      <h1 class="mt-1 text-2xl font-semibold text-neutral-900">发布课程与章节</h1>
    </div>

    <!-- ─────────────────────────────────────────────────────
        Create course form — two-row layout
        ───────────────────────────────────────────────────── -->
    <div class="mb-8 rounded-xl border border-border bg-white p-6 shadow-sm">
      <h2 class="mb-5 text-base font-semibold text-neutral-900">创建新课程</h2>

      <!-- Feedback message -->
      <div
        v-if="message"
        :class="[
          'mb-4 rounded-md px-4 py-3 text-sm',
          messageType === 'success' ? 'bg-success-bg text-success' : 'bg-danger-bg text-danger',
        ]"
      >{{ message }}</div>

      <form class="space-y-4" @submit.prevent="createCourse">
        <!-- Row 1: title (60%) + category (40%) -->
        <div class="grid gap-3 sm:grid-cols-[3fr_2fr]">
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-neutral-700">课程名称 <span class="text-danger">*</span></label>
            <input
              v-model="form.title"
              required
              placeholder="输入课程名称"
              class="h-10 rounded-md border border-border bg-white px-3 text-sm text-neutral-900 outline-none transition-all duration-200 placeholder:text-neutral-400 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15"
            />
          </div>
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-neutral-700">所属分类</label>
            <select
              v-model="form.categoryId"
              class="h-10 rounded-md border border-border bg-white px-3 text-sm text-neutral-700 outline-none transition-all duration-200 focus:border-primary-600"
            >
              <option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option>
            </select>
          </div>
        </div>

        <!-- Row 1.5: cover upload -->
        <div class="flex items-center gap-3">
          <img v-if="form.cover" :src="form.cover" class="h-10 w-16 rounded object-cover border border-border flex-shrink-0" />
          <label class="inline-flex h-10 flex-1 cursor-pointer items-center gap-2 rounded-md border border-border bg-white px-3 text-sm text-neutral-500 transition-all duration-200 hover:bg-neutral-50" :class="coverUploading ? 'opacity-60 pointer-events-none' : ''">
            <ImagePlus class="h-4 w-4 text-neutral-400 flex-shrink-0" />
            {{ coverUploading ? '上传中…' : form.cover ? '重新上传封面' : '上传课程封面（可选）' }}
            <input class="hidden" type="file" accept="image/jpeg,image/png,image/webp,image/gif" :disabled="coverUploading" @change="handleCoverUpload" />
          </label>
        </div>

        <!-- Row 2: intro (70%) + price (20%) + submit (10%) -->
        <div class="grid gap-3 sm:grid-cols-[7fr_2fr_auto]">
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-neutral-700">课程简介</label>
            <input
              v-model="form.intro"
              placeholder="用一句话介绍这门课程（可选）"
              class="h-10 rounded-md border border-border bg-white px-3 text-sm text-neutral-900 outline-none transition-all duration-200 placeholder:text-neutral-400 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15"
            />
          </div>
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-neutral-700">价格（元）</label>
            <div class="relative">
              <span class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 select-none text-sm text-neutral-400">¥</span>
              <input
                v-model.number="form.price"
                type="number"
                min="0"
                step="0.01"
                placeholder="0"
                class="h-10 w-full min-w-[120px] rounded-md border border-border bg-white py-0 pl-7 pr-3 text-sm text-neutral-900 outline-none transition-all duration-200 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15"
              />
            </div>
          </div>
          <div class="flex flex-col gap-1.5">
            <label class="invisible text-sm font-medium">操作</label>
            <button
              type="submit"
              class="inline-flex h-10 min-w-[80px] items-center justify-center gap-1.5 rounded-md bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500"
            >
              <Plus class="h-4 w-4" />创建
            </button>
          </div>
        </div>

        <p class="text-xs text-neutral-400">价格填 0 表示免费课程；创建后需添加章节视频并提交审核才能发布。</p>
      </form>
    </div>

    <!-- ─────────────────────────────────────────────────────
        Course list
        ───────────────────────────────────────────────────── -->
    <div class="space-y-4">
      <article
        v-for="course in courses"
        :key="course.id"
        class="overflow-hidden rounded-xl border border-border bg-white shadow-sm"
      >
        <!-- Course header -->
        <div class="flex flex-wrap items-start justify-between gap-4 p-5">
          <div class="flex gap-4">
            <div class="relative flex-shrink-0">
              <img v-if="course.cover" :src="course.cover" class="h-20 w-28 rounded-lg object-cover border border-border" />
              <div v-else class="flex h-20 w-28 items-center justify-center rounded-lg border border-dashed border-border bg-neutral-50 text-xs text-neutral-400">暂无封面</div>
              <label class="absolute inset-0 flex cursor-pointer items-center justify-center rounded-lg bg-black/40 opacity-0 transition-opacity hover:opacity-100" :class="coverUploadingMap[course.id] ? 'opacity-100 pointer-events-none' : ''">
                <span class="text-xs font-medium text-white">{{ coverUploadingMap[course.id] ? '上传中…' : '更换封面' }}</span>
                <input class="hidden" type="file" accept="image/jpeg,image/png,image/webp,image/gif" :disabled="coverUploadingMap[course.id]" @change="uploadCourseCover(course, $event)" />
              </label>
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
              <Trash2 class="h-4 w-4" />删除课程
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

    <!-- Empty state — three-layer structure -->
    <div v-if="courses.length === 0" class="mt-4 rounded-xl border border-border bg-white shadow-sm">
      <Empty
        :icon="BookOpen"
        title="还没有创建课程"
        description="填写上方表单创建你的第一门课程"
      />
    </div>
  </div>
</template>
