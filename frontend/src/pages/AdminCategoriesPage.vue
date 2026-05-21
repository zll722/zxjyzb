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
const inputCls = 'h-9 w-full rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100'

const editing = () => form.id > 0

async function load() {
  loading.value = true; error.value = ''
  try { categories.value = await categoryApi.list() }
  catch (err) { error.value = err instanceof Error ? err.message : '分类加载失败' }
  finally { loading.value = false }
}

function resetForm() { form.id = 0; form.name = ''; form.sort = 0 }
function editCategory(category: Category) { form.id = category.id; form.name = category.name; form.sort = category.sort }

async function saveCategory() {
  if (!form.name.trim()) { error.value = '请输入分类名称'; return }
  saving.value = true; error.value = ''
  try {
    if (editing()) { await categoryApi.update(form.id, { name: form.name.trim(), sort: form.sort }); message.value = '分类已更新' }
    else { await categoryApi.create({ name: form.name.trim(), sort: form.sort }); message.value = '分类已新增' }
    resetForm(); await load()
  } catch (err) { error.value = err instanceof Error ? err.message : '分类保存失败' }
  finally { saving.value = false }
}

async function removeCategory(category: Category) {
  if (!window.confirm(`确认删除分类「${category.name}」？`)) return
  error.value = ''
  try {
    await categoryApi.remove(category.id); message.value = '分类已删除'
    if (form.id === category.id) resetForm(); await load()
  } catch (err) { error.value = err instanceof Error ? err.message : '分类删除失败，请确认没有课程正在使用该分类' }
}

onMounted(() => load())
</script>

<template>
  <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">管理员分类管理</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">课程一级分类配置</h1>
        <p class="mt-1 text-sm text-neutral-500">分类用于课程创建、课程筛选和课程审核展示，删除分类前请确认没有课程正在关联使用。</p>
      </div>

      <div v-if="message" class="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</div>
      <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

      <div class="grid gap-6 lg:grid-cols-[360px_1fr]">
        <!-- Form -->
        <form class="rounded-xl border border-border bg-white p-5 shadow-sm" @submit.prevent="saveCategory">
          <p class="text-xs font-medium text-primary-600">{{ editing() ? '编辑分类' : '新增分类' }}</p>
          <h2 class="mt-1 font-semibold text-neutral-900">{{ editing() ? form.name : '创建课程分类' }}</h2>
          <div class="mt-5 space-y-3">
            <input v-model="form.name" :class="inputCls" placeholder="分类名称" />
            <input v-model.number="form.sort" type="number" :class="inputCls" placeholder="排序值" />
          </div>
          <div class="mt-5 flex gap-3">
            <button :disabled="saving" class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-primary-600 px-4 text-sm font-semibold text-white hover:bg-primary-700 disabled:opacity-50">
              <Plus v-if="!editing()" class="h-4 w-4" />
              <Pencil v-else class="h-4 w-4" />{{ editing() ? '保存修改' : '新增分类' }}
            </button>
            <button type="button" class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-border px-3 text-sm text-neutral-600 hover:bg-neutral-100" @click="resetForm">
              <RotateCcw class="h-4 w-4" />重置
            </button>
          </div>
        </form>

        <!-- Category table -->
        <div class="overflow-hidden rounded-xl border border-border bg-white shadow-sm">
          <div class="grid grid-cols-[1fr_80px_160px] gap-4 border-b border-border bg-neutral-100 px-4 py-3 text-xs font-medium text-neutral-500">
            <span>分类名称</span><span>排序</span><span>操作</span>
          </div>
          <p v-if="loading" class="px-4 py-6 text-sm text-neutral-400">分类加载中...</p>
          <p v-else-if="!categories.length" class="px-4 py-6 text-sm text-neutral-400">暂无分类</p>
          <article v-for="category in categories" v-else :key="category.id" class="grid grid-cols-[1fr_80px_160px] items-center gap-4 border-b border-border px-4 py-3 last:border-b-0 hover:bg-neutral-100/50">
            <div>
              <p class="font-medium text-neutral-900">{{ category.name }}</p>
              <p class="text-xs text-neutral-400">ID {{ category.id }}</p>
            </div>
            <span class="text-sm text-neutral-500">{{ category.sort }}</span>
            <div class="flex gap-1.5">
              <button class="inline-flex items-center gap-1 rounded border border-border px-2.5 py-1 text-xs hover:bg-neutral-100" @click="editCategory(category)">
                <Pencil class="h-3 w-3" />编辑
              </button>
              <button class="inline-flex items-center gap-1 rounded border border-red-200 px-2.5 py-1 text-xs text-red-600 hover:bg-red-50" @click="removeCategory(category)">
                <Trash2 class="h-3 w-3" />删除
              </button>
            </div>
          </article>
        </div>
      </div>
  </div>
</template>
