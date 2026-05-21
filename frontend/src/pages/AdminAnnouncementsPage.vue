<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus, Save, Search } from 'lucide-vue-next'
import { announcementApi, type Announcement, type PageResult } from '@/lib/api'

const filters = reactive({ keyword: '', type: '', status: '' })
const form = reactive({ title: '', content: '', type: 'SYSTEM', status: 'DRAFT', pinned: 0, syncMessage: false })
const editForms = reactive<Record<number, { title: string; content: string; type: string; status: string; pinned: number; syncMessage: boolean }>>({})
const page = ref<PageResult<Announcement>>({ records: [], total: 0, size: 10, current: 1 })
const loading = ref(false)
const message = ref('')
const error = ref('')

const typeLabels: Record<string, string> = { SYSTEM: '系统公告', COURSE: '课程通知', LIVE: '直播通知', EXAM: '考试提醒' }
const statusLabels: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下线' }
const inputCls = 'h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100'

async function load(current = 1) {
  loading.value = true; error.value = ''
  try {
    page.value = await announcementApi.adminPage({ current, size: page.value.size, ...filters })
    page.value.records.forEach((item) => {
      editForms[item.id] = { title: item.title, content: item.content, type: item.type || 'SYSTEM', status: item.status, pinned: item.pinned, syncMessage: false }
    })
  } catch (err) { error.value = err instanceof Error ? err.message : '公告列表加载失败' }
  finally { loading.value = false }
}

async function createAnnouncement() {
  message.value = ''; error.value = ''
  try {
    await announcementApi.create(form)
    Object.assign(form, { title: '', content: '', type: 'SYSTEM', status: 'DRAFT', pinned: 0, syncMessage: false })
    message.value = '公告已创建'; await load(1)
  } catch (err) { error.value = err instanceof Error ? err.message : '创建失败' }
}

async function save(item: Announcement) { await announcementApi.update(item.id, editForms[item.id]); message.value = '公告已保存'; await load(page.value.current) }
async function publish(id: number) { await announcementApi.publish(id, editForms[id]?.syncMessage || false); message.value = '公告已发布'; await load(page.value.current) }
async function offline(id: number) { await announcementApi.offline(id); message.value = '公告已下线'; await load(page.value.current) }
async function remove(id: number) { await announcementApi.remove(id); message.value = '公告已删除'; await load(page.value.current) }

function formatTime(value?: string) { return value ? new Date(value).toLocaleString() : '-' }
function statusCls(status: string) {
  if (status === 'PUBLISHED') return 'bg-success-bg text-success'
  if (status === 'OFFLINE') return 'bg-neutral-100 text-neutral-500'
  return 'bg-warning-bg text-warning'
}

onMounted(() => load())
</script>

<template>
  <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">管理员公告管理</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">公告发布、编辑与下线</h1>
        <p class="mt-1 text-sm text-neutral-500">公告面向前台公告中心展示，管理操作由后端管理员权限校验。</p>
      </div>

      <!-- Filters -->
      <div class="mb-6 rounded-xl border border-border bg-white p-4 shadow-sm">
        <form class="flex flex-wrap gap-3" @submit.prevent="load(1)">
          <input v-model="filters.keyword" :class="inputCls + ' flex-1 min-w-40'" placeholder="搜索公告标题" />
          <select v-model="filters.type" :class="inputCls + ' w-36'">
            <option value="">全部类型</option>
            <option value="SYSTEM">系统公告</option>
            <option value="COURSE">课程通知</option>
            <option value="LIVE">直播通知</option>
            <option value="EXAM">考试提醒</option>
          </select>
          <select v-model="filters.status" :class="inputCls + ' w-36'">
            <option value="">全部状态</option>
            <option value="DRAFT">草稿</option>
            <option value="PUBLISHED">已发布</option>
            <option value="OFFLINE">已下线</option>
          </select>
          <button class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-primary-600 px-4 text-sm font-semibold text-white hover:bg-primary-700">
            <Search class="h-4 w-4" />筛选
          </button>
        </form>
        <div v-if="message" class="mt-3 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{{ message }}</div>
        <div v-if="error" class="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{{ error }}</div>
      </div>

      <!-- Create form -->
      <div class="mb-6 rounded-xl border border-border bg-white p-5 shadow-sm">
        <h2 class="mb-4 font-semibold text-neutral-900">新建公告</h2>
        <form class="grid gap-3" @submit.prevent="createAnnouncement">
          <div class="grid gap-3 md:grid-cols-[1fr_150px_150px_100px]">
            <input v-model="form.title" required :class="inputCls" placeholder="公告标题" />
            <select v-model="form.type" :class="inputCls">
              <option value="SYSTEM">系统公告</option>
              <option value="COURSE">课程通知</option>
              <option value="LIVE">直播通知</option>
              <option value="EXAM">考试提醒</option>
            </select>
            <select v-model="form.status" :class="inputCls">
              <option value="DRAFT">草稿</option>
              <option value="PUBLISHED">直接发布</option>
            </select>
            <input v-model.number="form.pinned" min="0" type="number" :class="inputCls" placeholder="置顶" />
          </div>
          <label class="inline-flex w-fit items-center gap-2 text-sm text-neutral-600">
            <input v-model="form.syncMessage" type="checkbox" class="h-4 w-4 accent-indigo-600" />
            发布时同步站内消息
          </label>
          <textarea v-model="form.content" required rows="4" class="rounded-lg border border-border bg-white px-3 py-2 text-sm outline-none transition placeholder:text-neutral-400 focus:border-primary-400 focus:ring-2 focus:ring-primary-100" placeholder="公告内容" />
          <button class="inline-flex h-9 w-fit items-center gap-1.5 rounded-lg bg-primary-600 px-4 text-sm font-semibold text-white hover:bg-primary-700">
            <Plus class="h-4 w-4" />创建公告
          </button>
        </form>
      </div>

      <!-- Announcement list -->
      <div v-if="loading" class="py-6 text-center text-sm text-neutral-400">公告加载中...</div>
      <div v-else-if="!page.records.length" class="rounded-xl border border-border bg-white p-8 text-center text-sm text-neutral-400">暂无公告</div>
      <div v-else class="space-y-4">
        <article v-for="item in page.records" :key="item.id" class="rounded-xl border border-border bg-white p-5 shadow-sm">
          <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
            <div class="flex flex-wrap items-center gap-2">
              <span class="rounded-full bg-primary-50 px-2.5 py-0.5 text-xs font-medium text-primary-700">{{ typeLabels[item.type || ''] || '公告' }}</span>
              <span class="rounded-full px-2.5 py-0.5 text-xs font-medium" :class="statusCls(item.status)">{{ statusLabels[item.status] || item.status }}</span>
              <span class="text-xs text-neutral-400">{{ formatTime(item.publishTime || item.createdAt) }}</span>
            </div>
            <div class="flex flex-wrap gap-1.5">
              <button :disabled="item.status === 'PUBLISHED'" class="rounded border border-emerald-200 px-2.5 py-1 text-xs font-medium text-emerald-700 hover:bg-emerald-50 disabled:opacity-40" @click="publish(item.id)">发布</button>
              <button :disabled="item.status === 'OFFLINE'" class="rounded border border-amber-200 px-2.5 py-1 text-xs font-medium text-amber-700 hover:bg-amber-50 disabled:opacity-40" @click="offline(item.id)">下线</button>
              <button class="rounded border border-red-200 px-2.5 py-1 text-xs font-medium text-red-600 hover:bg-red-50" @click="remove(item.id)">删除</button>
            </div>
          </div>
          <div class="grid gap-3">
            <input v-model="editForms[item.id].title" :class="inputCls + ' w-full font-semibold'" />
            <div class="grid gap-3 md:grid-cols-3">
              <select v-model="editForms[item.id].type" :class="inputCls">
                <option value="SYSTEM">系统公告</option>
                <option value="COURSE">课程通知</option>
                <option value="LIVE">直播通知</option>
                <option value="EXAM">考试提醒</option>
              </select>
              <select v-model="editForms[item.id].status" :class="inputCls">
                <option value="DRAFT">草稿</option>
                <option value="PUBLISHED">已发布</option>
                <option value="OFFLINE">已下线</option>
              </select>
              <input v-model.number="editForms[item.id].pinned" min="0" type="number" :class="inputCls" placeholder="置顶排序" />
            </div>
            <label class="inline-flex w-fit items-center gap-2 text-sm text-neutral-600">
              <input v-model="editForms[item.id].syncMessage" type="checkbox" class="h-4 w-4 accent-indigo-600" />
              发布时同步站内消息
            </label>
            <textarea v-model="editForms[item.id].content" rows="3" class="rounded-lg border border-border bg-white px-3 py-2 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
            <button class="inline-flex h-9 w-fit items-center gap-1.5 rounded-lg bg-primary-600 px-4 text-sm font-semibold text-white hover:bg-primary-700" @click="save(item)">
              <Save class="h-4 w-4" />保存修改
            </button>
          </div>
        </article>
      </div>

      <div class="mt-5 flex items-center justify-between text-xs text-neutral-400">
        <span>共 {{ page.total }} 条</span>
        <div class="flex gap-1.5">
          <button :disabled="page.current <= 1" class="rounded border border-border px-3 py-1.5 disabled:opacity-40 hover:bg-neutral-100" @click="load(page.current - 1)">上一页</button>
          <button :disabled="page.current * page.size >= page.total" class="rounded border border-border px-3 py-1.5 disabled:opacity-40 hover:bg-neutral-100" @click="load(page.current + 1)">下一页</button>
        </div>
      </div>
  </div>
</template>
