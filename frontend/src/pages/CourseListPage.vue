<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ChevronLeft, ChevronRight, Search } from 'lucide-vue-next'
import { categoryApi, courseApi, formatCourseStatus, type Category, type Course } from '@/lib/api'

const PAGE_SIZE = 12

const categories = ref<Category[]>([])
const courses = ref<Course[]>([])
const keyword = ref('')
const categoryId = ref<number | ''>('')
const loading = ref(false)
const currentPage = ref(1)
const totalPages = ref(1)
const total = ref(0)

async function load(page = 1) {
  loading.value = true
  currentPage.value = page
  try {
    const result = await courseApi.page({
      current: page,
      size: PAGE_SIZE,
      categoryId: categoryId.value,
      keyword: keyword.value,
      status: 'PUBLISHED',
    })
    courses.value = result.records
    total.value = result.total
    totalPages.value = Math.max(1, Math.ceil(result.total / PAGE_SIZE))
  } finally {
    loading.value = false
  }
}

// 分类变化自动触发
watch(categoryId, () => load(1))

function handleSearch() {
  load(1)
}

onMounted(async () => {
  categories.value = await categoryApi.list()
  await load(1)
})
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/" class="text-xl font-black">EduLive</RouterLink>
        <RouterLink to="/dashboard" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">工作台</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">课程中心</p>
        <h1 class="mt-2 text-4xl font-black">发现适合你的课程</h1>
        <p class="mt-2 text-sm text-slate-400">共 {{ total }} 门课程</p>
        <div class="mt-8 grid gap-4 md:grid-cols-[1fr_200px_100px]">
          <div class="relative">
            <Search class="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
            <input
              v-model="keyword"
              class="w-full rounded-2xl border border-white/10 bg-slate-900 py-3 pl-12 pr-4 outline-none focus:border-cyan-300"
              placeholder="搜索课程关键词"
              @keyup.enter="handleSearch"
            />
          </div>
          <select v-model="categoryId" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
            <option value="">全部分类</option>
            <option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
          <button class="rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200" @click="handleSearch">搜索</button>
        </div>
      </section>

      <!-- 课程网格 -->
      <section class="mt-8">
        <!-- 加载中骨架 -->
        <div v-if="loading" class="grid gap-5 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          <div v-for="i in 8" :key="i" class="rounded-[2rem] border border-white/10 bg-white/8 p-5">
            <div class="h-40 animate-pulse rounded-[1.5rem] bg-white/10" />
            <div class="mt-4 space-y-2">
              <div class="h-3 w-1/2 animate-pulse rounded bg-white/10" />
              <div class="h-6 animate-pulse rounded bg-white/10" />
              <div class="h-10 animate-pulse rounded bg-white/10" />
            </div>
          </div>
        </div>

        <div v-else-if="courses.length > 0" class="grid gap-5 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          <RouterLink
            v-for="course in courses"
            :key="course.id"
            :to="`/courses/${course.id}`"
            class="group rounded-[2rem] border border-white/10 bg-white/8 p-5 transition hover:-translate-y-1 hover:bg-white/12"
          >
            <div class="grid h-40 place-items-center rounded-[1.5rem] bg-gradient-to-br from-cyan-300 to-teal-300 text-5xl font-black text-slate-950">
              {{ course.title.slice(0, 1) }}
            </div>
            <div class="mt-5 flex items-center justify-between text-sm text-slate-400">
              <span>{{ course.categoryName }}</span>
              <span class="text-cyan-200 font-medium">{{ course.price > 0 ? `¥${course.price}` : '免费' }}</span>
            </div>
            <h2 class="mt-2 text-xl font-black leading-snug group-hover:text-cyan-200">{{ course.title }}</h2>
            <p class="mt-2 line-clamp-2 text-sm leading-6 text-slate-400">{{ course.intro || '暂无简介' }}</p>
            <div class="mt-4 flex items-center justify-between text-xs text-slate-500">
              <span>讲师：{{ course.teacherName }}</span>
              <span>{{ course.favoritesCount }} 收藏</span>
            </div>
          </RouterLink>
        </div>

        <div v-else class="mt-12 rounded-[2rem] border border-white/10 p-10 text-center text-slate-400">
          暂无课程，换个关键词试试
        </div>
      </section>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="mt-8 flex items-center justify-center gap-2">
        <button
          class="inline-flex h-9 w-9 items-center justify-center rounded-xl border border-white/10 hover:bg-white/10 disabled:opacity-40"
          :disabled="currentPage <= 1"
          @click="load(currentPage - 1)"
        >
          <ChevronLeft class="h-4 w-4" />
        </button>
        <button
          v-for="p in totalPages"
          :key="p"
          class="inline-flex h-9 w-9 items-center justify-center rounded-xl text-sm font-bold transition"
          :class="p === currentPage ? 'bg-cyan-300 text-slate-950' : 'border border-white/10 hover:bg-white/10'"
          @click="load(p)"
        >
          {{ p }}
        </button>
        <button
          class="inline-flex h-9 w-9 items-center justify-center rounded-xl border border-white/10 hover:bg-white/10 disabled:opacity-40"
          :disabled="currentPage >= totalPages"
          @click="load(currentPage + 1)"
        >
          <ChevronRight class="h-4 w-4" />
        </button>
      </div>
    </div>
  </main>
</template>
