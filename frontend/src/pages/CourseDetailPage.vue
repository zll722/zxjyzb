<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { BookOpen, Heart, PlayCircle, ShoppingCart } from 'lucide-vue-next'
import { courseApi, getToken, orderApi, type Chapter, type Course } from '@/lib/api'
import { useAuth } from '@/composables/useAuth'

const route = useRoute()
const auth = useAuth()
const course = ref<Course | null>(null)
const activeChapter = ref<Chapter | null>(null)
const message = ref('')
const buying = ref(false)
const canPlay = computed(() => course.value?.accessible !== false)

// 进度保存节流：最多每30秒保存一次
let lastSavedAt = 0
async function saveProgress(event: Event) {
  if (!course.value || !activeChapter.value || !getToken()) return
  const video = event.target as HTMLVideoElement
  if (!video.duration || Number.isNaN(video.duration)) return
  const now = Date.now()
  const isFinished = event.type === 'ended' || (event.type === 'pause' && video.currentTime > 0)
  if (!isFinished && now - lastSavedAt < 30_000) return
  lastSavedAt = now
  const progress = Math.min(100, Math.max(1, Math.round((video.currentTime / video.duration) * 100)))
  await courseApi.saveRecord({ courseId: course.value.id, chapterId: activeChapter.value.id, progress })
  message.value = `学习进度已保存：${progress}%`
  if (isFinished) setTimeout(() => { message.value = '' }, 3000)
}

function formatDuration(seconds: number) {
  if (!seconds) return '--'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  return `${m}:${String(s).padStart(2, '0')}`
}

function formatCourseStatus(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    REVIEWING: '审核中',
    PUBLISHED: '已发布',
    REJECTED: '已退回',
    OFFLINE: '已下线',
  }
  return map[status] ?? status
}

async function load() {
  course.value = await courseApi.detail(Number(route.params.id))
  activeChapter.value = course.value.chapters?.find((c) => c.videoPath) || course.value.chapters?.[0] || null
}

async function toggleFavorite() {
  if (!course.value || !getToken()) {
    message.value = '请先登录后收藏'
    return
  }
  if (auth.state.user?.role !== 'STUDENT') {
    message.value = '仅学生账号可收藏课程'
    return
  }
  if (course.value.favorite) {
    await courseApi.unfavorite(course.value.id)
  } else {
    await courseApi.favorite(course.value.id)
  }
  await load()
}

async function buyCourse() {
  if (!course.value || !getToken()) {
    message.value = '请先登录后购买'
    return
  }
  buying.value = true
  message.value = ''
  try {
    const order = await orderApi.create(course.value.id)
    await orderApi.pay(order.id)
    message.value = '购买成功！正在加载课程内容...'
    await load()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '购买失败，请稍后重试'
  } finally {
    buying.value = false
  }
}

function selectChapter(chapter: Chapter) {
  activeChapter.value = chapter
  message.value = ''
}

onMounted(load)
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div v-if="course" class="mx-auto max-w-6xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/courses" class="text-sm text-cyan-200">返回课程中心</RouterLink>
        <RouterLink to="/dashboard" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">工作台</RouterLink>
      </nav>

      <section class="grid gap-8 lg:grid-cols-[0.9fr_1.1fr]">
        <div class="grid min-h-80 place-items-center rounded-[2rem] bg-gradient-to-br from-cyan-300 to-teal-300 text-8xl font-black text-slate-950 shadow-2xl shadow-cyan-500/20">
          {{ course.title.slice(0, 1) }}
        </div>
        <div class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
          <p class="text-sm text-cyan-200">{{ course.categoryName }} · {{ formatCourseStatus(course.status) }}</p>
          <h1 class="mt-3 text-5xl font-black">{{ course.title }}</h1>
          <p class="mt-6 leading-8 text-slate-300">{{ course.intro || '暂无课程简介' }}</p>
          <div class="mt-8 flex flex-wrap gap-3 text-sm text-slate-300">
            <span class="rounded-full bg-white/8 px-4 py-2">讲师 {{ course.teacherName }}</span>
            <span class="rounded-full bg-white/8 px-4 py-2">收藏 {{ course.favoritesCount }}</span>
            <span class="rounded-full bg-white/8 px-4 py-2">
              {{ course.price > 0 ? `¥${course.price}` : '免费' }}
            </span>
          </div>

          <div class="mt-8 flex flex-wrap gap-3">
            <!-- 购买按钮：付费课程 + 未购买 -->
            <button
              v-if="course.price > 0 && !course.accessible"
              :disabled="buying"
              class="inline-flex items-center gap-2 rounded-full bg-cyan-300 px-6 py-3 font-bold text-slate-950 hover:bg-cyan-200 disabled:opacity-60"
              @click="buyCourse"
            >
              <ShoppingCart class="h-5 w-5" />
              {{ buying ? '处理中...' : `立即购买 ¥${course.price}` }}
            </button>

            <!-- 收藏按钮 -->
            <button
              class="inline-flex items-center gap-2 rounded-full border border-white/15 px-6 py-3 font-bold text-white hover:bg-white/10"
              @click="toggleFavorite"
            >
              <Heart class="h-5 w-5" :class="course.favorite ? 'fill-cyan-300 text-cyan-300' : ''" />
              {{ course.favorite ? '取消收藏' : '收藏课程' }}
            </button>
          </div>

          <p v-if="message" class="mt-4 text-sm" :class="message.includes('失败') ? 'text-red-300' : 'text-cyan-200'">{{ message }}</p>
        </div>
      </section>

      <section class="mt-8 rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <div class="mb-6 flex items-center gap-3">
          <PlayCircle class="h-6 w-6 text-cyan-200" />
          <h2 class="text-2xl font-black">章节视频</h2>
        </div>
        <video
          v-if="activeChapter?.videoPath && canPlay"
          class="w-full rounded-3xl bg-black"
          controls
          :src="courseApi.chapterVideoUrl(course.id, activeChapter.id)"
          @pause="saveProgress"
          @ended="saveProgress"
          @timeupdate="saveProgress"
        />
        <div v-else class="rounded-3xl border border-white/10 bg-slate-900 p-8 text-center">
          <p class="text-slate-300">
            {{ canPlay ? '当前章节暂无视频' : '该课程需要购买后才能观看' }}
          </p>
          <button
            v-if="!canPlay && course.price > 0"
            :disabled="buying"
            class="mt-4 inline-flex items-center gap-2 rounded-full bg-cyan-300 px-6 py-3 font-bold text-slate-950 hover:bg-cyan-200 disabled:opacity-60"
            @click="buyCourse"
          >
            <ShoppingCart class="h-5 w-5" />
            {{ buying ? '处理中...' : `立即购买 ¥${course.price}` }}
          </button>
        </div>
      </section>

      <section class="mt-8 rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <div class="mb-6 flex items-center gap-3">
          <BookOpen class="h-6 w-6 text-cyan-200" />
          <h2 class="text-2xl font-black">章节目录</h2>
        </div>
        <div class="space-y-3">
          <button
            v-for="(chapter, idx) in course.chapters"
            :key="chapter.id"
            class="flex w-full items-center justify-between rounded-2xl bg-slate-900 p-4 text-left hover:bg-slate-800"
            :class="activeChapter?.id === chapter.id ? 'ring-2 ring-cyan-300/60' : ''"
            @click="selectChapter(chapter)"
          >
            <div>
              <p class="font-semibold">第{{ idx + 1 }}章 · {{ chapter.title }}</p>
              <p class="mt-1 text-sm text-slate-400">
                时长 {{ formatDuration(chapter.duration) }} · {{ chapter.videoPath ? '可播放' : '暂无视频' }}
              </p>
            </div>
            <span class="inline-flex items-center gap-2 rounded-full border border-white/10 px-4 py-2 text-sm">
              <PlayCircle class="h-4 w-4" />学习
            </span>
          </button>
          <p v-if="!course.chapters?.length" class="text-slate-300">暂无章节</p>
        </div>
      </section>
    </div>

    <!-- 加载占位 -->
    <div v-else class="mx-auto max-w-6xl space-y-8 py-8">
      <div class="h-8 w-32 animate-pulse rounded-full bg-white/10" />
      <div class="grid gap-8 lg:grid-cols-[0.9fr_1.1fr]">
        <div class="min-h-80 animate-pulse rounded-[2rem] bg-white/10" />
        <div class="space-y-4 rounded-[2rem] border border-white/10 bg-white/8 p-8">
          <div class="h-4 w-24 animate-pulse rounded bg-white/10" />
          <div class="h-12 w-3/4 animate-pulse rounded-xl bg-white/10" />
          <div class="h-20 animate-pulse rounded-xl bg-white/10" />
        </div>
      </div>
    </div>
  </main>
</template>
