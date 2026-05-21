<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ArrowRight, BookOpen, Radio, ShieldCheck, Users, Video } from 'lucide-vue-next'
import { RouterLink } from 'vue-router'
import { bannerApi, courseApi, liveApi, type Course, type LiveRoom, type SiteBanner } from '@/lib/api'
import AppNav from '@/components/layout/AppNav.vue'

const banners = ref<SiteBanner[]>([])
const courses = ref<Course[]>([])
const liveRooms = ref<LiveRoom[]>([])
const scheduledRooms = ref<LiveRoom[]>([])
const activeBanner = ref(0)
let autoTimer: ReturnType<typeof setInterval> | null = null

const fallbackBanners = computed<SiteBanner[]>(() => {
  const course = courses.value[0]
  const liveRoom = liveRooms.value[0] || scheduledRooms.value[0]
  return [
    {
      id: -1,
      title: course?.title || '精选热门课程',
      subtitle: course?.intro || '从入门到精通，名师亲授，实践驱动学习。',
      imageUrl: course?.cover || '',
      linkUrl: course ? `/courses/${course.id}` : '/courses',
      sort: 0,
      status: 'ENABLED',
    },
    {
      id: -2,
      title: liveRoom?.title || '实时直播课堂',
      subtitle: liveRoom?.intro || '参与聊天、弹幕互动，随时提问，沉浸式学习体验。',
      imageUrl: liveRoom?.cover || '',
      linkUrl: liveRoom ? `/live/${liveRoom.id}` : '/live',
      sort: 1,
      status: 'ENABLED',
    },
  ]
})

const displayBanners = computed(() => (banners.value.length ? banners.value : fallbackBanners.value))
const currentBanner  = computed(() => displayBanners.value[activeBanner.value] || displayBanners.value[0])

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
  banners.value   = bannerList
  courses.value   = coursePage.records
  liveRooms.value = livePage.records
  scheduledRooms.value = scheduledPage.records
  activeBanner.value = 0
  resetAutoPlay()
}

onMounted(loadHomeData)
onBeforeUnmount(() => { if (autoTimer) clearInterval(autoTimer) })
</script>

<template>
  <div class="min-h-screen bg-neutral-50">
    <AppNav />

    <!-- ═══════════════════════════════════════════════
        Hero — light gradient bg, dark text, no pure purple
        ═══════════════════════════════════════════════ -->
    <section class="relative overflow-hidden bg-gradient-to-br from-[#FAFBFF] to-[#F0F0FE]">
      <!-- Decorative geometric shapes -->
      <div class="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
        <div class="absolute -right-24 -top-24 h-96 w-96 rounded-full bg-primary-100/40 blur-3xl" />
        <div class="absolute -bottom-16 -left-16 h-64 w-64 rounded-full bg-primary-50/60 blur-2xl" />
        <div class="absolute right-1/3 top-1/2 h-32 w-32 rounded-full bg-primary-200/30 blur-2xl" />
      </div>

      <div class="relative mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8 lg:py-24">
        <div class="grid items-center gap-12 lg:grid-cols-2 lg:gap-16">

          <!-- Hero text -->
          <div class="animate-fade-in">
            <!-- Value tags -->
            <div class="mb-6 flex flex-wrap gap-2">
              <span class="inline-flex items-center gap-1.5 rounded-full border border-primary-200 bg-white px-3 py-1 text-xs font-medium text-primary-600 shadow-sm">
                <span class="live-dot" />实时互动直播
              </span>
              <span class="inline-flex items-center gap-1.5 rounded-full border border-neutral-200 bg-white px-3 py-1 text-xs font-medium text-neutral-600 shadow-sm">
                名师精品课程
              </span>
              <span class="inline-flex items-center gap-1.5 rounded-full border border-neutral-200 bg-white px-3 py-1 text-xs font-medium text-neutral-600 shadow-sm">
                闭环学习体系
              </span>
            </div>

            <h1 class="text-4xl font-semibold leading-tight text-neutral-900 md:text-5xl">
              把课程、直播、作业<br />
              <span class="text-primary-600">集中到一个教学现场</span>
            </h1>
            <p class="mt-5 text-base leading-7 text-neutral-600">
              支持教师开播授课、学生互动学习，覆盖在线教育平台从内容发布到直播互动的核心闭环。
            </p>

            <div class="mt-8 flex flex-wrap gap-3">
              <!-- Primary CTA -->
              <RouterLink
                to="/courses"
                class="inline-flex min-h-[44px] items-center gap-2 rounded-md bg-primary-600 px-6 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500"
              >
                浏览课程 <ArrowRight class="h-4 w-4" />
              </RouterLink>
              <!-- Secondary CTA -->
              <RouterLink
                to="/live"
                class="inline-flex min-h-[44px] items-center gap-2 rounded-md border border-primary-300 bg-white px-6 text-sm font-semibold text-primary-600 transition-all duration-200 hover:bg-primary-50"
              >
                <Radio class="h-4 w-4" />直播中心
              </RouterLink>
            </div>
          </div>

          <!-- Banner carousel -->
          <div class="relative">
            <RouterLink
              v-if="currentBanner"
              :to="currentBanner.linkUrl || '/courses'"
              class="group relative block overflow-hidden rounded-xl shadow-lg transition-all duration-300 hover:shadow-xl"
            >
              <img
                v-if="currentBanner.imageUrl"
                :src="currentBanner.imageUrl"
                :alt="currentBanner.title"
                class="aspect-video w-full object-cover transition-transform duration-500 group-hover:scale-[1.03]"
                loading="lazy"
              />
              <!-- Gradient placeholder -->
              <div
                v-else
                class="aspect-video w-full bg-gradient-to-br from-primary-100 via-primary-200 to-primary-300 transition duration-500 group-hover:brightness-105"
              />
              <div class="absolute inset-0 bg-gradient-to-t from-neutral-900/60 via-neutral-900/10 to-transparent" />
              <div class="absolute inset-x-0 bottom-0 p-5">
                <h3 class="text-lg font-semibold text-white">{{ currentBanner.title }}</h3>
                <p class="mt-1 line-clamp-1 text-sm text-white/75">{{ currentBanner.subtitle }}</p>
              </div>
            </RouterLink>
            <!-- Dot indicators -->
            <div v-if="displayBanners.length > 1" class="absolute bottom-4 right-4 flex gap-1.5">
              <button
                v-for="(_, index) in displayBanners"
                :key="index"
                type="button"
                :class="['h-1.5 rounded-full transition-all duration-300', index === activeBanner ? 'w-6 bg-white' : 'w-1.5 bg-white/50']"
                @click.prevent="setActiveBanner(index)"
              />
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════════════════════════════════════
        Stats strip — white container, 64px gap from hero
        ═══════════════════════════════════════════════ -->
    <div class="mx-auto mt-16 max-w-7xl px-4 sm:px-6 lg:px-8">
      <div class="overflow-hidden rounded-xl border border-border bg-white shadow-sm">
        <div class="grid grid-cols-3 divide-x divide-border">
          <div
            v-for="(stat, i) in [['500+', '门', '精品课程', '涵盖各类专业领域'], ['200+', '位', '认证讲师', '均为行业一线专家'], ['10,000+', '名', '学习用户', '正在平台持续成长']]"
            :key="i"
            class="flex flex-col items-center justify-center px-6 py-6 text-center sm:py-8"
          >
            <div class="flex items-baseline gap-1">
              <span class="text-3xl font-semibold text-neutral-900">{{ stat[0] }}</span>
              <span class="text-sm text-neutral-400">{{ stat[1] }}</span>
            </div>
            <p class="mt-1 text-sm font-medium text-neutral-600">{{ stat[2] }}</p>
            <p class="mt-0.5 text-xs text-neutral-400">{{ stat[3] }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══════════════════════════════════════════════
        Hot courses
        ═══════════════════════════════════════════════ -->
    <section class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
      <div class="flex items-center justify-between">
        <h2 class="text-xl font-semibold text-neutral-900">热门课程</h2>
        <RouterLink
          to="/courses"
          class="flex items-center gap-1 text-sm font-medium text-primary-600 transition-colors hover:text-primary-500"
        >
          查看更多 <ArrowRight class="h-3.5 w-3.5" />
        </RouterLink>
      </div>

      <div class="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        <RouterLink
          v-for="course in courses"
          :key="course.id"
          :to="`/courses/${course.id}`"
          class="group overflow-hidden rounded-xl border border-border bg-white shadow-sm transition-all duration-200 hover:shadow-md hover:-translate-y-px"
        >
          <!-- Course cover -->
          <div class="relative aspect-video overflow-hidden bg-primary-50">
            <img
              v-if="course.cover"
              :src="course.cover"
              :alt="course.title"
              class="h-full w-full object-cover transition-transform duration-300 group-hover:scale-[1.03]"
              loading="lazy"
            />
            <div
              v-else
              class="flex h-full w-full items-center justify-center bg-gradient-to-br from-primary-100 to-primary-200 text-3xl font-semibold text-primary-600"
            >
              {{ course.title.slice(0, 1) }}
            </div>
          </div>

          <div class="p-4">
            <p class="text-xs text-neutral-400">{{ course.categoryName }}</p>
            <h3 class="mt-1 font-semibold text-neutral-900 transition-colors group-hover:text-primary-600">
              {{ course.title }}
            </h3>
            <p class="mt-1 line-clamp-2 text-xs leading-5 text-neutral-500">{{ course.intro || '暂无简介' }}</p>

            <div class="mt-3 flex items-center justify-between">
              <div class="flex items-center gap-1.5">
                <div class="grid h-5 w-5 place-items-center overflow-hidden rounded-full bg-primary-100 text-[9px] font-semibold text-primary-700">
                  {{ course.teacherName?.slice(0, 1) }}
                </div>
                <span class="text-xs text-neutral-400">{{ course.teacherName }}</span>
              </div>
              <span class="rounded-md bg-primary-50 px-2 py-0.5 text-xs font-semibold text-primary-700">
                {{ course.price > 0 ? `¥${course.price}` : '免费' }}
              </span>
            </div>
          </div>
        </RouterLink>
      </div>
    </section>

    <!-- ═══════════════════════════════════════════════
        Live rooms
        ═══════════════════════════════════════════════ -->
    <section
      v-if="liveRooms.length > 0 || scheduledRooms.length > 0"
      class="border-t border-border bg-white py-16"
    >
      <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between">
          <h2 class="text-xl font-semibold text-neutral-900">正在直播</h2>
          <RouterLink
            to="/live"
            class="flex items-center gap-1 text-sm font-medium text-primary-600 transition-colors hover:text-primary-500"
          >
            查看更多 <ArrowRight class="h-3.5 w-3.5" />
          </RouterLink>
        </div>

        <div class="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <RouterLink
            v-for="room in [...liveRooms, ...scheduledRooms].slice(0, 4)"
            :key="room.id"
            :to="`/live/${room.id}`"
            class="group overflow-hidden rounded-xl border border-border bg-neutral-50 transition-all duration-200 hover:shadow-md hover:-translate-y-px"
          >
            <!-- Cover with LIVE badge -->
            <div class="relative aspect-video overflow-hidden bg-neutral-200">
              <img
                v-if="room.cover"
                :src="room.cover"
                :alt="room.title"
                class="h-full w-full object-cover"
                loading="lazy"
              />
              <div
                v-else
                class="flex h-full w-full items-center justify-center bg-gradient-to-br from-neutral-100 to-neutral-200"
              >
                <Radio class="h-8 w-8 text-neutral-400" stroke-width="1.5" />
              </div>
              <!-- LIVE badge -->
              <div
                v-if="room.status === 'LIVE'"
                class="absolute left-2 top-2 flex items-center gap-1.5 rounded-md bg-danger px-2 py-0.5 text-[11px] font-bold text-white"
              >
                <span class="live-dot h-1.5 w-1.5 bg-white" />LIVE
              </div>
              <div
                v-else
                class="absolute left-2 top-2 rounded-md bg-warning px-2 py-0.5 text-[11px] font-bold text-white"
              >
                即将开始
              </div>
            </div>

            <div class="p-3">
              <h3 class="truncate text-sm font-semibold text-neutral-800 transition-colors group-hover:text-primary-600">
                {{ room.title }}
              </h3>
              <div class="mt-1.5 flex items-center justify-between">
                <span class="text-xs text-neutral-400">{{ room.teacherName }}</span>
                <div class="flex items-center gap-1 text-xs text-neutral-400">
                  <Users class="h-3.5 w-3.5" stroke-width="1.5" />
                  {{ room.onlineCount }}
                </div>
              </div>
            </div>
          </RouterLink>
        </div>
      </div>
    </section>

    <!-- ═══════════════════════════════════════════════
        Feature cards — single color icon system
        ═══════════════════════════════════════════════ -->
    <section class="py-16">
      <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div class="grid gap-4 sm:grid-cols-3">
          <div
            v-for="(feat, i) in [
              { icon: ShieldCheck, title: '三角色权限', desc: '管理员、教师、学生分端使用，接口按角色鉴权，安全可靠。' },
              { icon: Radio,       title: '直播互动',   desc: '实时音视频结合 WebSocket 聊天、弹幕、投票和连麦功能。' },
              { icon: BookOpen,    title: '课程闭环',   desc: '课程审核、章节视频、直播回放、作业和学习进度统一管理。' },
            ]"
            :key="i"
            class="rounded-xl border border-border bg-white p-5 shadow-sm"
          >
            <div class="mb-3 grid h-10 w-10 place-items-center rounded-lg bg-primary-50 text-primary-600">
              <component :is="feat.icon" class="h-5 w-5" stroke-width="1.75" />
            </div>
            <h3 class="font-semibold text-neutral-900">{{ feat.title }}</h3>
            <p class="mt-1.5 text-sm text-neutral-500">{{ feat.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="border-t border-border bg-white py-8">
      <div class="mx-auto max-w-7xl px-4 text-center sm:px-6 lg:px-8">
        <div class="flex items-center justify-center gap-2">
          <div class="grid h-7 w-7 place-items-center rounded-lg bg-primary-600 text-white">
            <Video class="h-3.5 w-3.5" />
          </div>
          <span class="font-semibold text-neutral-900">EduLive</span>
        </div>
        <p class="mt-3 text-sm text-neutral-400">让学习更高效，让知识有价值。© 2025 EduLive.</p>
      </div>
    </footer>
  </div>
</template>
