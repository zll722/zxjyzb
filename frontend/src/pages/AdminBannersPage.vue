<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ImagePlus, Pencil, Plus, Trash2, X } from 'lucide-vue-next'
import { bannerApi, type SiteBanner } from '@/lib/api'

const filters = reactive({ status: '' })
const banners = ref<SiteBanner[]>([])
const loading = ref(false)
const message = ref('')
const error = ref('')

const modalOpen = ref(false)
const editingBanner = ref<SiteBanner | null>(null)
const modalForm = reactive({ title: '', subtitle: '', imageUrl: '', linkUrl: '', sort: 0, status: 'ENABLED' as 'ENABLED' | 'DISABLED' })
const modalUploading = ref(false)
const modalImageInputRef = ref<HTMLInputElement | null>(null)

const statusLabels: Record<string, string> = { ENABLED: '展示中', DISABLED: '已隐藏' }
const inputCls = 'h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100'

async function load() {
  loading.value = true; error.value = ''
  try { banners.value = await bannerApi.adminList(filters) }
  catch (err) { error.value = err instanceof Error ? err.message : '轮播加载失败' }
  finally { loading.value = false }
}

function openCreateModal() {
  editingBanner.value = null
  Object.assign(modalForm, { title: '', subtitle: '', imageUrl: '', linkUrl: '', sort: 0, status: 'ENABLED' })
  modalOpen.value = true
}

function openEditModal(item: SiteBanner) {
  editingBanner.value = item
  Object.assign(modalForm, { title: item.title, subtitle: item.subtitle || '', imageUrl: item.imageUrl, linkUrl: item.linkUrl || '', sort: item.sort, status: item.status })
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
  editingBanner.value = null
}

async function handleModalImageUpload(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  modalUploading.value = true
  try {
    modalForm.imageUrl = await bannerApi.uploadImage(file)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '图片上传失败'
  } finally {
    modalUploading.value = false
    ;(event.target as HTMLInputElement).value = ''
  }
}

async function submitModal() {
  message.value = ''; error.value = ''
  try {
    if (editingBanner.value) {
      await bannerApi.update(editingBanner.value.id, modalForm)
      message.value = '轮播已保存'
    } else {
      await bannerApi.create(modalForm)
      message.value = '轮播已创建'
    }
    closeModal()
    await load()
  } catch (err) { error.value = err instanceof Error ? err.message : '操作失败' }
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
      <div class="flex flex-wrap items-center justify-between gap-3">
        <form class="flex flex-wrap gap-3" @submit.prevent="load">
          <select v-model="filters.status" :class="inputCls + ' w-36'">
            <option value="">全部状态</option>
            <option value="ENABLED">展示中</option>
            <option value="DISABLED">已隐藏</option>
          </select>
          <button class="inline-flex h-9 items-center rounded-lg bg-neutral-100 px-4 text-sm font-medium text-neutral-700 hover:bg-neutral-200">筛选</button>
        </form>
        <button
          class="inline-flex h-9 items-center gap-2 rounded-lg bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700"
          @click="openCreateModal"
        >
          <Plus class="h-4 w-4" />新建轮播
        </button>
      </div>
      <div v-if="message" class="mt-3 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{{ message }}</div>
      <div v-if="error" class="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{{ error }}</div>
    </div>

    <!-- Banner list -->
    <div v-if="loading" class="py-6 text-center text-sm text-neutral-400">轮播加载中...</div>
    <div v-else-if="!banners.length" class="rounded-xl border border-border bg-white p-8 text-center text-sm text-neutral-400">暂无轮播</div>
    <div v-else class="grid gap-5 md:grid-cols-2 lg:grid-cols-3">
      <article v-for="item in banners" :key="item.id" class="overflow-hidden rounded-xl border border-border bg-white shadow-sm">
        <img :src="item.imageUrl" :alt="item.title" class="h-40 w-full object-cover" />
        <div class="p-4">
          <div class="mb-3 flex items-center justify-between gap-2">
            <div>
              <p class="font-semibold text-neutral-900">{{ item.title }}</p>
              <p v-if="item.subtitle" class="text-xs text-neutral-500">{{ item.subtitle }}</p>
            </div>
            <span class="shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium" :class="item.status === 'ENABLED' ? 'bg-success-bg text-success' : 'bg-neutral-100 text-neutral-500'">{{ statusLabels[item.status] || item.status }}</span>
          </div>
          <div class="flex gap-2">
            <button class="inline-flex flex-1 items-center justify-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-neutral-700 hover:bg-neutral-100" @click="openEditModal(item)">
              <Pencil class="h-3.5 w-3.5" />编辑
            </button>
            <button class="inline-flex items-center gap-1 rounded-lg border border-red-200 px-3 py-1.5 text-xs text-red-600 hover:bg-red-50" @click="remove(item.id)">
              <Trash2 class="h-3.5 w-3.5" />删除
            </button>
          </div>
        </div>
      </article>
    </div>

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="modalOpen" class="fixed inset-0 z-[70] flex items-center justify-center p-4" @keydown.esc="closeModal">
          <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="closeModal" />
          <div class="modal-panel relative w-full max-w-lg rounded-2xl border border-border bg-white shadow-2xl">
            <div class="flex items-center justify-between border-b border-border px-6 py-4">
              <h2 class="text-base font-semibold text-neutral-900">{{ editingBanner ? '编辑轮播' : '新建轮播' }}</h2>
              <button class="rounded-lg p-1.5 text-neutral-400 transition hover:bg-neutral-100" @click="closeModal"><X class="h-4 w-4" /></button>
            </div>
            <form class="space-y-4 p-6" @submit.prevent="submitModal">
              <!-- Image upload -->
              <div>
                <p class="mb-2 text-sm font-medium text-neutral-700">轮播图片 <span class="text-danger">*</span></p>
                <div
                  class="relative flex h-36 cursor-pointer items-center justify-center overflow-hidden rounded-xl border-2 border-dashed border-border bg-neutral-50 transition hover:border-primary-400 hover:bg-primary-50"
                  @click="modalImageInputRef?.click()"
                >
                  <img v-if="modalForm.imageUrl" :src="modalForm.imageUrl" alt="图片预览" class="h-full w-full object-cover" />
                  <div v-else class="flex flex-col items-center gap-2 text-neutral-400">
                    <ImagePlus class="h-8 w-8" />
                    <span class="text-xs">点击上传轮播图片</span>
                  </div>
                  <div v-if="modalUploading" class="absolute inset-0 flex items-center justify-center bg-white/70">
                    <span class="text-xs text-neutral-500">上传中...</span>
                  </div>
                  <button
                    v-if="modalForm.imageUrl && !modalUploading"
                    type="button"
                    class="absolute right-2 top-2 rounded-full bg-black/50 p-1 text-white hover:bg-black/70"
                    @click.stop="modalForm.imageUrl = ''"
                  >
                    <X class="h-3 w-3" />
                  </button>
                </div>
                <input ref="modalImageInputRef" type="file" accept="image/jpeg,image/png,image/webp,image/gif" class="hidden" @change="handleModalImageUpload" />
              </div>
              <div class="grid gap-3 sm:grid-cols-2">
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">标题 <span class="text-danger">*</span></label>
                  <input v-model="modalForm.title" required :class="inputCls + ' w-full'" placeholder="轮播标题" />
                </div>
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">副标题</label>
                  <input v-model="modalForm.subtitle" :class="inputCls + ' w-full'" placeholder="副标题（可选）" />
                </div>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-neutral-700">跳转链接</label>
                <input v-model="modalForm.linkUrl" :class="inputCls + ' w-full'" placeholder="点击轮播跳转的链接（可选）" />
              </div>
              <div class="grid gap-3 sm:grid-cols-2">
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">排序</label>
                  <input v-model.number="modalForm.sort" min="0" type="number" :class="inputCls + ' w-full'" />
                </div>
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-neutral-700">状态</label>
                  <select v-model="modalForm.status" :class="inputCls + ' w-full'">
                    <option value="ENABLED">展示中</option>
                    <option value="DISABLED">隐藏</option>
                  </select>
                </div>
              </div>
              <div class="flex justify-end gap-3 pt-2">
                <button type="button" class="rounded-md border border-border px-4 py-2 text-sm font-medium text-neutral-700 transition hover:bg-neutral-50" @click="closeModal">取消</button>
                <button type="submit" :disabled="modalUploading" class="rounded-md bg-primary-600 px-5 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700 disabled:opacity-50">{{ editingBanner ? '保存' : '创建' }}</button>
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
