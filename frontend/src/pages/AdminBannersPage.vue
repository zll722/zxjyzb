<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus, Save, Trash2 } from 'lucide-vue-next'
import { apiUrl, bannerApi, type SiteBanner } from '@/lib/api'

const filters = reactive({ status: '' })
const form = reactive({ title: '', subtitle: '', imageUrl: '', linkUrl: '/', sort: 0, status: 'ENABLED' })
const editForms = reactive<Record<number, { title: string; subtitle: string; imageUrl: string; linkUrl: string; sort: number; status: 'ENABLED' | 'DISABLED' }>>({})
const banners = ref<SiteBanner[]>([])
const loading = ref(false)
const message = ref('')
const error = ref('')

const statusLabels: Record<string, string> = { ENABLED: '展示中', DISABLED: '已隐藏' }
const sampleImageUrl = apiUrl('/banners/sample-image')
const inputCls = 'h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100'

async function load() {
  loading.value = true; error.value = ''
  try {
    banners.value = await bannerApi.adminList(filters)
    banners.value.forEach((item) => {
      editForms[item.id] = { title: item.title, subtitle: item.subtitle || '', imageUrl: item.imageUrl, linkUrl: item.linkUrl || '', sort: item.sort, status: item.status }
    })
  } catch (err) { error.value = err instanceof Error ? err.message : '轮播加载失败' }
  finally { loading.value = false }
}

async function createBanner() {
  message.value = ''; error.value = ''
  try {
    await bannerApi.create(form)
    Object.assign(form, { title: '', subtitle: '', imageUrl: '', linkUrl: '/', sort: 0, status: 'ENABLED' })
    message.value = '轮播已创建'; await load()
  } catch (err) { error.value = err instanceof Error ? err.message : '创建失败' }
}

async function save(item: SiteBanner) {
  await bannerApi.update(item.id, editForms[item.id]); message.value = '轮播已保存'; await load()
}

async function remove(id: number) {
  await bannerApi.remove(id); message.value = '轮播已删除'; await load()
}

onMounted(load)
</script>

<template>
  <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">管理员轮播管理</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">首页轮播配置</h1>
        <p class="mt-1 text-sm text-neutral-500">配置后会优先展示在首页首屏轮播区，可指向课程、直播、回放或公告页面。</p>
      </div>

      <!-- Filter + messages -->
      <div class="mb-6 rounded-xl border border-border bg-white p-4 shadow-sm">
        <form class="flex flex-wrap gap-3" @submit.prevent="load">
          <select v-model="filters.status" :class="inputCls + ' w-36'">
            <option value="">全部状态</option>
            <option value="ENABLED">展示中</option>
            <option value="DISABLED">已隐藏</option>
          </select>
          <button class="inline-flex h-9 items-center rounded-lg bg-primary-600 px-4 text-sm font-semibold text-white hover:bg-primary-700">筛选</button>
        </form>
        <div v-if="message" class="mt-3 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{{ message }}</div>
        <div v-if="error" class="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{{ error }}</div>
      </div>

      <!-- Create form -->
      <div class="mb-6 rounded-xl border border-border bg-white p-5 shadow-sm">
        <h2 class="mb-4 font-semibold text-neutral-900">新建轮播</h2>
        <form class="grid gap-3" @submit.prevent="createBanner">
          <div class="grid gap-3 md:grid-cols-[1fr_1fr_100px_140px]">
            <input v-model="form.title" required :class="inputCls" placeholder="标题" />
            <input v-model="form.subtitle" :class="inputCls" placeholder="副标题" />
            <input v-model.number="form.sort" min="0" type="number" :class="inputCls" placeholder="排序" />
            <select v-model="form.status" :class="inputCls">
              <option value="ENABLED">展示中</option>
              <option value="DISABLED">隐藏</option>
            </select>
          </div>
          <div class="grid gap-3 md:grid-cols-2">
            <input v-model="form.imageUrl" required :class="inputCls" :placeholder="sampleImageUrl" />
            <input v-model="form.linkUrl" :class="inputCls" placeholder="跳转地址，如 /courses" />
          </div>
          <button class="inline-flex h-9 w-fit items-center gap-1.5 rounded-lg bg-primary-600 px-4 text-sm font-semibold text-white hover:bg-primary-700">
            <Plus class="h-4 w-4" />创建轮播
          </button>
        </form>
      </div>

      <!-- Banner list -->
      <div v-if="loading" class="py-6 text-center text-sm text-neutral-400">轮播加载中...</div>
      <div v-else-if="!banners.length" class="rounded-xl border border-border bg-white p-8 text-center text-sm text-neutral-400">暂无轮播</div>
      <div v-else class="grid gap-5">
        <article v-for="item in banners" :key="item.id" class="overflow-hidden rounded-xl border border-border bg-white shadow-sm">
          <img :src="item.imageUrl" :alt="item.title" class="h-44 w-full object-cover" />
          <div class="p-5">
            <div class="mb-4 flex items-center justify-between gap-3">
              <span class="rounded-full px-2.5 py-0.5 text-xs font-medium" :class="item.status === 'ENABLED' ? 'bg-success-bg text-success' : 'bg-neutral-100 text-neutral-500'">{{ statusLabels[item.status] || item.status }}</span>
              <button class="inline-flex items-center gap-1 rounded border border-red-200 px-2.5 py-1 text-xs text-red-600 hover:bg-red-50" @click="remove(item.id)"><Trash2 class="h-3.5 w-3.5" />删除</button>
            </div>
            <div class="grid gap-3">
              <input v-model="editForms[item.id].title" :class="inputCls + ' font-semibold w-full'" />
              <input v-model="editForms[item.id].subtitle" :class="inputCls + ' w-full'" placeholder="副标题" />
              <div class="grid gap-3 md:grid-cols-[1fr_1fr_100px_140px]">
                <input v-model="editForms[item.id].imageUrl" :class="inputCls" placeholder="图片 URL" />
                <input v-model="editForms[item.id].linkUrl" :class="inputCls" placeholder="跳转链接" />
                <input v-model.number="editForms[item.id].sort" min="0" type="number" :class="inputCls" />
                <select v-model="editForms[item.id].status" :class="inputCls">
                  <option value="ENABLED">展示中</option>
                  <option value="DISABLED">隐藏</option>
                </select>
              </div>
              <button class="inline-flex h-9 w-fit items-center gap-1.5 rounded-lg bg-primary-600 px-4 text-sm font-semibold text-white hover:bg-primary-700" @click="save(item)">
                <Save class="h-4 w-4" />保存修改
              </button>
            </div>
          </div>
        </article>
      </div>
  </div>
</template>
