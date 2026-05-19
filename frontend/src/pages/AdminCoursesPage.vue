<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { CheckCircle2, Search, XCircle } from 'lucide-vue-next'
import { adminApi, type Course, type PageResult } from '@/lib/api'

const filters = reactive({ status: '' })
const page = ref<PageResult<Course>>({ records: [], total: 0, size: 10, current: 1 })
const loading = ref(false)
const message = ref('')
const error = ref('')

const statusLabels: Record<string, string> = { DRAFT: '草稿', REVIEWING: '待审核', PUBLISHED: '已发布', OFFLINE: '已下架' }

async function load(current = 1) {
  loading.value = true
  error.value = ''
  try {
    page.value = await adminApi.courses({ current, size: page.value.size, status: filters.status })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '课程列表加载失败'
  } finally {
    loading.value = false
  }
}

async function review(course: Course, approved: boolean) {
  await adminApi.reviewCourse(course.id, approved)
  message.value = approved ? '课程已审核通过' : '课程已下架'
  await load(page.value.current)
}

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

onMounted(() => load())
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/admin" class="text-sm text-cyan-200">返回后台</RouterLink>
        <RouterLink to="/courses" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">课程中心</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">管理员课程审核</p>
        <h1 class="mt-2 text-4xl font-black">课程发布审核与下架</h1>
        <form class="mt-8 grid gap-4 sm:grid-cols-[220px_140px]" @submit.prevent="load(1)">
          <select v-model="filters.status" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
            <option value="">全部状态</option>
            <option value="DRAFT">草稿</option>
            <option value="REVIEWING">待审核</option>
            <option value="PUBLISHED">已发布</option>
            <option value="OFFLINE">已下架</option>
          </select>
          <button class="inline-flex items-center justify-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">
            <Search class="h-4 w-4" />筛选
          </button>
        </form>
        <p v-if="message" class="mt-4 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <section class="mt-8 overflow-hidden rounded-[2rem] border border-white/10 bg-white/8 backdrop-blur">
        <div class="grid grid-cols-[1.5fr_160px_150px_190px_190px] gap-4 border-b border-white/10 px-6 py-4 text-sm text-slate-400">
          <span>课程</span><span>教师</span><span>分类</span><span>创建时间</span><span>操作</span>
        </div>
        <p v-if="loading" class="p-6 text-slate-300">课程加载中...</p>
        <p v-else-if="!page.records.length" class="p-6 text-slate-300">暂无符合条件的课程</p>
        <article v-for="course in page.records" v-else :key="course.id" class="grid grid-cols-[1.5fr_160px_150px_190px_190px] items-center gap-4 border-b border-white/10 px-6 py-4 last:border-b-0">
          <div>
            <p class="font-bold text-white">{{ course.title }}</p>
            <p class="mt-1 line-clamp-1 text-xs text-slate-400">{{ course.intro || '暂无简介' }}</p>
            <span :class="['mt-2 inline-flex rounded-full px-3 py-1 text-xs font-bold', course.status === 'PUBLISHED' ? 'bg-emerald-400/15 text-emerald-200' : course.status === 'OFFLINE' ? 'bg-red-400/15 text-red-200' : 'bg-cyan-300/15 text-cyan-100']">{{ statusLabels[course.status] || course.status }}</span>
          </div>
          <p class="text-sm text-slate-300">{{ course.teacherName || '-' }}</p>
          <p class="text-sm text-slate-300">{{ course.categoryName || '-' }}</p>
          <p class="text-sm text-slate-300">{{ formatTime(course.createdAt) }}</p>
          <div class="flex flex-wrap gap-2">
            <button :disabled="course.status === 'PUBLISHED'" class="inline-flex items-center gap-1 rounded-full border border-emerald-300/40 px-3 py-1 text-xs text-emerald-100 disabled:opacity-40" @click="review(course, true)">
              <CheckCircle2 class="h-3 w-3" />通过
            </button>
            <button :disabled="course.status === 'OFFLINE'" class="inline-flex items-center gap-1 rounded-full border border-red-300/40 px-3 py-1 text-xs text-red-100 disabled:opacity-40" @click="review(course, false)">
              <XCircle class="h-3 w-3" />下架
            </button>
          </div>
        </article>
        <div class="flex items-center justify-between px-6 py-4 text-sm text-slate-300">
          <span>共 {{ page.total }} 条</span>
          <div class="flex gap-2">
            <button :disabled="page.current <= 1" class="rounded-full border border-white/10 px-4 py-2 disabled:opacity-40" @click="load(page.current - 1)">上一页</button>
            <button :disabled="page.current * page.size >= page.total" class="rounded-full border border-white/10 px-4 py-2 disabled:opacity-40" @click="load(page.current + 1)">下一页</button>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>
