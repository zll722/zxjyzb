<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Pencil, Plus, Trash2, X } from 'lucide-vue-next'
import { categoryApi, type Category } from '@/lib/api'

const categories = ref<Category[]>([])
const loading = ref(false)
const message = ref('')
const error = ref('')

const modalOpen = ref(false)
const editingCategory = ref<Category | null>(null)
const modalForm = reactive({ name: '', sort: 0 })
const saving = ref(false)

async function load() {
  loading.value = true; error.value = ''
  try { categories.value = await categoryApi.list() }
  catch (err) { error.value = err instanceof Error ? err.message : '分类加载失败' }
  finally { loading.value = false }
}

function openCreateModal() {
  editingCategory.value = null
  modalForm.name = ''
  modalForm.sort = 0
  modalOpen.value = true
}

function openEditModal(category: Category) {
  editingCategory.value = category
  modalForm.name = category.name
  modalForm.sort = category.sort
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
  editingCategory.value = null
}

async function submitModal() {
  if (!modalForm.name.trim()) { error.value = '请输入分类名称'; return }
  saving.value = true; error.value = ''
  try {
    if (editingCategory.value) {
      await categoryApi.update(editingCategory.value.id, { name: modalForm.name.trim(), sort: modalForm.sort })
      message.value = '分类已更新'
    } else {
      await categoryApi.create({ name: modalForm.name.trim(), sort: modalForm.sort })
      message.value = '分类已新增'
    }
    closeModal()
    await load()
  } catch (err) { error.value = err instanceof Error ? err.message : '分类保存失败' }
  finally { saving.value = false }
}

async function removeCategory(category: Category) {
  if (!window.confirm(`确认删除分类「${category.name}」？`)) return
  error.value = ''
  try {
    await categoryApi.remove(category.id); message.value = '分类已删除'
    await load()
  } catch (err) { error.value = err instanceof Error ? err.message : '分类删除失败，请确认没有课程正在使用该分类' }
}

onMounted(() => load())
</script>

<template>
  <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <p class="text-sm font-medium text-primary-600">管理员分类管理</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">课程一级分类配置</h1>
        <p class="mt-1 text-sm text-neutral-500">分类用于课程创建、课程筛选和课程审核展示，删除分类前请确认没有课程正在关联使用。</p>
      </div>
      <button
        class="inline-flex items-center gap-2 rounded-xl bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700"
        @click="openCreateModal"
      >
        <Plus class="h-4 w-4" />新增分类
      </button>
    </div>

    <div v-if="message" class="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</div>
    <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

    <div class="overflow-hidden rounded-xl border border-border bg-white shadow-sm">
      <div class="grid grid-cols-[1fr_80px_160px] gap-4 border-b border-border bg-neutral-100 px-4 py-3 text-xs font-medium text-neutral-500">
        <span>分类名称</span><span>排序</span><span>操作</span>
      </div>
      <p v-if="loading" class="px-4 py-6 text-sm text-neutral-400">分类加载中...</p>
      <p v-else-if="!categories.length" class="px-4 py-6 text-sm text-neutral-400">暂无分类，点击右上角新增</p>
      <article
        v-for="category in categories"
        v-else
        :key="category.id"
        class="grid grid-cols-[1fr_80px_160px] items-center gap-4 border-b border-border px-4 py-3 last:border-b-0 hover:bg-neutral-100/50"
      >
        <div>
          <p class="font-medium text-neutral-900">{{ category.name }}</p>
          <p class="text-xs text-neutral-400">ID {{ category.id }}</p>
        </div>
        <span class="text-sm text-neutral-500">{{ category.sort }}</span>
        <div class="flex gap-1.5">
          <button class="inline-flex items-center gap-1 rounded border border-border px-2.5 py-1 text-xs hover:bg-neutral-100" @click="openEditModal(category)">
            <Pencil class="h-3 w-3" />编辑
          </button>
          <button class="inline-flex items-center gap-1 rounded border border-red-200 px-2.5 py-1 text-xs text-red-600 hover:bg-red-50" @click="removeCategory(category)">
            <Trash2 class="h-3 w-3" />删除
          </button>
        </div>
      </article>
    </div>

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="modalOpen" class="fixed inset-0 z-[70] flex items-center justify-center p-4" @keydown.esc="closeModal">
          <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="closeModal" />
          <div class="modal-panel relative w-full max-w-sm rounded-2xl border border-border bg-white shadow-2xl">
            <div class="flex items-center justify-between border-b border-border px-6 py-4">
              <h2 class="text-base font-semibold text-neutral-900">{{ editingCategory ? '编辑分类' : '新增分类' }}</h2>
              <button class="rounded-lg p-1.5 text-neutral-400 transition hover:bg-neutral-100" @click="closeModal"><X class="h-4 w-4" /></button>
            </div>
            <form class="space-y-4 p-6" @submit.prevent="submitModal">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-neutral-700">分类名称 <span class="text-danger">*</span></label>
                <input v-model="modalForm.name" required placeholder="分类名称" class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15" />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-neutral-700">排序值</label>
                <input v-model.number="modalForm.sort" type="number" placeholder="排序值（越小越靠前）" class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600" />
              </div>
              <div class="flex justify-end gap-3 pt-2">
                <button type="button" class="rounded-md border border-border px-4 py-2 text-sm font-medium text-neutral-700 transition hover:bg-neutral-50" @click="closeModal">取消</button>
                <button type="submit" :disabled="saving" class="rounded-md bg-primary-600 px-5 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700 disabled:opacity-50">
                  {{ editingCategory ? '保存' : '新增' }}
                </button>
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
