<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Send, SquarePlus } from 'lucide-vue-next'
import { categoryApi, courseApi, formatCourseStatus, formatDuration, type Category, type Chapter, type Course } from '@/lib/api'
import { useAuth } from '@/composables/useAuth'

const auth = useAuth()
const categories = ref<Category[]>([])
const courses = ref<Course[]>([])
const message = ref('')
const form = reactive({ title: '', intro: '', categoryId: 0, price: 0 })
const chapterForms = reactive<Record<number, { title: string; sort: number; duration: number }>>({})
const uploading = reactive<Record<number, boolean>>({})
const isTeacher = computed(() => auth.state.user?.role === 'TEACHER')

async function load() {
  categories.value = await categoryApi.list()
  if (!form.categoryId && categories.value[0]) form.categoryId = categories.value[0].id
  const userId = auth.state.user?.id
  const page = await courseApi.page({ current: 1, size: 50, teacherId: userId })
  courses.value = page.records
  courses.value.forEach((course) => {
    if (!chapterForms[course.id]) chapterForms[course.id] = { title: '', sort: (course.chapters?.length ?? 0) + 1, duration: 0 }
  })
}

async function createCourse() {
  if (!isTeacher.value) {
    message.value = '请使用教师账号创建课程'
    return
  }
  await courseApi.create({ title: form.title, intro: form.intro, categoryId: form.categoryId, price: form.price })
  form.title = ''
  form.intro = ''
  form.price = 0
  message.value = '课程已创建，请在下方添加章节并提交审核'
  await load()
}

async function submitReview(id: number) {
  await courseApi.submitReview(id)
  message.value = '已提交审核，等待管理员审核'
  await load()
}

async function createChapter(courseId: number) {
  const item = chapterForms[courseId]
  await courseApi.createChapter(courseId, item)
  item.title = ''
  item.sort += 1
  item.duration = 0
  message.value = '章节已添加'
  await load()
}

async function uploadChapterVideo(courseId: number, chapter: Chapter, event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploading[chapter.id] = true
  try {
    // 自动读取视频时长
    const duration = await getVideoDuration(file)
    if (duration > 0) {
      chapter.duration = Math.round(duration)
    }
    const updated = await courseApi.uploadChapterVideo(courseId, chapter.id, file)
    chapter.videoPath = updated.videoPath
    if (duration > 0) {
      message.value = `视频已上传，时长 ${formatDuration(Math.round(duration))}`
    } else {
      message.value = '视频已上传'
    }
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
    video.onloadedmetadata = () => {
      URL.revokeObjectURL(url)
      resolve(isFinite(video.duration) ? video.duration : 0)
    }
    video.onerror = () => {
      URL.revokeObjectURL(url)
      resolve(0)
    }
  })
}

onMounted(load)
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/dashboard" class="text-sm text-cyan-200">返回工作台</RouterLink>
        <RouterLink to="/courses" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">课程中心</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">教师课程管理</p>
        <h1 class="mt-2 text-4xl font-black">发布课程与章节</h1>
        <p v-if="message" class="mt-4 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <form class="mt-8 grid gap-4 lg:grid-cols-[1fr_1fr_160px_120px_120px]" @submit.prevent="createCourse">
          <input v-model="form.title" required class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="课程名称" />
          <input v-model="form.intro" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="课程简介" />
          <select v-model="form.categoryId" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
            <option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
          <div class="relative">
            <span class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">¥</span>
            <input v-model.number="form.price" type="number" min="0" step="0.01" class="w-full rounded-2xl border border-white/10 bg-slate-900 py-3 pl-7 pr-3 outline-none focus:border-cyan-300" placeholder="价格" />
          </div>
          <button class="inline-flex items-center justify-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">
            <Plus class="h-4 w-4" />创建
          </button>
        </form>
        <p class="mt-3 text-xs text-slate-500">价格填 0 表示免费课程；创建后需添加章节视频并提交审核才能发布。</p>
      </section>

      <section class="mt-8 space-y-5">
        <article v-for="course in courses" :key="course.id" class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <div class="flex items-center gap-3">
                <span class="rounded-full px-3 py-1 text-xs font-bold" :class="{
                  'bg-slate-600/60 text-slate-300': course.status === 'DRAFT',
                  'bg-amber-400/20 text-amber-200': course.status === 'REVIEWING',
                  'bg-green-400/20 text-green-200': course.status === 'PUBLISHED',
                  'bg-red-400/20 text-red-200': course.status === 'REJECTED',
                }">{{ formatCourseStatus(course.status) }}</span>
                <span class="text-sm text-slate-400">{{ course.categoryName }}</span>
                <span class="text-sm text-cyan-200">{{ course.price > 0 ? `¥${course.price}` : '免费' }}</span>
              </div>
              <h2 class="mt-2 text-2xl font-black">{{ course.title }}</h2>
              <p class="mt-2 text-sm text-slate-300">{{ course.intro || '暂无简介' }}</p>
            </div>
            <button v-if="course.status === 'DRAFT'" class="inline-flex items-center gap-2 rounded-full border border-cyan-300/40 px-5 py-2 text-sm text-cyan-100 hover:bg-cyan-300/10" @click="submitReview(course.id)">
              <Send class="h-4 w-4" />提交审核
            </button>
          </div>

          <!-- 添加章节表单 -->
          <div class="mt-6 grid gap-3 md:grid-cols-[1fr_100px_120px]">
            <input v-model="chapterForms[course.id].title" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="新章节标题" />
            <input v-model.number="chapterForms[course.id].sort" type="number" min="1" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="排序" />
            <button class="inline-flex items-center justify-center gap-2 rounded-2xl bg-white px-4 py-3 font-bold text-slate-950 hover:bg-cyan-100" @click="createChapter(course.id)">
              <SquarePlus class="h-4 w-4" />添加章节
            </button>
          </div>
          <p class="mt-1 text-xs text-slate-500">上传视频时会自动读取时长，无需手动填写。</p>

          <!-- 章节列表 -->
          <div v-if="course.chapters?.length" class="mt-5 space-y-3">
            <div v-for="(chapter, idx) in course.chapters" :key="chapter.id" class="grid gap-3 rounded-2xl border border-white/10 bg-slate-900/80 p-4 md:grid-cols-[1fr_220px]">
              <div>
                <p class="font-semibold">第{{ idx + 1 }}章 · {{ chapter.title }}</p>
                <p class="mt-1 text-sm text-slate-400">{{ formatDuration(chapter.duration) }} · {{ chapter.videoPath ? '已上传视频' : '未上传视频' }}</p>
              </div>
              <label class="inline-flex cursor-pointer items-center justify-center rounded-2xl border border-cyan-300/40 px-4 py-3 text-sm text-cyan-100 hover:bg-cyan-300/10">
                {{ uploading[chapter.id] ? '上传中...' : (chapter.videoPath ? '重新上传' : '上传视频') }}
                <input class="hidden" type="file" accept="video/mp4,video/webm,video/ogg,video/quicktime,video/x-msvideo" :disabled="uploading[chapter.id]" @change="uploadChapterVideo(course.id, chapter, $event)" />
              </label>
            </div>
          </div>
        </article>
      </section>
    </div>
  </main>
</template>
