<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { ArrowRight, LayoutDashboard, LogOut, Radio, ShieldCheck, Sparkles, Video } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { bannerApi, courseApi, liveApi, type Course, type LiveRoom, type SiteBanner } from '@/lib/api'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const auth = useAuth()
const banners = ref<SiteBanner[]>([])
const courses = ref<Course[]>([])
const liveRooms = ref<LiveRoom[]>([])
const scheduledRooms = ref<LiveRoom[]>([])
const activeBanner = ref(0)
let autoTimer: ReturnType<typeof setInterval> | null = null

function imageByPrompt(prompt: string, imageSize = 'landscape_16_9') {
  return `https://coreva-normal.trae.ai/api/ide/v1/text_to_image?prompt=${encodeURIComponent(prompt)}&image_size=${imageSize}`
}

const fallbackBanners = computed<SiteBanner[]>(() => {
  const course = courses.value[0]
  const liveRoom = liveRooms.value[0] || scheduledRooms.value[0]
  return [
    {
      id: -1,
      title: course?.title || '热门课程推荐',
      subtitle: course?.intro || '精选高质量课程，覆盖课程、直播、作业与学习进度闭环。',
      imageUrl: course?.cover || imageByPrompt('modern online education platform hero banner, teacher livestream class, students learning dashboards, dark blue glassmorphism, cinematic lighting'),
      linkUrl: course ? `/courses/${course.id}` : '/courses',
      sort: 0,
      status: 'ENABLED',
    },
    {
      id: -2,
      title: liveRoom?.title || '实时直播课堂',
      subtitle: liveRoom?.intro || '进入直播中心，参与聊天、弹幕、投票和连麦互动。',
      imageUrl: liveRoom?.cover || imageByPrompt('realistic online livestream classroom banner, teacher presenting screen share, interactive chat bubbles, cyan neon, dark technology style'),
      linkUrl: liveRoom ? `/live/${liveRoom.id}` : '/live',
      sort: 1,
      status: 'ENABLED',
    },
  ]
})

const displayBanners = computed(() => (banners.value.length ? banners.value : fallbackBanners.value))
const currentBanner = computed(() => displayBanners.value[activeBanner.value] || displayBanners.value[0])

function setActiveBanner(index: number) {
  activeBanner.value = index
  resetAutoPlay()
}

function nextBanner() {
  activeBanner.value = (activeBanner.value + 1) % displayBanners.value.length
}

function resetAutoPlay() {
  if (autoTimer) clearInterval(autoTimer)
  autoTimer = setInterval(nextBanner, 5000)
}

async function loadHomeData() {
  const [bannerList, coursePage, livePage, scheduledPage] = await Promise.all([
    bannerApi.list().catch(() => []),
    courseApi.page({ current: 1, size: 8, status: 'PUBLISHED' }).catch(() => ({ records: [], total: 0, size: 8, current: 1 })),
    liveApi.page({ current: 1, size: 4, status: 'LIVE' }).catch(() => ({ records: [], total: 0, size: 4, current: 1 })),
    liveApi.page({ current: 1, size: 4, status: 'SCHEDULED' }).catch(() => ({ records: [], total: 0, size: 4, current: 1 })),
  ])
  banners.value = bannerList
  courses.value = coursePage.records
  liveRooms.value = livePage.records
  scheduledRooms.value = scheduledPage.records
  activeBanner.value = 0
  resetAutoPlay()
}

function logout() {
  auth.logout()
  router.push('/')
}

onMounted(loadHomeData)
onBeforeUnmount(() => { if (autoTimer) clearInterval(autoTimer) })
</script>

<template>
  <main class="min-h-screen overflow-hidden bg-[#07111f] text-white">
    <section class="relative mx-auto flex min-h-screen max-w-7xl flex-col px-6 py-8 lg:px-8">
      <div class="absolute inset-0 -z-0 bg-[radial-gradient(circle_at_20%_20%,rgba(56,189,248,0.28),transparent_32%),radial-gradient(circle_at_80%_30%,rgba(20,184,166,0.2),transparent_28%),linear-gradient(135deg,#07111f_0%,#0f172a_55%,#111827_100%)]" />

      <!-- 顶部导航 -->
      <nav class="relative z-10 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="grid h-11 w-11 place-items-center rounded-2xl bg-cyan-400 text-slate-950 shadow-lg shadow-cyan-500/30">
            <Video class="h-6 w-6" />
          </div>
          <div>
            <p class="text-sm text-cyan-200">EduLive</p>
            <h1 class="text-lg font-semibold tracking-wide">在线教育直播平台</h1>
          </div>
        </div>

        <!-- 未登录：显示登录/注册 -->
        <div v-if="!auth.state.user" class="flex items-center gap-3">
          <RouterLink to="/login" class="rounded-full px-5 py-2 text-sm text-slate-200 transition hover:bg-white/10">登录</RouterLink>
          <RouterLink to="/register" class="rounded-full bg-white px-5 py-2 text-sm font-semibold text-slate-950 transition hover:bg-cyan-100">注册</RouterLink>
        </div>

        <!-- 已登录：显示用户信息 + 工作台 -->
        <div v-else class="flex items-center gap-3">
          <span class="hidden text-sm text-slate-300 sm:block">{{ auth.state.user.username }}</span>
          <RouterLink to="/dashboard" class="inline-flex items-center gap-2 rounded-full bg-white/10 px-5 py-2 text-sm font-semibold text-white transition hover:bg-white/20">
            <LayoutDashboard class="h-4 w-4" />工作台
          </RouterLink>
          <button class="inline-flex items-center gap-1 rounded-full px-3 py-2 text-sm text-slate-400 transition hover:text-white" @click="logout">
            <LogOut class="h-4 w-4" />
          </button>
        </div>
      </nav>

      <!-- 主体内容 -->
      <div class="relative z-10 grid flex-1 items-center gap-12 py-16 lg:grid-cols-[1.05fr_0.95fr]">
        <div>
          <div class="mb-8 inline-flex items-center gap-2 rounded-full border border-cyan-300/20 bg-white/8 px-4 py-2 text-sm text-cyan-100 backdrop-blur">
            <Sparkles class="h-4 w-4" />
            Vue3 + Spring Boot + Agora 实时直播教学
          </div>
          <h2 class="max-w-4xl text-5xl font-black leading-tight tracking-tight md:text-7xl">
            把课程、直播、作业与管理集中到一个教学现场
          </h2>
          <p class="mt-6 max-w-2xl text-lg leading-8 text-slate-300">
            支持教师开播授课、学生互动学习、管理员审核监管，覆盖在线教育平台从内容发布到直播互动的核心闭环。
          </p>
          <div class="mt-10 flex flex-wrap gap-4">
            <RouterLink
              v-if="!auth.state.user"
              to="/register"
              class="inline-flex items-center gap-2 rounded-full bg-cyan-300 px-7 py-3 font-semibold text-slate-950 shadow-xl shadow-cyan-500/20 transition hover:-translate-y-0.5 hover:bg-cyan-200"
            >
              开始使用 <ArrowRight class="h-5 w-5" />
            </RouterLink>
            <RouterLink
              to="/courses"
              class="inline-flex items-center gap-2 rounded-full border border-white/15 px-7 py-3 font-semibold text-white transition hover:bg-white/10"
            >
              浏览课程
            </RouterLink>
            <RouterLink
              to="/live"
              class="inline-flex items-center gap-2 rounded-full border border-white/15 px-7 py-3 font-semibold text-white transition hover:bg-white/10"
            >
              <Radio class="h-4 w-4" />直播中心
            </RouterLink>
          </div>
        </div>

        <!-- 轮播图 -->
        <div class="relative">
          <RouterLink
            v-if="currentBanner"
            :to="currentBanner.linkUrl || '/courses'"
            class="group relative block overflow-hidden rounded-[2rem] border border-white/10 bg-white/10 shadow-2xl shadow-black/30 backdrop-blur-xl"
          >
            <img :src="currentBanner.imageUrl" :alt="currentBanner.title" class="h-[30rem] w-full object-cover opacity-85 transition duration-500 group-hover:scale-105" />
            <div class="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/25 to-transparent" />
            <div class="absolute inset-x-0 bottom-0 p-7">
              <h3 class="text-3xl font-black">{{ currentBanner.title }}</h3>
              <p class="mt-3 line-clamp-2 text-slate-200">{{ currentBanner.subtitle }}</p>
              <div class="mt-5 flex items-center gap-2 text-sm font-bold text-cyan-100">
                查看详情 <ArrowRight class="h-4 w-4 transition group-hover:translate-x-1" />
              </div>
            </div>
          </RouterLink>

          <!-- 轮播指示器 -->
          <div v-if="displayBanners.length > 1" class="absolute bottom-10 right-7 flex gap-2">
            <button
              v-for="(_, index) in displayBanners"
              :key="index"
              type="button"
              :class="['h-2 rounded-full transition-all duration-300', index === activeBanner ? 'w-8 bg-cyan-300' : 'w-2 bg-white/40 hover:bg-white/60']"
              @click.prevent="setActiveBanner(index)"
            />
          </div>
        </div>
      </div>

      <!-- 底部特性卡片 -->
      <div class="relative z-10 grid gap-4 pb-8 md:grid-cols-3">
        <div class="rounded-3xl border border-white/10 bg-white/8 p-5 backdrop-blur">
          <ShieldCheck class="mb-4 h-7 w-7 text-cyan-200" />
          <h3 class="font-semibold">三角色权限</h3>
          <p class="mt-2 text-sm text-slate-300">管理员、教师、学生分端使用，接口按角色鉴权。</p>
        </div>
        <div class="rounded-3xl border border-white/10 bg-white/8 p-5 backdrop-blur">
          <Radio class="mb-4 h-7 w-7 text-cyan-200" />
          <h3 class="font-semibold">直播互动</h3>
          <p class="mt-2 text-sm text-slate-300">Agora 音视频结合 WebSocket 聊天、弹幕、投票和连麦。</p>
        </div>
        <div class="rounded-3xl border border-white/10 bg-white/8 p-5 backdrop-blur">
          <Video class="mb-4 h-7 w-7 text-cyan-200" />
          <h3 class="font-semibold">课程闭环</h3>
          <p class="mt-2 text-sm text-slate-300">课程审核、章节视频、回放、作业和学习进度统一管理。</p>
        </div>
      </div>
    </section>
  </main>
</template>
