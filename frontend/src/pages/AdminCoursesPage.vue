<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { CheckCircle2, Search, XCircle } from 'lucide-vue-next'
import { adminApi, type Course, type PageResult } from '@/lib/api'

const filters = reactive({ status: '' })
const page    = ref<PageResult<Course>>({ records: [], total: 0, size: 10, current: 1 })
const loading = ref(false)
const message = ref('')
const error   = ref('')

const statusLabels: Record<string, string> = {
  DRAFT:     '草稿',
  REVIEWING: '待审核',
  PUBLISHED: '已发布',
  OFFLINE:   '已下架',
}

async function load(current = 1) {
  loading.value = true; error.value = ''
  try { page.value = await adminApi.courses({ current, size: page.value.size, status: filters.status }) }
  catch (err) { error.value = err instanceof Error ? err.message : '课程列表加载失败' }
  finally { loading.value = false }
}

async function review(course: Course, approved: boolean) {
  await adminApi.reviewCourse(course.id, approved)
  message.value = approved ? '课程已审核通过' : '课程已下架'
  await load(page.value.current)
}

function formatTime(value?: string) { return value ? new Date(value).toLocaleString() : '-' }

function statusCls(status: string) {
  if (status === 'PUBLISHED') return 'bg-success-bg text-success'
  if (status === 'OFFLINE')   return 'bg-danger-bg text-danger'
  if (status === 'REVIEWING') return 'bg-warning-bg text-warning'
  return 'bg-neutral-100 text-neutral-500'
}

onMounted(() => load())
</script>

<template>
  <div class="mx-auto max-w-7xl">
    <div class="mb-6">
      <p class="text-sm font-medium text-neutral-400">管理员课程审核</p>
      <h1 class="mt-1 text-2xl font-semibold text-neutral-900">课程发布审核与下架</h1>
    </div>

    <!-- Filter bar -->
    <div class="mb-6 rounded-xl border border-border bg-white p-4 shadow-sm">
      <form class="flex flex-wrap gap-3" @submit.prevent="load(1)">
        <select
          v-model="filters.status"
          class="h-10 w-40 rounded-md border border-border bg-white px-3 text-sm outline-none transition-all duration-200 focus:border-primary-600"
        >
          <option value="">全部状态</option>
          <option value="DRAFT">草稿</option>
          <option value="REVIEWING">待审核</option>
          <option value="PUBLISHED">已发布</option>
          <option value="OFFLINE">已下架</option>
        </select>
        <button class="inline-flex h-10 items-center gap-1.5 rounded-md bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500">
          <Search class="h-4 w-4" />筛选
        </button>
      </form>
      <div v-if="message" class="mt-3 rounded-md bg-success-bg px-3 py-2 text-sm text-success">{{ message }}</div>
      <div v-if="error"   class="mt-3 rounded-md bg-danger-bg  px-3 py-2 text-sm text-danger">{{ error }}</div>
    </div>

    <!-- Course table -->
    <div class="overflow-hidden rounded-xl border border-border bg-white shadow-sm">
      <div class="grid grid-cols-[1.5fr_130px_120px_160px_160px] gap-4 border-b border-border bg-neutral-50 px-4 py-3 text-xs font-medium text-neutral-500">
        <span>课程</span><span>教师</span><span>分类</span><span>创建时间</span><span>操作</span>
      </div>
      <p v-if="loading"               class="px-4 py-6 text-sm text-neutral-400">课程加载中…</p>
      <p v-else-if="!page.records.length" class="px-4 py-6 text-sm text-neutral-400">暂无符合条件的课程</p>
      <article
        v-for="course in page.records"
        v-else
        :key="course.id"
        class="grid grid-cols-[1.5fr_130px_120px_160px_160px] items-center gap-4 border-b border-border px-4 py-3 last:border-b-0 hover:bg-neutral-50/80"
      >
        <div>
          <p class="font-medium text-neutral-900">{{ course.title }}</p>
          <p class="mt-0.5 line-clamp-1 text-xs text-neutral-400">{{ course.intro || '暂无简介' }}</p>
          <span class="mt-1 inline-flex rounded-full px-2 py-0.5 text-xs font-medium" :class="statusCls(course.status)">
            {{ statusLabels[course.status] || course.status }}
          </span>
        </div>
        <p class="text-sm text-neutral-500">{{ course.teacherName || '-' }}</p>
        <p class="text-sm text-neutral-500">{{ course.categoryName || '-' }}</p>
        <p class="text-xs text-neutral-400">{{ formatTime(course.createdAt) }}</p>
        <div class="flex flex-wrap gap-1.5">
          <button
            :disabled="course.status === 'PUBLISHED'"
            class="inline-flex min-h-[32px] items-center gap-1 rounded-md border border-success/30 px-2.5 py-1 text-xs font-medium text-success transition-colors hover:bg-success-bg disabled:opacity-40"
            @click="review(course, true)"
          >
            <CheckCircle2 class="h-3.5 w-3.5" />通过
          </button>
          <button
            :disabled="course.status === 'OFFLINE'"
            class="inline-flex min-h-[32px] items-center gap-1 rounded-md border border-danger/30 px-2.5 py-1 text-xs font-medium text-danger transition-colors hover:bg-danger-bg disabled:opacity-40"
            @click="review(course, false)"
          >
            <XCircle class="h-3.5 w-3.5" />下架
          </button>
        </div>
      </article>
      <div class="flex items-center justify-between px-4 py-3 text-xs text-neutral-400">
        <span>共 {{ page.total }} 条</span>
        <div class="flex gap-1.5">
          <button :disabled="page.current <= 1"                      class="rounded-md border border-border px-3 py-1.5 text-xs disabled:opacity-40 hover:bg-neutral-100 transition-colors" @click="load(page.current - 1)">上一页</button>
          <button :disabled="page.current * page.size >= page.total" class="rounded-md border border-border px-3 py-1.5 text-xs disabled:opacity-40 hover:bg-neutral-100 transition-colors" @click="load(page.current + 1)">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>
