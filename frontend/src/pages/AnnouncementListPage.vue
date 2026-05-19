<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Megaphone, Search } from 'lucide-vue-next'
import { announcementApi, type Announcement, type PageResult } from '@/lib/api'

const filters = reactive({ keyword: '', type: '' })
const page = ref<PageResult<Announcement>>({ records: [], total: 0, size: 10, current: 1 })
const loading = ref(false)
const error = ref('')

const typeLabels: Record<string, string> = { SYSTEM: '系统公告', COURSE: '课程通知', LIVE: '直播通知', EXAM: '考试提醒' }

async function load(current = 1) {
  loading.value = true
  error.value = ''
  try {
    page.value = await announcementApi.page({ current, size: page.value.size, keyword: filters.keyword, type: filters.type })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '公告加载失败'
  } finally {
    loading.value = false
  }
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
        <RouterLink to="/dashboard" class="text-sm text-cyan-200">返回工作台</RouterLink>
        <RouterLink to="/messages" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">我的消息</RouterLink>
      </nav>

      <section class="relative overflow-hidden rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <div class="absolute inset-0 -z-0 bg-[radial-gradient(circle_at_18%_20%,rgba(34,211,238,0.18),transparent_30%),radial-gradient(circle_at_85%_20%,rgba(20,184,166,0.12),transparent_28%)]" />
        <div class="relative flex flex-wrap items-center justify-between gap-6">
          <div>
            <p class="text-sm text-cyan-200">公告中心</p>
            <h1 class="mt-2 text-4xl font-black">平台公告与教学通知</h1>
            <p class="mt-3 max-w-3xl text-slate-300">集中查看系统公告、课程安排、直播与考试提醒，按发布时间同步展示。</p>
          </div>
          <div class="rounded-[2rem] border border-cyan-300/30 bg-cyan-300/10 p-5 text-cyan-100">
            <Megaphone class="h-10 w-10" />
          </div>
        </div>

        <form class="relative mt-8 grid gap-4 md:grid-cols-[1fr_220px_140px]" @submit.prevent="load(1)">
          <input v-model="filters.keyword" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="搜索公告标题" />
          <select v-model="filters.type" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
            <option value="">全部类型</option>
            <option value="SYSTEM">系统公告</option>
            <option value="COURSE">课程通知</option>
            <option value="LIVE">直播通知</option>
            <option value="EXAM">考试提醒</option>
          </select>
          <button class="inline-flex items-center justify-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">
            <Search class="h-4 w-4" />筛选
          </button>
        </form>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <p v-if="loading" class="mt-8 text-slate-300">公告加载中...</p>
      <section v-else class="mt-8 grid gap-5">
        <p v-if="!page.records.length" class="rounded-[2rem] border border-white/10 bg-white/8 p-8 text-slate-300">暂无公告</p>
        <RouterLink v-for="item in page.records" v-else :key="item.id" :to="`/announcements/${item.id}`" class="group rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur transition hover:-translate-y-1 hover:border-cyan-300/40">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <span class="inline-flex rounded-full bg-cyan-300/15 px-3 py-1 text-xs font-bold text-cyan-100">{{ typeLabels[item.type || ''] || item.type || '公告' }}</span>
              <h2 class="mt-4 text-2xl font-black group-hover:text-cyan-100">{{ item.title }}</h2>
              <p class="mt-2 line-clamp-2 text-sm leading-6 text-slate-300">{{ item.content }}</p>
            </div>
            <p class="text-sm text-slate-400">{{ formatTime(item.publishTime || item.createdAt) }}</p>
          </div>
        </RouterLink>
      </section>

      <div class="mt-8 flex items-center justify-between text-sm text-slate-300">
        <span>共 {{ page.total }} 条</span>
        <div class="flex gap-2">
          <button :disabled="page.current <= 1" class="rounded-full border border-white/10 px-4 py-2 disabled:opacity-40" @click="load(page.current - 1)">上一页</button>
          <button :disabled="page.current * page.size >= page.total" class="rounded-full border border-white/10 px-4 py-2 disabled:opacity-40" @click="load(page.current + 1)">下一页</button>
        </div>
      </div>
    </div>
  </main>
</template>
