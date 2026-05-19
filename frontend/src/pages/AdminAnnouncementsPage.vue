<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Megaphone, Plus, Search } from 'lucide-vue-next'
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

async function load(current = 1) {
  loading.value = true
  error.value = ''
  try {
    page.value = await announcementApi.adminPage({ current, size: page.value.size, ...filters })
    page.value.records.forEach((item) => {
      editForms[item.id] = { title: item.title, content: item.content, type: item.type || 'SYSTEM', status: item.status, pinned: item.pinned, syncMessage: false }
    })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '公告列表加载失败'
  } finally {
    loading.value = false
  }
}

async function createAnnouncement() {
  message.value = ''
  error.value = ''
  try {
    await announcementApi.create(form)
    form.title = ''
    form.content = ''
    form.type = 'SYSTEM'
    form.status = 'DRAFT'
    form.pinned = 0
    form.syncMessage = false
    message.value = '公告已创建'
    await load(1)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '创建失败'
  }
}

async function save(item: Announcement) {
  await announcementApi.update(item.id, editForms[item.id])
  message.value = '公告已保存'
  await load(page.value.current)
}

async function publish(id: number) {
  await announcementApi.publish(id, editForms[id]?.syncMessage || false)
  message.value = '公告已发布'
  await load(page.value.current)
}

async function offline(id: number) {
  await announcementApi.offline(id)
  message.value = '公告已下线'
  await load(page.value.current)
}

async function remove(id: number) {
  await announcementApi.remove(id)
  message.value = '公告已删除'
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
        <RouterLink to="/announcements" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">公告中心</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <div class="flex flex-wrap items-center justify-between gap-6">
          <div>
            <p class="text-sm text-cyan-200">管理员公告管理</p>
            <h1 class="mt-2 text-4xl font-black">公告发布、编辑与下线</h1>
            <p class="mt-3 max-w-3xl text-slate-300">公告面向前台公告中心展示，管理操作由后端管理员权限校验。</p>
          </div>
          <div class="rounded-[2rem] border border-cyan-300/30 bg-cyan-300/10 p-5 text-cyan-100">
            <Megaphone class="h-10 w-10" />
          </div>
        </div>

        <form class="mt-8 grid gap-4 lg:grid-cols-[1fr_180px_180px_140px]" @submit.prevent="load(1)">
          <input v-model="filters.keyword" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="搜索公告标题" />
          <select v-model="filters.type" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
            <option value="">全部类型</option>
            <option value="SYSTEM">系统公告</option>
            <option value="COURSE">课程通知</option>
            <option value="LIVE">直播通知</option>
            <option value="EXAM">考试提醒</option>
          </select>
          <select v-model="filters.status" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
            <option value="">全部状态</option>
            <option value="DRAFT">草稿</option>
            <option value="PUBLISHED">已发布</option>
            <option value="OFFLINE">已下线</option>
          </select>
          <button class="inline-flex items-center justify-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">
            <Search class="h-4 w-4" />筛选
          </button>
        </form>
        <p v-if="message" class="mt-4 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <section class="mt-8 rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
        <h2 class="text-2xl font-black">新建公告</h2>
        <form class="mt-5 grid gap-4" @submit.prevent="createAnnouncement">
          <div class="grid gap-4 md:grid-cols-[1fr_180px_180px_140px]">
            <input v-model="form.title" required class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="公告标题" />
            <select v-model="form.type" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
              <option value="SYSTEM">系统公告</option>
              <option value="COURSE">课程通知</option>
              <option value="LIVE">直播通知</option>
              <option value="EXAM">考试提醒</option>
            </select>
            <select v-model="form.status" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
              <option value="DRAFT">草稿</option>
              <option value="PUBLISHED">直接发布</option>
            </select>
            <input v-model.number="form.pinned" min="0" type="number" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="置顶排序" />
          </div>
          <label class="inline-flex w-fit items-center gap-2 rounded-2xl border border-white/10 px-4 py-3 text-sm text-slate-200">
            <input v-model="form.syncMessage" type="checkbox" class="h-4 w-4 accent-cyan-300" />
            发布时同步站内消息
          </label>
          <textarea v-model="form.content" required rows="5" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="公告内容" />
          <button class="inline-flex w-fit items-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">
            <Plus class="h-4 w-4" />创建公告
          </button>
        </form>
      </section>

      <section class="mt-8 grid gap-5">
        <p v-if="loading" class="text-slate-300">公告加载中...</p>
        <p v-else-if="!page.records.length" class="rounded-[2rem] border border-white/10 bg-white/8 p-8 text-slate-300">暂无公告</p>
        <article v-for="item in page.records" v-else :key="item.id" class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="flex flex-wrap items-center justify-between gap-4">
            <div class="flex flex-wrap gap-3">
              <span class="inline-flex rounded-full bg-cyan-300/15 px-3 py-1 text-xs font-bold text-cyan-100">{{ typeLabels[item.type || ''] || item.type || '公告' }}</span>
              <span class="inline-flex rounded-full bg-white/10 px-3 py-1 text-xs font-bold text-slate-200">{{ statusLabels[item.status] || item.status }}</span>
              <span class="text-xs text-slate-500">{{ formatTime(item.publishTime || item.createdAt) }}</span>
            </div>
            <div class="flex flex-wrap gap-2">
              <button :disabled="item.status === 'PUBLISHED'" class="rounded-full border border-emerald-300/40 px-3 py-1 text-xs text-emerald-100 disabled:opacity-40" @click="publish(item.id)">发布</button>
              <button :disabled="item.status === 'OFFLINE'" class="rounded-full border border-amber-300/40 px-3 py-1 text-xs text-amber-100 disabled:opacity-40" @click="offline(item.id)">下线</button>
              <button class="rounded-full border border-red-300/40 px-3 py-1 text-xs text-red-100" @click="remove(item.id)">删除</button>
            </div>
          </div>
          <div class="mt-5 grid gap-4">
            <input v-model="editForms[item.id].title" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 font-bold outline-none focus:border-cyan-300" />
            <div class="grid gap-4 md:grid-cols-3">
              <select v-model="editForms[item.id].type" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
                <option value="SYSTEM">系统公告</option>
                <option value="COURSE">课程通知</option>
                <option value="LIVE">直播通知</option>
                <option value="EXAM">考试提醒</option>
              </select>
              <select v-model="editForms[item.id].status" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
                <option value="DRAFT">草稿</option>
                <option value="PUBLISHED">已发布</option>
                <option value="OFFLINE">已下线</option>
              </select>
              <input v-model.number="editForms[item.id].pinned" min="0" type="number" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="置顶排序" />
            </div>
            <label class="inline-flex w-fit items-center gap-2 rounded-2xl border border-white/10 px-4 py-3 text-sm text-slate-200">
              <input v-model="editForms[item.id].syncMessage" type="checkbox" class="h-4 w-4 accent-cyan-300" />
              发布时同步站内消息
            </label>
            <textarea v-model="editForms[item.id].content" rows="4" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
            <button class="w-fit rounded-full bg-cyan-300 px-5 py-2 text-sm font-bold text-slate-950 hover:bg-cyan-200" @click="save(item)">保存修改</button>
          </div>
        </article>
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
