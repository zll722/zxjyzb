<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ChevronLeft, ChevronRight, Search } from 'lucide-vue-next'
import { categoryApi, courseApi, type Category, type Course } from '@/lib/api'
import AppLayout from '@/components/layout/AppLayout.vue'
import Empty from '@/components/Empty.vue'

const PAGE_SIZE = 12

const categories = ref<Category[]>([])
const courses    = ref<Course[]>([])
const keyword    = ref('')
const categoryId = ref<number | ''>('')
const loading    = ref(false)
const currentPage = ref(1)
const totalPages  = ref(1)
const total       = ref(0)

async function load(page = 1) {
  loading.value = true
  currentPage.value = page
  try {
    const result = await courseApi.page({
      current: page, size: PAGE_SIZE,
      categoryId: categoryId.value,
      keyword: keyword.value,
      status: 'PUBLISHED',
    })
    courses.value   = result.records
    total.value     = result.total
    totalPages.value = Math.max(1, Math.ceil(result.total / PAGE_SIZE))
  } finally {
    loading.value = false
  }
}

watch(categoryId, () => load(1))
function handleSearch() { load(1) }

onMounted(async () => {
  categories.value = await categoryApi.list()
  await load(1)
})
</script>

<template>
  <AppLayout>
    <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">

      <!-- Page header -->
      <div class="mb-6">
        <p class="text-sm font-medium text-neutral-400">课程中心</p>
        <h1 class="mt-1 text-2xl font-semibold text-neutral-900">发现适合你的课程</h1>
        <p class="mt-1 text-sm text-neutral-500">共 {{ total }} 门课程</p>
      </div>

      <!-- Search & filter bar -->
      <div class="mb-5 flex flex-wrap gap-3">
        <div class="relative min-w-52 flex-1">
          <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-neutral-400" />
          <input
            v-model="keyword"
            class="h-10 w-full rounded-md border border-border bg-white pl-9 pr-3 text-sm text-neutral-900 outline-none transition-all duration-200 placeholder:text-neutral-400 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15"
            placeholder="搜索课程关键词"
            @keyup.enter="handleSearch"
          />
        </div>
        <select
          v-model="categoryId"
          class="h-10 rounded-md border border-border bg-white px-3 text-sm text-neutral-700 outline-none transition-all duration-200 focus:border-primary-600"
        >
          <option value="">全部分类</option>
          <option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option>
        </select>
        <button
          class="inline-flex h-10 items-center gap-1.5 rounded-md bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500"
          @click="handleSearch"
        >
          <Search class="h-4 w-4" />搜索
        </button>
      </div>

      <!-- Category pills -->
      <div v-if="categories.length" class="mb-6 flex flex-wrap gap-2">
        <button
          class="rounded-full px-3 py-1 text-xs font-medium transition-all duration-200"
          :class="categoryId === '' ? 'bg-primary-600 text-white' : 'border border-border text-neutral-600 hover:bg-neutral-100'"
          @click="categoryId = ''"
        >全部</button>
        <button
          v-for="cat in categories"
          :key="cat.id"
          class="rounded-full px-3 py-1 text-xs font-medium transition-all duration-200"
          :class="categoryId === cat.id ? 'bg-primary-600 text-white' : 'border border-border text-neutral-600 hover:bg-neutral-100'"
          @click="categoryId = cat.id"
        >{{ cat.name }}</button>
      </div>

      <!-- Skeleton -->
      <div v-if="loading" class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        <div v-for="i in 8" :key="i" class="overflow-hidden rounded-xl border border-border bg-white shadow-sm">
          <div class="aspect-video animate-pulse bg-neutral-100" />
          <div class="space-y-2 p-4">
            <div class="h-3 w-1/3 animate-pulse rounded bg-neutral-100" />
            <div class="h-4 animate-pulse rounded bg-neutral-100" />
            <div class="h-8 animate-pulse rounded bg-neutral-100" />
          </div>
        </div>
      </div>

      <!-- Course grid -->
      <div v-else-if="courses.length > 0" class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        <RouterLink
          v-for="course in courses"
          :key="course.id"
          :to="`/courses/${course.id}`"
          class="group overflow-hidden rounded-xl border border-border bg-white shadow-sm transition-all duration-200 hover:shadow-md hover:-translate-y-px"
        >
          <!-- Cover 16:9 -->
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
            >{{ course.title.slice(0, 1) }}</div>
          </div>

          <div class="p-4">
            <p class="text-xs text-neutral-400">{{ course.categoryName }}</p>
            <h2 class="mt-1 font-semibold text-neutral-900 transition-colors group-hover:text-primary-600">
              {{ course.title }}
            </h2>
            <p class="mt-1.5 line-clamp-2 text-xs leading-5 text-neutral-500">{{ course.intro || '暂无简介' }}</p>
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

      <Empty v-else title="暂无课程" description="换个关键词或分类试试" />

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="mt-8 flex items-center justify-center gap-1">
        <button
          class="inline-flex h-9 w-9 items-center justify-center rounded-md border border-border text-neutral-500 transition-all duration-200 hover:bg-neutral-100 disabled:opacity-40"
          :disabled="currentPage <= 1"
          @click="load(currentPage - 1)"
        ><ChevronLeft class="h-4 w-4" /></button>
        <button
          v-for="p in totalPages"
          :key="p"
          class="inline-flex h-9 w-9 items-center justify-center rounded-md text-sm font-medium transition-all duration-200"
          :class="p === currentPage ? 'bg-primary-600 text-white' : 'border border-border text-neutral-600 hover:bg-neutral-100'"
          @click="load(p)"
        >{{ p }}</button>
        <button
          class="inline-flex h-9 w-9 items-center justify-center rounded-md border border-border text-neutral-500 transition-all duration-200 hover:bg-neutral-100 disabled:opacity-40"
          :disabled="currentPage >= totalPages"
          @click="load(currentPage + 1)"
        ><ChevronRight class="h-4 w-4" /></button>
      </div>

    </div>
  </AppLayout>
</template>
