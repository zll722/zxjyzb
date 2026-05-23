<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Clapperboard, Pencil, Plus, Trash2, UploadCloud, X } from 'lucide-vue-next'
import { replayApi, type LiveReplay } from '@/lib/api'
import Empty from '@/components/Empty.vue'

const replays = ref<LiveReplay[]>([])
const message = ref('')

const modalOpen = ref(false)
const editingReplay = ref<LiveReplay | null>(null)
const modalForm = reactive({ title: '', intro: '', status: 'DRAFT' as 'DRAFT' | 'PUBLISHED' })

async function load() {
  const page = await replayApi.mine({ current: 1, size: 50 })
  replays.value = page.records
}

function openCreateModal() {
  editingReplay.value = null
  modalForm.title = ''
  modalForm.intro = ''
  modalForm.status = 'DRAFT'
  modalOpen.value = true
}

function openEditModal(replay: LiveReplay) {
  editingReplay.value = replay
  modalForm.title = replay.title
  modalForm.intro = replay.intro || ''
  modalForm.status = replay.status
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
  editingReplay.value = null
}

async function submitModal() {
  try {
    if (editingReplay.value) {
      await replayApi.update(editingReplay.value.id, { roomId: editingReplay.value.roomId, title: modalForm.title, intro: modalForm.intro, status: modalForm.status })
      message.value = '回放已保存'
    } else {
      await replayApi.create({ title: modalForm.title, intro: modalForm.intro, status: modalForm.status })
      message.value = '回放已创建'
    }
    closeModal()
    await load()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '操作失败'
  }
}

async function removeReplay(id: number) {
  await replayApi.remove(id); message.value = '回放已删除'; await load()
}

async function uploadVideo(replay: LiveReplay, event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  await replayApi.uploadVideo(replay.id, file)
  input.value = ''; message.value = '视频已上传'; await load()
}

onMounted(load)
</script>

<template>
  <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <p class="text-sm font-medium text-primary-600">教师回放管理</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">上传本地视频并发布回放</h1>
      </div>
      <button
        class="inline-flex items-center gap-2 rounded-xl bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700"
        @click="openCreateModal"
      >
        <Plus class="h-4 w-4" />新增回放
      </button>
    </div>

    <div v-if="message" class="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</div>

    <div class="space-y-4">
      <article v-for="replay in replays" :key="replay.id" class="rounded-xl border border-border bg-white p-5 shadow-sm">
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div>
            <span class="inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium" :class="replay.status === 'PUBLISHED' ? 'bg-success-bg text-success' : 'bg-neutral-100 text-neutral-600'">
              {{ replay.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </span>
            <h2 class="mt-1.5 font-semibold text-neutral-900">{{ replay.title }}</h2>
            <p class="text-xs text-neutral-400">{{ replay.videoPath ? `已上传 ${(replay.fileSize / 1024 / 1024).toFixed(2)} MB` : '未上传视频' }}</p>
            <p v-if="replay.intro" class="mt-1 text-sm text-neutral-500">{{ replay.intro }}</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <button
              class="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-neutral-700 hover:bg-neutral-100"
              @click="openEditModal(replay)"
            >
              <Pencil class="h-3.5 w-3.5" />编辑
            </button>
            <RouterLink :to="`/replays/${replay.id}`" class="rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-neutral-700 hover:bg-neutral-100">预览</RouterLink>
            <label class="inline-flex cursor-pointer items-center gap-1.5 rounded-lg bg-primary-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-primary-700">
              <UploadCloud class="h-3.5 w-3.5" />上传视频
              <input class="hidden" type="file" accept="video/mp4,video/webm,video/ogg,video/quicktime,video/x-msvideo" @change="uploadVideo(replay, $event)" />
            </label>
            <button class="inline-flex items-center gap-1.5 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50" @click="removeReplay(replay.id)">
              <Trash2 class="h-3.5 w-3.5" />删除
            </button>
          </div>
        </div>
      </article>
    </div>

    <Empty v-if="replays.length === 0" title="暂无回放记录" :icon="Clapperboard" />

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="modalOpen" class="fixed inset-0 z-[70] flex items-center justify-center p-4" @keydown.esc="closeModal">
          <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="closeModal" />
          <div class="modal-panel relative w-full max-w-md rounded-2xl border border-border bg-white shadow-2xl">
            <div class="flex items-center justify-between border-b border-border px-6 py-4">
              <h2 class="text-base font-semibold text-neutral-900">{{ editingReplay ? '编辑回放' : '新增回放' }}</h2>
              <button class="rounded-lg p-1.5 text-neutral-400 transition hover:bg-neutral-100" @click="closeModal"><X class="h-4 w-4" /></button>
            </div>
            <form class="space-y-4 p-6" @submit.prevent="submitModal">
              <div>
                <label class="mb-1.5 block text-sm font-medium text-neutral-700">回放标题 <span class="text-danger">*</span></label>
                <input v-model="modalForm.title" required placeholder="请输入回放标题" class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15" />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-neutral-700">回放简介</label>
                <input v-model="modalForm.intro" placeholder="回放简介（可选）" class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15" />
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-medium text-neutral-700">状态</label>
                <select v-model="modalForm.status" class="h-10 w-full rounded-md border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-600">
                  <option value="DRAFT">草稿</option>
                  <option value="PUBLISHED">发布</option>
                </select>
              </div>
              <div class="flex justify-end gap-3 pt-2">
                <button type="button" class="rounded-md border border-border px-4 py-2 text-sm font-medium text-neutral-700 transition hover:bg-neutral-50" @click="closeModal">取消</button>
                <button type="submit" class="rounded-md bg-primary-600 px-5 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700">{{ editingReplay ? '保存' : '创建' }}</button>
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
