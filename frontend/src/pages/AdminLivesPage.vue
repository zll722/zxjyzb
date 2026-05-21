<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Power, Search } from 'lucide-vue-next'
import { liveApi, type LiveRoom, type PageResult } from '@/lib/api'

const filters = reactive({ status: '' })
const page    = ref<PageResult<LiveRoom>>({ records: [], total: 0, size: 10, current: 1 })
const loading = ref(false)
const message = ref('')
const error   = ref('')

const statusLabels: Record<string, string> = { SCHEDULED: '待开始', LIVE: '直播中', ENDED: '已结束' }

async function load(current = 1) {
  loading.value = true; error.value = ''
  try { page.value = await liveApi.page({ current, size: page.value.size, status: filters.status }) }
  catch (err) { error.value = err instanceof Error ? err.message : '直播间加载失败' }
  finally { loading.value = false }
}

async function forceClose(room: LiveRoom) {
  if (!window.confirm(`确认强制关闭直播间「${room.title}」？`)) return
  error.value = ''
  try {
    await liveApi.forceClose(room.id); message.value = '直播间已强制关闭'; await load(page.value.current)
  } catch (err) { error.value = err instanceof Error ? err.message : '强制关闭失败' }
}

function formatTime(value?: string) { return value ? new Date(value).toLocaleString() : '-' }

function statusCls(status: string) {
  if (status === 'LIVE')  return 'bg-success-bg text-success'
  if (status === 'ENDED') return 'bg-neutral-100 text-neutral-500'
  return 'bg-warning-bg text-warning'
}

onMounted(() => load())
</script>

<template>
  <div class="mx-auto max-w-7xl">
    <div class="mb-6">
      <p class="text-sm font-medium text-neutral-400">管理员直播管理</p>
      <h1 class="mt-1 text-2xl font-semibold text-neutral-900">全平台直播间与历史状态</h1>
      <p class="mt-1 text-sm text-neutral-500">查看待开始、直播中、已结束直播间，发现异常时可强制关闭。</p>
    </div>

    <!-- Filter bar -->
    <div class="mb-6 rounded-xl border border-border bg-white p-4 shadow-sm">
      <form class="flex flex-wrap gap-3" @submit.prevent="load(1)">
        <select
          v-model="filters.status"
          class="h-10 w-40 rounded-md border border-border bg-white px-3 text-sm outline-none transition-all duration-200 focus:border-primary-600"
        >
          <option value="">全部状态</option>
          <option value="SCHEDULED">待开始</option>
          <option value="LIVE">直播中</option>
          <option value="ENDED">已结束</option>
        </select>
        <button class="inline-flex h-10 items-center gap-1.5 rounded-md bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500">
          <Search class="h-4 w-4" />筛选
        </button>
      </form>
      <div v-if="message" class="mt-3 rounded-md bg-success-bg px-3 py-2 text-sm text-success">{{ message }}</div>
      <div v-if="error"   class="mt-3 rounded-md bg-danger-bg  px-3 py-2 text-sm text-danger">{{ error }}</div>
    </div>

    <!-- Table -->
    <div class="overflow-auto rounded-xl border border-border bg-white shadow-sm">
      <table class="w-full min-w-[900px] text-sm">
        <thead class="border-b border-border bg-neutral-50">
          <tr class="text-left text-xs font-medium text-neutral-500">
            <th class="px-4 py-3">直播间</th>
            <th class="px-4 py-3 w-28">教师</th>
            <th class="px-4 py-3 w-24">状态</th>
            <th class="px-4 py-3 w-40">预定时间</th>
            <th class="px-4 py-3 w-40">开始时间</th>
            <th class="px-4 py-3 w-40">结束时间</th>
            <th class="px-4 py-3 w-32">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7" class="px-4 py-8 text-center text-neutral-400">直播间加载中…</td>
          </tr>
          <tr v-else-if="!page.records.length">
            <td colspan="7" class="px-4 py-8 text-center text-neutral-400">暂无符合条件的直播间</td>
          </tr>
          <tr
            v-for="room in page.records"
            v-else
            :key="room.id"
            class="border-b border-border hover:bg-neutral-50/80"
          >
            <td class="px-4 py-3">
              <p class="font-medium text-neutral-900">{{ room.title }}</p>
              <p class="line-clamp-1 text-xs text-neutral-400">{{ room.intro || '暂无简介' }}</p>
              <p class="text-xs text-neutral-400">在线 {{ room.onlineCount || 0 }} 人</p>
            </td>
            <td class="px-4 py-3 text-neutral-500">{{ room.teacherName || '-' }}</td>
            <td class="px-4 py-3">
              <span class="rounded-full px-2 py-0.5 text-xs font-medium" :class="statusCls(room.status)">
                {{ statusLabels[room.status] || room.status }}
              </span>
            </td>
            <td class="px-4 py-3 text-xs text-neutral-400">{{ formatTime(room.scheduledTime) }}</td>
            <td class="px-4 py-3 text-xs text-neutral-400">{{ formatTime(room.startTime) }}</td>
            <td class="px-4 py-3 text-xs text-neutral-400">{{ formatTime(room.endTime) }}</td>
            <td class="px-4 py-3">
              <div class="flex flex-wrap gap-1.5">
                <RouterLink
                  :to="`/live/${room.id}`"
                  class="rounded-md border border-border px-2 py-1 text-xs transition-colors hover:bg-neutral-100"
                >查看</RouterLink>
                <button
                  :disabled="room.status !== 'LIVE'"
                  class="inline-flex items-center gap-1 rounded-md border border-danger/30 px-2 py-1 text-xs font-medium text-danger transition-colors hover:bg-danger-bg disabled:opacity-40"
                  @click="forceClose(room)"
                >
                  <Power class="h-3 w-3" />关闭
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="flex items-center justify-between px-4 py-3 text-xs text-neutral-400">
        <span>共 {{ page.total }} 条</span>
        <div class="flex gap-1.5">
          <button :disabled="page.current <= 1"                      class="rounded-md border border-border px-3 py-1.5 disabled:opacity-40 hover:bg-neutral-100 transition-colors" @click="load(page.current - 1)">上一页</button>
          <button :disabled="page.current * page.size >= page.total" class="rounded-md border border-border px-3 py-1.5 disabled:opacity-40 hover:bg-neutral-100 transition-colors" @click="load(page.current + 1)">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>
