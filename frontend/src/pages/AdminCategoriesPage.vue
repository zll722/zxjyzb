<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Pencil, Plus, RotateCcw, Trash2 } from 'lucide-vue-next'
import { categoryApi, type Category } from '@/lib/api'

const categories = ref<Category[]>([])
const form = reactive({ id: 0, name: '', sort: 0 })
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const error = ref('')

const editing = () => form.id > 0

async function load() {
  loading.value = true
  error.value = ''
  try {
    categories.value = await categoryApi.list()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '分类加载失败'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = 0
  form.name = ''
  form.sort = 0
}

function editCategory(category: Category) {
  form.id = category.id
  form.name = category.name
  form.sort = category.sort
}

async function saveCategory() {
  if (!form.name.trim()) {
    error.value = '请输入分类名称'
    return
  }
  saving.value = true
  error.value = ''
  try {
    if (editing()) {
      await categoryApi.update(form.id, { name: form.name.trim(), sort: form.sort })
      message.value = '分类已更新'
    } else {
      await categoryApi.create({ name: form.name.trim(), sort: form.sort })
      message.value = '分类已新增'
    }
    resetForm()
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '分类保存失败'
  } finally {
    saving.value = false
  }
}

async function removeCategory(category: Category) {
  if (!window.confirm(`确认删除分类「${category.name}」？`)) return
  error.value = ''
  try {
    await categoryApi.remove(category.id)
    message.value = '分类已删除'
    if (form.id === category.id) resetForm()
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '分类删除失败，请确认没有课程正在使用该分类'
  }
}

onMounted(() => load())
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/admin" class="text-sm text-cyan-200">返回后台</RouterLink>
        <RouterLink to="/admin/courses" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">课程审核</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">管理员分类管理</p>
        <h1 class="mt-2 text-4xl font-black">课程一级分类配置</h1>
        <p class="mt-3 max-w-3xl text-slate-300">分类用于课程创建、课程筛选和课程审核展示，删除分类前请确认没有课程正在关联使用。</p>
        <p v-if="message" class="mt-4 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <section class="mt-8 grid gap-6 lg:grid-cols-[420px_1fr]">
        <form class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur" @submit.prevent="saveCategory">
          <p class="text-sm text-cyan-200">{{ editing() ? '编辑分类' : '新增分类' }}</p>
          <h2 class="mt-2 text-2xl font-black">{{ editing() ? form.name : '创建课程分类' }}</h2>
          <div class="mt-6 space-y-4">
            <input v-model="form.name" class="w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="分类名称" />
            <input v-model.number="form.sort" type="number" class="w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="排序值" />
          </div>
          <div class="mt-6 flex flex-wrap gap-3">
            <button :disabled="saving" class="inline-flex items-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200 disabled:opacity-50">
              <Plus v-if="!editing()" class="h-4 w-4" />
              <Pencil v-else class="h-4 w-4" />{{ editing() ? '保存修改' : '新增分类' }}
            </button>
            <button type="button" class="inline-flex items-center gap-2 rounded-2xl border border-white/10 px-5 py-3 text-sm hover:bg-white/10" @click="resetForm">
              <RotateCcw class="h-4 w-4" />重置
            </button>
          </div>
        </form>

        <div class="overflow-hidden rounded-[2rem] border border-white/10 bg-white/8 backdrop-blur">
          <div class="grid grid-cols-[1fr_120px_180px] gap-4 border-b border-white/10 px-6 py-4 text-sm text-slate-400">
            <span>分类名称</span><span>排序</span><span>操作</span>
          </div>
          <p v-if="loading" class="p-6 text-slate-300">分类加载中...</p>
          <p v-else-if="!categories.length" class="p-6 text-slate-300">暂无分类</p>
          <article v-for="category in categories" v-else :key="category.id" class="grid grid-cols-[1fr_120px_180px] items-center gap-4 border-b border-white/10 px-6 py-4 last:border-b-0">
            <div>
              <p class="font-bold text-white">{{ category.name }}</p>
              <p class="text-xs text-slate-400">ID {{ category.id }}</p>
            </div>
            <span class="text-sm text-slate-300">{{ category.sort }}</span>
            <div class="flex flex-wrap gap-2">
              <button class="inline-flex items-center gap-1 rounded-full border border-white/10 px-3 py-1 text-xs hover:bg-white/10" @click="editCategory(category)">
                <Pencil class="h-3 w-3" />编辑
              </button>
              <button class="inline-flex items-center gap-1 rounded-full border border-red-300/40 px-3 py-1 text-xs text-red-100 hover:bg-red-400/10" @click="removeCategory(category)">
                <Trash2 class="h-3 w-3" />删除
              </button>
            </div>
          </article>
        </div>
      </section>
    </div>
  </main>
</template>
