<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus, Trash2 } from 'lucide-vue-next'
import { adminApi, type BarrageKeyword, type BarrageRecord, type PageResult } from '@/lib/api'

const page = ref<PageResult<BarrageRecord>>({ records: [], total: 0, size: 10, current: 1 })
const keywords = ref<BarrageKeyword[]>([])
const filters = reactive({ roomId: '' as number | '', keyword: '', startTime: '', endTime: '' })
const keywordForm = reactive({ id: 0, keyword: '' })
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const error = ref('')
const inputCls = 'h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100'

async function loadBarrages(current = 1) {
  loading.value = true; error.value = ''
  try { page.value = await adminApi.barrages({ current, size: page.value.size, ...filters }) }
  catch (err) { error.value = err instanceof Error ? err.message : '弹幕记录加载失败' }
  finally { loading.value = false }
}

async function loadKeywords() {
  try { keywords.value = await adminApi.barrageKeywords() }
  catch (err) { error.value = err instanceof Error ? err.message : '屏蔽词加载失败' }
}

async function saveKeyword() {
  if (!keywordForm.keyword.trim()) { error.value = '请输入屏蔽词'; return }
  saving.value = true; error.value = ''
  try {
    if (keywordForm.id > 0) { await adminApi.updateBarrageKeyword(keywordForm.id, { keyword: keywordForm.keyword.trim() }); message.value = '屏蔽词已更新' }
    else { await adminApi.createBarrageKeyword({ keyword: keywordForm.keyword.trim() }); message.value = '屏蔽词已新增' }
    resetKeywordForm(); await loadKeywords()
  } catch (err) { error.value = err instanceof Error ? err.message : '屏蔽词保存失败' }
  finally { saving.value = false }
}

function editKeyword(item: BarrageKeyword) { keywordForm.id = item.id; keywordForm.keyword = item.keyword }
function resetKeywordForm() { keywordForm.id = 0; keywordForm.keyword = '' }

async function removeKeyword(item: BarrageKeyword) {
  if (!window.confirm(`确认删除屏蔽词「${item.keyword}」？`)) return
  error.value = ''
  try {
    await adminApi.deleteBarrageKeyword(item.id); message.value = '屏蔽词已删除'
    if (keywordForm.id === item.id) resetKeywordForm(); await loadKeywords()
  } catch (err) { error.value = err instanceof Error ? err.message : '屏蔽词删除失败' }
}

async function removeBarrage(item: BarrageRecord) {
  if (!window.confirm(`确认删除这条弹幕「${item.content}」？`)) return
  error.value = ''
  try {
    await adminApi.deleteBarrage(item.id); message.value = '违规弹幕已删除'; await loadBarrages(page.value.current)
  } catch (err) { error.value = err instanceof Error ? err.message : '弹幕删除失败' }
}

function formatTime(value?: string) { return value ? value.replace('T', ' ').slice(0, 19) : '-' }

onMounted(async () => { await Promise.all([loadBarrages(), loadKeywords()]) })
</script>

<template>
  <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">管理员弹幕管理</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">弹幕记录审查与关键词屏蔽</h1>
        <p class="mt-1 text-sm text-neutral-500">按直播间、内容关键词和发送时间定位违规弹幕，维护屏蔽词后聊天与弹幕发送会被后端拦截。</p>
      </div>

      <div v-if="message" class="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</div>
      <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

      <div class="grid gap-6 lg:grid-cols-[1fr_320px]">
        <!-- Barrage list -->
        <div class="rounded-xl border border-border bg-white p-5 shadow-sm">
          <form class="mb-4 flex flex-wrap gap-3" @submit.prevent="loadBarrages(1)">
            <input v-model="filters.roomId" type="number" min="1" :class="inputCls + ' w-32'" placeholder="直播间ID" />
            <input v-model="filters.keyword" :class="inputCls + ' flex-1 min-w-32'" placeholder="弹幕关键词" />
            <input v-model="filters.startTime" type="datetime-local" :class="inputCls + ' w-48'" />
            <input v-model="filters.endTime" type="datetime-local" :class="inputCls + ' w-48'" />
            <button class="inline-flex h-9 items-center rounded-lg bg-primary-600 px-4 text-sm font-semibold text-white hover:bg-primary-700">筛选</button>
          </form>

          <div class="overflow-auto rounded-lg border border-border">
            <table class="w-full min-w-[640px] text-sm">
              <thead class="border-b border-border bg-neutral-100">
                <tr class="text-xs font-medium text-neutral-500">
                  <th class="px-4 py-3 text-left">内容</th>
                  <th class="px-4 py-3 text-left w-36">直播间</th>
                  <th class="px-4 py-3 text-left w-28">用户</th>
                  <th class="px-4 py-3 text-left w-36">时间</th>
                  <th class="px-4 py-3 text-left w-20">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in page.records" :key="item.id" class="border-b border-border hover:bg-neutral-100/50">
                  <td class="px-4 py-3 font-medium text-neutral-900">{{ item.content }}</td>
                  <td class="px-4 py-3 text-neutral-500">#{{ item.roomId }} {{ item.roomTitle || '' }}</td>
                  <td class="px-4 py-3 text-neutral-500">#{{ item.userId }}</td>
                  <td class="px-4 py-3 text-xs text-neutral-400">{{ formatTime(item.createdAt) }}</td>
                  <td class="px-4 py-3">
                    <button class="inline-flex items-center gap-1 rounded border border-red-200 px-2 py-1 text-xs text-red-600 hover:bg-red-50" @click="removeBarrage(item)">
                      <Trash2 class="h-3 w-3" />删除
                    </button>
                  </td>
                </tr>
                <tr v-if="!loading && page.records.length === 0">
                  <td colspan="5" class="px-4 py-8 text-center text-neutral-400">暂无弹幕记录</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="mt-4 flex items-center justify-between text-xs text-neutral-400">
            <span>共 {{ page.total }} 条，第 {{ page.current }} 页</span>
            <div class="flex gap-1.5">
              <button :disabled="page.current <= 1" class="rounded border border-border px-3 py-1.5 disabled:opacity-40 hover:bg-neutral-100" @click="loadBarrages(page.current - 1)">上一页</button>
              <button :disabled="page.current * page.size >= page.total" class="rounded border border-border px-3 py-1.5 disabled:opacity-40 hover:bg-neutral-100" @click="loadBarrages(page.current + 1)">下一页</button>
            </div>
          </div>
        </div>

        <!-- Keywords sidebar -->
        <aside class="rounded-xl border border-border bg-white p-5 shadow-sm">
          <h2 class="mb-4 font-semibold text-neutral-900">屏蔽词维护</h2>
          <form class="flex gap-2" @submit.prevent="saveKeyword">
            <input v-model="keywordForm.keyword" :class="inputCls + ' flex-1'" placeholder="输入屏蔽词" />
            <button class="inline-flex h-9 w-9 items-center justify-center rounded-lg bg-primary-600 text-white hover:bg-primary-700 disabled:opacity-50" :disabled="saving">
              <Plus class="h-4 w-4" />
            </button>
          </form>
          <button v-if="keywordForm.id > 0" class="mt-2 text-xs text-primary-600" @click="resetKeywordForm">取消编辑</button>

          <div class="mt-5 space-y-2">
            <div v-for="item in keywords" :key="item.id" class="flex items-center justify-between gap-2 rounded-lg border border-border p-3">
              <button class="text-sm font-medium text-neutral-800 hover:text-primary-600" @click="editKeyword(item)">{{ item.keyword }}</button>
              <button class="rounded border border-red-200 px-2 py-0.5 text-xs text-red-600 hover:bg-red-50" @click="removeKeyword(item)">删除</button>
            </div>
            <p v-if="keywords.length === 0" class="rounded-lg bg-neutral-100 p-3 text-xs text-neutral-400">暂无屏蔽词</p>
          </div>
        </aside>
      </div>
  </div>
</template>
