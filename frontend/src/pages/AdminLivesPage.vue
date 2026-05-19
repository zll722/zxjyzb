<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Power, Radio, Search } from 'lucide-vue-next'
import { liveApi, type LiveRoom, type PageResult } from '@/lib/api'

const filters = reactive({ status: '' })
const page = ref<PageResult<LiveRoom>>({ records: [], total: 0, size: 10, current: 1 })
const loading = ref(false)
const message = ref('')
const error = ref('')

const statusLabels: Record<string, string> = { SCHEDULED: '待开始', LIVE: '直播中', ENDED: '已结束' }

async function load(current = 1) {
  loading.value = true
  error.value = ''
  try {
    page.value = await liveApi.page({ current, size: page.value.size, status: filters.status })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '直播间加载失败'
  } finally {
    loading.value = false
  }
}

async function forceClose(room: LiveRoom) {
  if (!window.confirm(`确认强制关闭直播间「${room.title}」？`)) return
  error.value = ''
  try {
    await liveApi.forceClose(room.id)
    message.value = '直播间已强制关闭'
    await load(page.value.current)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '强制关闭失败'
  }
}

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

function statusClass(status: string) {
  if (status === 'LIVE') return 'bg-emerald-400/15 text-emerald-200'
  if (status === 'ENDED') return 'bg-slate-400/15 text-slate-200'
  return 'bg-cyan-300/15 text-cyan-100'
}

onMounted(() => load())
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/admin" class="text-sm text-cyan-200">返回后台</RouterLink>
        <RouterLink to="/live" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">直播中心</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <div class="flex flex-wrap items-center justify-between gap-6">
          <div>
            <p class="text-sm text-cyan-200">管理员直播管理</p>
            <h1 class="mt-2 text-4xl font-black">全平台直播间与历史状态</h1>
            <p class="mt-3 max-w-3xl text-slate-300">查看待开始、直播中、已结束直播间，发现异常直播时可由管理员强制关闭。</p>
          </div>
          <div class="rounded-[2rem] border border-cyan-300/30 bg-cyan-300/10 p-5 text-cyan-100">
            <Radio class="h-10 w-10" />
          </div>
        </div>
        <form class="mt-8 grid gap-4 sm:grid-cols-[220px_140px]" @submit.prevent="load(1)">
          <select v-model="filters.status" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
            <option value="">全部状态</option>
            <option value="SCHEDULED">待开始</option>
            <option value="LIVE">直播中</option>
            <option value="ENDED">已结束</option>
          </select>
          <button class="inline-flex items-center justify-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">
            <Search class="h-4 w-4" />筛选
          </button>
        </form>
        <p v-if="message" class="mt-4 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <section class="mt-8 overflow-hidden rounded-[2rem] border border-white/10 bg-white/8 backdrop-blur">
        <div class="grid grid-cols-[1.4fr_130px_120px_170px_170px_170px_150px] gap-4 border-b border-white/10 px-6 py-4 text-sm text-slate-400">
          <span>直播间</span><span>教师</span><span>状态</span><span>预定时间</span><span>开始时间</span><span>结束时间</span><span>操作</span>
        </div>
        <p v-if="loading" class="p-6 text-slate-300">直播间加载中...</p>
        <p v-else-if="!page.records.length" class="p-6 text-slate-300">暂无符合条件的直播间</p>
        <article v-for="room in page.records" v-else :key="room.id" class="grid grid-cols-[1.4fr_130px_120px_170px_170px_170px_150px] items-center gap-4 border-b border-white/10 px-6 py-4 last:border-b-0">
          <div>
            <p class="font-bold text-white">{{ room.title }}</p>
            <p class="mt-1 line-clamp-1 text-xs text-slate-400">{{ room.intro || '暂无简介' }}</p>
            <p class="mt-1 text-xs text-cyan-100">在线 {{ room.onlineCount || 0 }} 人</p>
          </div>
          <p class="text-sm text-slate-300">{{ room.teacherName || '-' }}</p>
          <span :class="['w-fit rounded-full px-3 py-1 text-xs font-bold', statusClass(room.status)]">{{ statusLabels[room.status] || room.status }}</span>
          <p class="text-sm text-slate-300">{{ formatTime(room.scheduledTime) }}</p>
          <p class="text-sm text-slate-300">{{ formatTime(room.startTime) }}</p>
          <p class="text-sm text-slate-300">{{ formatTime(room.endTime) }}</p>
          <div class="flex flex-wrap gap-2">
            <RouterLink :to="`/live/${room.id}`" class="rounded-full border border-white/10 px-3 py-1 text-xs hover:bg-white/10">查看</RouterLink>
            <button :disabled="room.status !== 'LIVE'" class="inline-flex items-center gap-1 rounded-full border border-red-300/40 px-3 py-1 text-xs text-red-100 hover:bg-red-400/10 disabled:opacity-40" @click="forceClose(room)">
              <Power class="h-3 w-3" />关闭
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
