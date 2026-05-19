<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { CheckCheck, Mail, Search } from 'lucide-vue-next'
import { messageApi, type PageResult, type UserMessage } from '@/lib/api'

const filters = reactive<{ readStatus: number | '' }>({ readStatus: '' })
const page = ref<PageResult<UserMessage>>({ records: [], total: 0, size: 10, current: 1 })
const unreadCount = ref(0)
const loading = ref(false)
const message = ref('')
const error = ref('')

async function load(current = 1) {
  loading.value = true
  error.value = ''
  try {
    const [list, count] = await Promise.all([
      messageApi.page({ current, size: page.value.size, readStatus: filters.readStatus }),
      messageApi.unreadCount(),
    ])
    page.value = list
    unreadCount.value = count
  } catch (err) {
    error.value = err instanceof Error ? err.message : '消息加载失败'
  } finally {
    loading.value = false
  }
}

async function markRead(item: UserMessage) {
  if (item.readStatus === 1) return
  await messageApi.markRead(item.id)
  message.value = '消息已标记为已读'
  await load(page.value.current)
}

async function markAllRead() {
  await messageApi.markAllRead()
  message.value = '全部消息已标记为已读'
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
        <RouterLink to="/dashboard" class="text-sm text-cyan-200">返回工作台</RouterLink>
        <RouterLink to="/announcements" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">公告中心</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <div class="flex flex-wrap items-center justify-between gap-6">
          <div>
            <p class="text-sm text-cyan-200">我的消息</p>
            <h1 class="mt-2 text-4xl font-black">站内提醒与未读消息</h1>
            <p class="mt-3 max-w-3xl text-slate-300">未读 {{ unreadCount }} 条，支持按状态筛选并快速全部已读。</p>
          </div>
          <div class="rounded-[2rem] border border-cyan-300/30 bg-cyan-300/10 p-5 text-cyan-100">
            <Mail class="h-10 w-10" />
          </div>
        </div>

        <form class="mt-8 grid gap-4 sm:grid-cols-[220px_140px_160px]" @submit.prevent="load(1)">
          <select v-model="filters.readStatus" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
            <option value="">全部消息</option>
            <option :value="0">未读</option>
            <option :value="1">已读</option>
          </select>
          <button class="inline-flex items-center justify-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">
            <Search class="h-4 w-4" />筛选
          </button>
          <button type="button" class="inline-flex items-center justify-center gap-2 rounded-2xl border border-white/10 px-5 py-3 font-bold text-white hover:bg-white/10" @click="markAllRead">
            <CheckCheck class="h-4 w-4" />全部已读
          </button>
        </form>
        <p v-if="message" class="mt-4 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <section class="mt-8 overflow-hidden rounded-[2rem] border border-white/10 bg-white/8 backdrop-blur">
        <p v-if="loading" class="p-6 text-slate-300">消息加载中...</p>
        <p v-else-if="!page.records.length" class="p-6 text-slate-300">暂无消息</p>
        <article v-for="item in page.records" v-else :key="item.id" :class="['border-b border-white/10 p-6 last:border-b-0', item.readStatus === 1 ? 'bg-transparent' : 'bg-cyan-300/5']">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <div class="flex flex-wrap items-center gap-3">
                <span class="inline-flex rounded-full bg-cyan-300/15 px-3 py-1 text-xs font-bold text-cyan-100">{{ item.senderName || '系统消息' }}</span>
                <span v-if="item.readStatus === 0" class="inline-flex rounded-full bg-amber-300/15 px-3 py-1 text-xs font-bold text-amber-100">未读</span>
              </div>
              <h2 class="mt-4 text-2xl font-black">{{ item.title }}</h2>
              <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-300">{{ item.content }}</p>
              <p class="mt-3 text-xs text-slate-500">{{ formatTime(item.createdAt) }}</p>
            </div>
            <button :disabled="item.readStatus === 1" class="rounded-full border border-white/10 px-4 py-2 text-sm text-white disabled:opacity-40" @click="markRead(item)">标记已读</button>
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
