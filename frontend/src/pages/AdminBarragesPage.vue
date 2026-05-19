<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { MessageSquareWarning, Plus, Trash2 } from 'lucide-vue-next'
import { adminApi, type BarrageKeyword, type BarrageRecord, type PageResult } from '@/lib/api'

const page = ref<PageResult<BarrageRecord>>({ records: [], total: 0, size: 10, current: 1 })
const keywords = ref<BarrageKeyword[]>([])
const filters = reactive({ roomId: '' as number | '', keyword: '', startTime: '', endTime: '' })
const keywordForm = reactive({ id: 0, keyword: '' })
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const error = ref('')

async function loadBarrages(current = 1) {
  loading.value = true
  error.value = ''
  try {
    page.value = await adminApi.barrages({ current, size: page.value.size, ...filters })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '弹幕记录加载失败'
  } finally {
    loading.value = false
  }
}

async function loadKeywords() {
  try {
    keywords.value = await adminApi.barrageKeywords()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '屏蔽词加载失败'
  }
}

async function saveKeyword() {
  if (!keywordForm.keyword.trim()) {
    error.value = '请输入屏蔽词'
    return
  }
  saving.value = true
  error.value = ''
  try {
    if (keywordForm.id > 0) {
      await adminApi.updateBarrageKeyword(keywordForm.id, { keyword: keywordForm.keyword.trim() })
      message.value = '屏蔽词已更新'
    } else {
      await adminApi.createBarrageKeyword({ keyword: keywordForm.keyword.trim() })
      message.value = '屏蔽词已新增'
    }
    resetKeywordForm()
    await loadKeywords()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '屏蔽词保存失败'
  } finally {
    saving.value = false
  }
}

function editKeyword(item: BarrageKeyword) {
  keywordForm.id = item.id
  keywordForm.keyword = item.keyword
}

function resetKeywordForm() {
  keywordForm.id = 0
  keywordForm.keyword = ''
}

async function removeKeyword(item: BarrageKeyword) {
  if (!window.confirm(`确认删除屏蔽词「${item.keyword}」？`)) return
  error.value = ''
  try {
    await adminApi.deleteBarrageKeyword(item.id)
    message.value = '屏蔽词已删除'
    if (keywordForm.id === item.id) resetKeywordForm()
    await loadKeywords()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '屏蔽词删除失败'
  }
}

async function removeBarrage(item: BarrageRecord) {
  if (!window.confirm(`确认删除这条弹幕「${item.content}」？`)) return
  error.value = ''
  try {
    await adminApi.deleteBarrage(item.id)
    message.value = '违规弹幕已删除'
    await loadBarrages(page.value.current)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '弹幕删除失败'
  }
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

onMounted(async () => {
  await Promise.all([loadBarrages(), loadKeywords()])
})
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/admin" class="text-sm text-cyan-200">返回后台</RouterLink>
        <RouterLink to="/live" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">直播中心</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <div class="flex flex-wrap items-center justify-between gap-5">
          <div>
            <p class="text-sm text-cyan-200">管理员弹幕管理</p>
            <h1 class="mt-2 text-4xl font-black">弹幕记录审查与关键词屏蔽</h1>
            <p class="mt-3 max-w-3xl text-slate-300">按直播间、内容关键词和发送时间定位违规弹幕，维护屏蔽词后聊天与弹幕发送会被后端拦截。</p>
          </div>
          <div class="rounded-[2rem] border border-cyan-300/30 bg-cyan-300/10 p-5 text-cyan-100">
            <MessageSquareWarning class="h-10 w-10" />
          </div>
        </div>
        <p v-if="message" class="mt-4 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <section class="mt-8 grid gap-6 lg:grid-cols-[1fr_360px]">
        <div class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <form class="grid gap-4 md:grid-cols-[160px_1fr_190px_190px_110px]" @submit.prevent="loadBarrages(1)">
            <input v-model="filters.roomId" type="number" min="1" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="直播间ID" />
            <input v-model="filters.keyword" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="弹幕关键词" />
            <input v-model="filters.startTime" type="datetime-local" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
            <input v-model="filters.endTime" type="datetime-local" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
            <button class="rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">筛选</button>
          </form>

          <div class="mt-6 overflow-hidden rounded-3xl border border-white/10">
            <table class="w-full min-w-[820px] text-left text-sm">
              <thead class="bg-white/10 text-slate-300">
                <tr>
                  <th class="px-5 py-4">内容</th>
                  <th class="px-5 py-4">直播间</th>
                  <th class="px-5 py-4">用户</th>
                  <th class="px-5 py-4">时间</th>
                  <th class="px-5 py-4">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in page.records" :key="item.id" class="border-t border-white/10">
                  <td class="px-5 py-4 font-bold text-white">{{ item.content }}</td>
                  <td class="px-5 py-4 text-slate-300">#{{ item.roomId }} {{ item.roomTitle || '' }}</td>
                  <td class="px-5 py-4 text-slate-300">#{{ item.userId }} {{ item.username || '' }}</td>
                  <td class="px-5 py-4 text-slate-300">{{ formatTime(item.createdAt) }}</td>
                  <td class="px-5 py-4">
                    <button class="inline-flex items-center gap-1 rounded-full bg-red-400/15 px-3 py-1 text-xs font-bold text-red-200 hover:bg-red-400/25" @click="removeBarrage(item)">
                      <Trash2 class="h-3 w-3" />删除
                    </button>
                  </td>
                </tr>
                <tr v-if="!loading && page.records.length === 0">
                  <td colspan="5" class="px-5 py-8 text-center text-slate-400">暂无弹幕记录</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="mt-5 flex items-center justify-between text-sm text-slate-300">
            <span>共 {{ page.total }} 条，第 {{ page.current }} 页</span>
            <div class="flex gap-2">
              <button class="rounded-full border border-white/10 px-4 py-2 disabled:opacity-40" :disabled="page.current <= 1" @click="loadBarrages(page.current - 1)">上一页</button>
              <button class="rounded-full border border-white/10 px-4 py-2 disabled:opacity-40" :disabled="page.current * page.size >= page.total" @click="loadBarrages(page.current + 1)">下一页</button>
            </div>
          </div>
        </div>

        <aside class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <h2 class="text-2xl font-black">屏蔽词维护</h2>
          <form class="mt-5 flex gap-2" @submit.prevent="saveKeyword">
            <input v-model="keywordForm.keyword" class="min-w-0 flex-1 rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="输入屏蔽词" />
            <button class="rounded-2xl bg-cyan-300 px-4 text-slate-950 disabled:opacity-50" :disabled="saving">
              <Plus class="h-4 w-4" />
            </button>
          </form>
          <button v-if="keywordForm.id > 0" class="mt-3 text-sm text-cyan-200" @click="resetKeywordForm">取消编辑</button>

          <div class="mt-6 space-y-3">
            <div v-for="item in keywords" :key="item.id" class="flex items-center justify-between gap-3 rounded-2xl border border-white/10 bg-slate-900/80 p-4">
              <button class="text-left font-bold text-white" @click="editKeyword(item)">{{ item.keyword }}</button>
              <button class="rounded-full border border-red-300/20 px-3 py-1 text-xs text-red-200 hover:bg-red-400/10" @click="removeKeyword(item)">删除</button>
            </div>
            <p v-if="keywords.length === 0" class="rounded-2xl bg-slate-900/80 p-4 text-sm text-slate-400">暂无屏蔽词</p>
          </div>
        </aside>
      </section>
    </div>
  </main>
</template>
