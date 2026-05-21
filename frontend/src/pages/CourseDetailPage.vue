<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { BookOpen, ChevronLeft, Heart, PlayCircle, ShoppingCart } from 'lucide-vue-next'
import { courseApi, getToken, orderApi, type Chapter, type Course } from '@/lib/api'
import { useAuth } from '@/composables/useAuth'
import AppLayout from '@/components/layout/AppLayout.vue'

const route   = useRoute()
const auth    = useAuth()
const course  = ref<Course | null>(null)
const activeChapter    = ref<Chapter | null>(null)
const message          = ref('')
const messageType      = ref<'success' | 'error'>('success')
const buying           = ref(false)
const favoriteLoading  = ref(false)
const canPlay          = computed(() => course.value?.accessible !== false)

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
  messageType.value = 'success'
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
  const map: Record<string, string> = { DRAFT: '草稿', REVIEWING: '审核中', PUBLISHED: '已发布', REJECTED: '已退回', OFFLINE: '已下线' }
  return map[status] ?? status
}

async function load() {
  course.value = await courseApi.detail(Number(route.params.id))
  activeChapter.value = course.value.chapters?.find((c) => c.videoPath) || course.value.chapters?.[0] || null
}

async function toggleFavorite() {
  if (favoriteLoading.value) return
  if (!course.value || !getToken()) { message.value = '请先登录后收藏'; messageType.value = 'error'; return }
  if (auth.state.user?.role !== 'STUDENT') { message.value = '仅学生账号可收藏课程'; messageType.value = 'error'; return }
  favoriteLoading.value = true
  try {
    if (course.value.favorite) {
      await courseApi.unfavorite(course.value.id); message.value = '已取消收藏'
    } else {
      await courseApi.favorite(course.value.id); message.value = '收藏成功'
    }
    messageType.value = 'success'
    await load()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '操作失败，请稍后重试'
    messageType.value = 'error'
  } finally {
    favoriteLoading.value = false
  }
}

async function buyCourse() {
  if (!course.value || !getToken()) { message.value = '请先登录后购买'; messageType.value = 'error'; return }
  buying.value = true; message.value = ''
  try {
    const order = await orderApi.create(course.value.id)
    await orderApi.pay(order.id)
    message.value = '购买成功！正在加载课程内容…'
    messageType.value = 'success'
    await load()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '购买失败，请稍后重试'
    messageType.value = 'error'
  } finally {
    buying.value = false
  }
}

function selectChapter(chapter: Chapter) { activeChapter.value = chapter; message.value = '' }

onMounted(load)
</script>

<template>
  <AppLayout>
    <div class="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">

      <!-- Breadcrumb -->
      <RouterLink
        to="/courses"
        class="mb-6 inline-flex items-center gap-1 text-sm text-neutral-500 transition-colors hover:text-primary-600"
      >
        <ChevronLeft class="h-4 w-4" />返回课程中心
      </RouterLink>

      <!-- Skeleton -->
      <div v-if="!course" class="space-y-6">
        <div class="grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
          <div class="min-h-64 animate-pulse rounded-xl bg-neutral-100" />
          <div class="space-y-4 rounded-xl border border-border bg-white p-6 shadow-sm">
            <div class="h-3 w-24 animate-pulse rounded bg-neutral-100" />
            <div class="h-8 w-3/4 animate-pulse rounded bg-neutral-100" />
            <div class="h-16 animate-pulse rounded bg-neutral-100" />
          </div>
        </div>
      </div>

      <template v-else>
        <!-- Course header -->
        <section class="mb-6 grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
          <!-- Cover -->
          <div class="flex min-h-60 items-center justify-center overflow-hidden rounded-xl bg-gradient-to-br from-primary-100 to-primary-200 shadow-sm">
            <img
              v-if="course.cover"
              :src="course.cover"
              :alt="course.title"
              class="h-full w-full object-cover"
              loading="lazy"
            />
            <span v-else class="text-7xl font-semibold text-primary-600">{{ course.title.slice(0, 1) }}</span>
          </div>

          <!-- Info card -->
          <div class="rounded-xl border border-border bg-white p-6 shadow-sm">
            <div class="flex items-center gap-2 text-xs text-neutral-400">
              <span>{{ course.categoryName }}</span>
              <span>·</span>
              <span>{{ formatCourseStatus(course.status) }}</span>
            </div>
            <h1 class="mt-2 text-2xl font-semibold text-neutral-900">{{ course.title }}</h1>
            <p class="mt-3 text-sm leading-7 text-neutral-600">{{ course.intro || '暂无课程简介' }}</p>

            <div class="mt-5 flex flex-wrap gap-2">
              <span class="rounded-md bg-neutral-100 px-3 py-1.5 text-xs text-neutral-600">讲师 {{ course.teacherName }}</span>
              <span class="rounded-md bg-neutral-100 px-3 py-1.5 text-xs text-neutral-600">{{ course.favoritesCount }} 人收藏</span>
              <span class="rounded-md bg-primary-50 px-3 py-1.5 text-xs font-semibold text-primary-700">
                {{ course.price > 0 ? `¥${course.price}` : '免费' }}
              </span>
            </div>

            <div class="mt-5 flex flex-wrap gap-3">
              <button
                v-if="course.price > 0 && !course.accessible"
                :disabled="buying"
                class="inline-flex min-h-[44px] items-center gap-2 rounded-md bg-primary-600 px-5 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500 disabled:opacity-60"
                @click="buyCourse"
              >
                <ShoppingCart class="h-4 w-4" />
                {{ buying ? '处理中…' : `立即购买 ¥${course.price}` }}
              </button>
              <button
                :disabled="favoriteLoading"
                class="inline-flex min-h-[44px] items-center gap-2 rounded-md border border-border px-5 text-sm font-medium text-neutral-700 transition-all duration-200 hover:bg-neutral-100 disabled:opacity-60"
                @click="toggleFavorite"
              >
                <Heart class="h-4 w-4" :class="course.favorite ? 'fill-danger text-danger' : ''" />
                {{ favoriteLoading ? '处理中…' : course.favorite ? '取消收藏' : '收藏课程' }}
              </button>
            </div>

            <div
              v-if="message"
              class="mt-3 rounded-md px-3 py-2 text-sm"
              :class="messageType === 'error' ? 'bg-danger-bg text-danger' : 'bg-success-bg text-success'"
            >{{ message }}</div>
          </div>
        </section>

        <!-- Video player -->
        <section class="mb-6 rounded-xl border border-border bg-white p-5 shadow-sm">
          <div class="mb-4 flex items-center gap-2">
            <PlayCircle class="h-5 w-5 text-primary-600" />
            <h2 class="font-semibold text-neutral-900">章节视频</h2>
          </div>
          <video
            v-if="activeChapter?.videoPath && canPlay"
            class="w-full rounded-lg bg-black"
            controls
            :src="courseApi.chapterVideoUrl(course.id, activeChapter.id)"
            @pause="saveProgress"
            @ended="saveProgress"
            @timeupdate="saveProgress"
          />
          <div v-else class="rounded-lg bg-neutral-50 p-8 text-center">
            <p class="text-sm text-neutral-500">
              {{ canPlay ? '课程暂未配置可播放章节，请关注后续更新' : '该课程需要购买后才能观看' }}
            </p>
            <button
              v-if="!canPlay && course.price > 0"
              :disabled="buying"
              class="mt-4 inline-flex min-h-[44px] items-center gap-2 rounded-md bg-primary-600 px-5 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500 disabled:opacity-60"
              @click="buyCourse"
            >
              <ShoppingCart class="h-4 w-4" />
              {{ buying ? '处理中…' : `立即购买 ¥${course.price}` }}
            </button>
          </div>
        </section>

        <!-- Chapter list -->
        <section class="rounded-xl border border-border bg-white p-5 shadow-sm">
          <div class="mb-4 flex items-center gap-2">
            <BookOpen class="h-5 w-5 text-primary-600" />
            <h2 class="font-semibold text-neutral-900">章节目录</h2>
          </div>
          <div class="space-y-2">
            <button
              v-for="(chapter, idx) in course.chapters"
              :key="chapter.id"
              class="flex w-full items-center justify-between rounded-lg border px-4 py-3 text-left transition-all duration-200"
              :class="activeChapter?.id === chapter.id
                ? 'border-primary-300 bg-primary-50'
                : 'border-border hover:bg-neutral-50'"
              @click="selectChapter(chapter)"
            >
              <div>
                <p
                  class="text-sm font-medium"
                  :class="activeChapter?.id === chapter.id ? 'text-primary-700' : 'text-neutral-800'"
                >第{{ idx + 1 }}章 · {{ chapter.title }}</p>
                <p class="mt-0.5 text-xs text-neutral-400">
                  时长 {{ formatDuration(chapter.duration) }} · {{ chapter.videoPath ? '可播放' : '暂无视频' }}
                </p>
              </div>
              <span class="flex items-center gap-1 rounded-md bg-primary-50 px-3 py-1 text-xs font-medium text-primary-600">
                <PlayCircle class="h-3.5 w-3.5" />学习
              </span>
            </button>

            <div v-if="!course.chapters?.length" class="rounded-lg bg-neutral-50 p-6 text-center text-sm text-neutral-500">
              课程暂未配置章节，请关注后续更新
            </div>
          </div>
        </section>
      </template>
    </div>
  </AppLayout>
</template>
