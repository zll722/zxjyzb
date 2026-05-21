<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { CheckCheck, Mail } from 'lucide-vue-next'
import { messageApi, type PageResult, type UserMessage } from '@/lib/api'
import AppLayout from '@/components/layout/AppLayout.vue'
import Empty from '@/components/Empty.vue'

const filters = reactive<{ readStatus: number | '' }>({ readStatus: '' })
const page = ref<PageResult<UserMessage>>({ records: [], total: 0, size: 10, current: 1 })
const unreadCount = ref(0)
const loading = ref(false)
const message = ref('')
const error = ref('')

async function load(current = 1) {
  loading.value = true; error.value = ''
  try {
    const [list, count] = await Promise.all([
      messageApi.page({ current, size: page.value.size, readStatus: filters.readStatus }),
      messageApi.unreadCount(),
    ])
    page.value = list; unreadCount.value = count
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

function formatTime(value?: string) { return value ? new Date(value).toLocaleString('zh-CN') : '-' }

onMounted(() => load())
</script>

<template>
  <AppLayout>
    <div class="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-6 flex items-start justify-between">
        <div>
          <p class="text-sm font-medium text-primary-600">我的消息</p>
          <h1 class="mt-1 text-2xl font-bold text-neutral-900">站内提醒</h1>
          <p class="mt-1 text-sm text-neutral-500">未读 {{ unreadCount }} 条</p>
        </div>
        <button
          class="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-2 text-sm font-medium text-neutral-700 transition hover:bg-neutral-100"
          @click="markAllRead"
        >
          <CheckCheck class="h-4 w-4" />全部已读
        </button>
      </div>

      <!-- Filter -->
      <div class="mb-4 flex gap-2">
        <select
          v-model="filters.readStatus"
          class="h-9 rounded-lg border border-border bg-white px-3 text-sm text-neutral-700 outline-none transition focus:border-primary-400"
          @change="load(1)"
        >
          <option value="">全部消息</option>
          <option :value="0">未读</option>
          <option :value="1">已读</option>
        </select>
      </div>

      <div v-if="message" class="mb-3 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</div>
      <div v-if="error" class="mb-3 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

      <!-- Message list -->
      <div v-if="loading" class="space-y-2">
        <div v-for="i in 4" :key="i" class="h-20 animate-pulse rounded-xl border border-border bg-white" />
      </div>
      <div v-else-if="page.records.length" class="divide-y divide-border overflow-hidden rounded-xl border border-border bg-white shadow-sm">
        <article
          v-for="item in page.records"
          :key="item.id"
          :class="['p-4', item.readStatus === 0 ? 'bg-primary-50' : '']"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <span class="text-xs font-medium text-neutral-500">{{ item.senderName || '系统消息' }}</span>
                <span v-if="item.readStatus === 0" class="rounded-full bg-primary-100 px-2 py-0.5 text-[10px] font-semibold text-primary-700">未读</span>
              </div>
              <h2 class="mt-1 font-semibold text-neutral-900">{{ item.title }}</h2>
              <p class="mt-1 whitespace-pre-wrap text-sm text-neutral-600">{{ item.content }}</p>
              <p class="mt-2 text-xs text-neutral-400">{{ formatTime(item.createdAt) }}</p>
            </div>
            <button
              :disabled="item.readStatus === 1"
              class="flex-shrink-0 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-neutral-600 transition hover:bg-neutral-100 disabled:opacity-40"
              @click="markRead(item)"
            >
              已读
            </button>
          </div>
        </article>
      </div>
      <Empty v-else title="暂无消息" :icon="Mail" />

      <!-- Pagination -->
      <div v-if="page.total > page.size" class="mt-4 flex items-center justify-between text-sm text-neutral-500">
        <span>共 {{ page.total }} 条</span>
        <div class="flex gap-1.5">
          <button :disabled="page.current <= 1" class="rounded-lg border border-border px-3 py-1.5 transition hover:bg-neutral-100 disabled:opacity-40" @click="load(page.current - 1)">上一页</button>
          <button :disabled="page.current * page.size >= page.total" class="rounded-lg border border-border px-3 py-1.5 transition hover:bg-neutral-100 disabled:opacity-40" @click="load(page.current + 1)">下一页</button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
