<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Pencil, Plus, Search, X } from 'lucide-vue-next'
import { announcementApi, type Announcement, type PageResult } from '@/lib/api'

const filters = reactive({ keyword: '', type: '', status: '' })
const page = ref<PageResult<Announcement>>({ records: [], total: 0, size: 10, current: 1 })
const loading = ref(false)
const message = ref('')
const error = ref('')

const modalOpen = ref(false)
const editingAnnouncement = ref<Announcement | null>(null)
const modalForm = reactive({ title: '', content: '', type: 'SYSTEM', status: 'DRAFT', pinned: 0, syncMessage: false })

const typeLabels: Record<string, string> = { SYSTEM: '系统公告', COURSE: '课程通知', LIVE: '直播通知', EXAM: '考试提醒' }
const statusLabels: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下线' }
const inputCls = 'h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100'

async function load(current = 1) {
  loading.value = true; error.value = ''
  try {
    page.value = await announcementApi.adminPage({ current, size: page.value.size, ...filters })
  } catch (err) { error.value = err instanceof Error ? err.message : '公告列表加载失败' }
  finally { loading.value = false }
}

function openCreateModal() {
  editingAnnouncement.value = null
  Object.assign(modalForm, { title: '', content: '', type: 'SYSTEM', status: 'DRAFT', pinned: 0, syncMessage: false })
  modalOpen.value = true
}

function openEditModal(item: Announcement) {
  editingAnnouncement.value = item
  Object.assign(modalForm, { title: item.title, content: item.content, type: item.type || 'SYSTEM', status: item.status, pinned: item.pinned, syncMessage: false })
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
  editingAnnouncement.value = null
}

async function submitModal() {
  message.value = ''; error.value = ''
  try {
    if (editingAnnouncement.value) {
      await announcementApi.update(editingAnnouncement.value.id, modalForm)
      message.value = '公告已保存'
    } else {
      await announcementApi.create(modalForm)
      message.value = '公告已创建'
    }
    closeModal()
    await load(page.value.current)
  } catch (err) { error.value = err instanceof Error ? err.message : '操作失败' }
}

async function publish(id: number, syncMessage = false) { await announcementApi.publish(id, syncMessage); message.value = '公告已发布'; await load(page.value.current) }
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
      <div class="flex flex-wrap items-center justify-between gap-3">
        <form class="flex flex-1 flex-wrap gap-3" @submit.prevent="load(1)">
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
          <button class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-neutral-100 px-4 text-sm font-medium text-neutral-700 hover:bg-neutral-200">
            <Search class="h-4 w-4" />筛选
          </button>
        </form>
        <button
          class="inline-flex h-9 items-center gap-2 rounded-lg bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700"
          @click="openCreateModal"
        >
          <Plus class="h-4 w-4" />新建公告
        </button>
      </div>
      <div v-if="message" class="mt-3 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{{ message }}</div>
      <div v-if="error" class="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{{ error }}</div>
    </div>

    <!-- Announcement list -->
    <div v-if="loading" class="py-6 text-center text-sm text-neutral-400">公告加载中...</div>
    <div v-else-if="!page.records.length" class="rounded-xl border border-border bg-white p-8 text-center text-sm text-neutral-400">暂无公告</div>
    <div v-else class="space-y-4">
      <article v-for="item in page.records" :key="item.id" class="rounded-xl border border-border bg-white p-5 shadow-sm">
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div class="flex-1">
            <div class="flex flex-wrap items-center gap-2">
              <span class="rounded-full bg-primary-50 px-2.5 py-0.5 text-xs font-medium text-primary-700">{{ typeLabels[item.type || ''] || '公告' }}</span>
              <span class="rounded-full px-2.5 py-0.5 text-xs font-medium" :class="statusCls(item.status)">{{ statusLabels[item.status] || item.status }}</span>
              <span class="text-xs text-neutral-400">{{ formatTime(item.publishTime || item.createdAt) }}</span>
            </div>
            <h2 class="mt-2 font-semibold text-neutral-900">{{ item.title }}</h2>
            <p class="mt-1 line-clamp-2 text-sm text-neutral-500">{{ item.content }}</p>
          </div>
          <div class="flex flex-wrap gap-1.5">
            <button class="inline-flex items-center gap-1 rounded border border-border px-2.5 py-1 text-xs font-medium text-neutral-700 hover:bg-neutral-100" @click="openEditModal(item)">
              <Pencil class="h-3 w-3" />编辑
            </button>
            <button :disabled="item.status === 'PUBLISHED'" class="rounded border border-emerald-200 px-2.5 py-1 text-xs font-medium text-emerald-700 hover:bg-emerald-50 disabled:opacity-40" @click="publish(item.id)">发布</button>
            <button :disabled="item.status === 'OFFLINE'" class="rounded border border-amber-200 px-2.5 py-1 text-xs font-medium text-amber-700 hover:bg-amber-50 disabled:opacity-40" @click="offline(item.id)">下线</button>
            <button class="rounded border border-red-200 px-2.5 py-1 text-xs font-medium text-red-600 hover:bg-red-50" @click="remove(item.id)">删除</button>
          </div>
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

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="modalOpen" class="fixed inset-0 z-[70] flex items-center justify-center p-4" @keydown.esc="closeModal">
          <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="closeModal" />
          <div class="modal-panel relative w-full max-w-lg rounded-2xl border border-border bg-white shadow-2xl" style="max-height: 90vh; overflow-y: auto;">
            <div class="flex items-center justify-between border-b border-border px-6 py-4">
              <h2 class="text-base font-semibold text-neutral-900">{{ editingAnnouncement ? '编辑公告' : '新建公告' }}</h2>
              <button class="rounded-lg p-1.5 text-neutral-400 transition hover:bg-neutral-100" @click="closeModal"><X class="h-4 w-4" /></button>
            </div>
            <form class="space-y-4 p-6" @submit.prevent="submitModal">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-neutral-700">公告标题 <span class="text-danger">*</span></label>
                <input v-model="modalForm.title" required placeholder="公告标题" :class="inputCls + ' w-full'" />
              </div>
              <div class="grid gap-3 sm:grid-cols-3">
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">类型</label>
                  <select v-model="modalForm.type" :class="inputCls + ' w-full'">
                    <option value="SYSTEM">系统公告</option>
                    <option value="COURSE">课程通知</option>
                    <option value="LIVE">直播通知</option>
                    <option value="EXAM">考试提醒</option>
                  </select>
                </div>
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">状态</label>
                  <select v-model="modalForm.status" :class="inputCls + ' w-full'">
                    <option value="DRAFT">草稿</option>
                    <option value="PUBLISHED">直接发布</option>
                  </select>
                </div>
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">置顶排序</label>
                  <input v-model.number="modalForm.pinned" min="0" type="number" :class="inputCls + ' w-full'" placeholder="0" />
                </div>
              </div>
              <label class="inline-flex w-fit items-center gap-2 text-sm text-neutral-600">
                <input v-model="modalForm.syncMessage" type="checkbox" class="h-4 w-4 accent-indigo-600" />
                发布时同步站内消息
              </label>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-neutral-700">公告内容 <span class="text-danger">*</span></label>
                <textarea v-model="modalForm.content" required rows="5" class="w-full rounded-lg border border-border bg-white px-3 py-2 text-sm outline-none transition placeholder:text-neutral-400 focus:border-primary-400 focus:ring-2 focus:ring-primary-100" placeholder="公告内容" />
              </div>
              <div class="flex justify-end gap-3 pt-2">
                <button type="button" class="rounded-md border border-border px-4 py-2 text-sm font-medium text-neutral-700 transition hover:bg-neutral-50" @click="closeModal">取消</button>
                <button type="submit" class="rounded-md bg-primary-600 px-5 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700">{{ editingAnnouncement ? '保存' : '创建' }}</button>
              </div>
            </form>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-active .modal-panel, .modal-leave-active .modal-panel { transition: transform 0.2s ease, opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .modal-panel, .modal-leave-to .modal-panel { transform: translateY(12px) scale(0.97); opacity: 0; }
</style>
