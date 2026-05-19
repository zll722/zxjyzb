<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Image, Plus, Trash2 } from 'lucide-vue-next'
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

async function load() {
  loading.value = true
  error.value = ''
  try {
    banners.value = await bannerApi.adminList(filters)
    banners.value.forEach((item) => {
      editForms[item.id] = { title: item.title, subtitle: item.subtitle || '', imageUrl: item.imageUrl, linkUrl: item.linkUrl || '', sort: item.sort, status: item.status }
    })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '轮播加载失败'
  } finally {
    loading.value = false
  }
}

async function createBanner() {
  message.value = ''
  error.value = ''
  try {
    await bannerApi.create(form)
    form.title = ''
    form.subtitle = ''
    form.imageUrl = ''
    form.linkUrl = '/'
    form.sort = 0
    form.status = 'ENABLED'
    message.value = '轮播已创建'
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '创建失败'
  }
}

async function save(item: SiteBanner) {
  await bannerApi.update(item.id, editForms[item.id])
  message.value = '轮播已保存'
  await load()
}

async function remove(id: number) {
  await bannerApi.remove(id)
  message.value = '轮播已删除'
  await load()
}

onMounted(load)
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/admin" class="text-sm text-cyan-200">返回后台</RouterLink>
        <RouterLink to="/" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">查看首页</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <div class="flex flex-wrap items-center justify-between gap-6">
          <div>
            <p class="text-sm text-cyan-200">管理员轮播管理</p>
            <h1 class="mt-2 text-4xl font-black">首页轮播配置</h1>
            <p class="mt-3 max-w-3xl text-slate-300">配置后会优先展示在首页首屏轮播区，可指向课程、直播、回放或公告页面。</p>
          </div>
          <div class="rounded-[2rem] border border-cyan-300/30 bg-cyan-300/10 p-5 text-cyan-100">
            <Image class="h-10 w-10" />
          </div>
        </div>

        <form class="mt-8 grid gap-4 md:grid-cols-[1fr_180px_140px]" @submit.prevent="load">
          <select v-model="filters.status" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
            <option value="">全部状态</option>
            <option value="ENABLED">展示中</option>
            <option value="DISABLED">已隐藏</option>
          </select>
          <button class="rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">筛选</button>
        </form>
        <p v-if="message" class="mt-4 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <section class="mt-8 rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
        <h2 class="text-2xl font-black">新建轮播</h2>
        <form class="mt-5 grid gap-4" @submit.prevent="createBanner">
          <div class="grid gap-4 md:grid-cols-[1fr_1fr_120px_140px]">
            <input v-model="form.title" required class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="标题" />
            <input v-model="form.subtitle" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="副标题" />
            <input v-model.number="form.sort" min="0" type="number" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="排序" />
            <select v-model="form.status" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
              <option value="ENABLED">展示中</option>
              <option value="DISABLED">隐藏</option>
            </select>
          </div>
          <div class="grid gap-4 md:grid-cols-[1fr_1fr]">
            <input v-model="form.imageUrl" required class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" :placeholder="sampleImageUrl" />
            <input v-model="form.linkUrl" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="跳转地址，如 /courses" />
          </div>
          <button class="inline-flex w-fit items-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">
            <Plus class="h-4 w-4" />创建轮播
          </button>
        </form>
      </section>

      <section class="mt-8 grid gap-5">
        <p v-if="loading" class="text-slate-300">轮播加载中...</p>
        <p v-else-if="!banners.length" class="rounded-[2rem] border border-white/10 bg-white/8 p-8 text-slate-300">暂无轮播</p>
        <article v-for="item in banners" v-else :key="item.id" class="overflow-hidden rounded-[2rem] border border-white/10 bg-white/8 backdrop-blur">
          <img :src="item.imageUrl" :alt="item.title" class="h-52 w-full object-cover" />
          <div class="p-6">
            <div class="flex flex-wrap items-center justify-between gap-4">
              <span class="rounded-full bg-cyan-300/15 px-3 py-1 text-xs font-bold text-cyan-100">{{ statusLabels[item.status] || item.status }}</span>
              <button class="inline-flex items-center gap-2 rounded-full border border-red-300/40 px-4 py-2 text-sm text-red-100" @click="remove(item.id)"><Trash2 class="h-4 w-4" />删除</button>
            </div>
            <div class="mt-5 grid gap-4">
              <input v-model="editForms[item.id].title" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 font-bold outline-none focus:border-cyan-300" />
              <input v-model="editForms[item.id].subtitle" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="副标题" />
              <div class="grid gap-4 md:grid-cols-[1fr_1fr_120px_140px]">
                <input v-model="editForms[item.id].imageUrl" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
                <input v-model="editForms[item.id].linkUrl" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
                <input v-model.number="editForms[item.id].sort" min="0" type="number" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
                <select v-model="editForms[item.id].status" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
                  <option value="ENABLED">展示中</option>
                  <option value="DISABLED">隐藏</option>
                </select>
              </div>
              <button class="w-fit rounded-full bg-cyan-300 px-5 py-2 text-sm font-bold text-slate-950 hover:bg-cyan-200" @click="save(item)">保存修改</button>
            </div>
          </div>
        </article>
      </section>
    </div>
  </main>
</template>
