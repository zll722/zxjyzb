<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Clapperboard, Plus, Save, Trash2, UploadCloud } from 'lucide-vue-next'
import { replayApi, type LiveReplay } from '@/lib/api'
import Empty from '@/components/Empty.vue'

const replays = ref<LiveReplay[]>([])
const form = reactive({ title: '', intro: '', status: 'DRAFT' })
const editForms = reactive<Record<number, { title: string; intro: string; status: 'DRAFT' | 'PUBLISHED' }>>({})
const message = ref('')

async function load() {
  const page = await replayApi.mine({ current: 1, size: 50 })
  replays.value = page.records
  replays.value.forEach((replay) => {
    editForms[replay.id] = { title: replay.title, intro: replay.intro || '', status: replay.status }
  })
}

async function createReplay() {
  await replayApi.create({ title: form.title, intro: form.intro, status: form.status })
  form.title = ''; form.intro = ''; form.status = 'DRAFT'
  message.value = '回放已创建'; await load()
}

async function saveReplay(replay: LiveReplay) {
  const item = editForms[replay.id]
  await replayApi.update(replay.id, { roomId: replay.roomId, title: item.title, intro: item.intro, status: item.status })
  message.value = '回放已保存'; await load()
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
      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">教师回放管理</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">上传本地视频并发布回放</h1>
      </div>

      <!-- Create form -->
      <div class="mb-8 rounded-xl border border-border bg-white p-5 shadow-sm">
        <h2 class="mb-4 font-semibold text-neutral-900">新增回放</h2>
        <div v-if="message" class="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</div>
        <form class="grid gap-3 lg:grid-cols-[1fr_1fr_120px_auto]" @submit.prevent="createReplay">
          <input v-model="form.title" required placeholder="回放标题" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
          <input v-model="form.intro" placeholder="回放简介（可选）" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
          <select v-model="form.status" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400">
            <option value="DRAFT">草稿</option>
            <option value="PUBLISHED">发布</option>
          </select>
          <button type="submit" class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700">
            <Plus class="h-4 w-4" />创建
          </button>
        </form>
      </div>

      <!-- Replay list -->
      <div class="space-y-4">
        <article v-for="replay in replays" :key="replay.id" class="rounded-xl border border-border bg-white p-5 shadow-sm">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <span class="inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium" :class="replay.status === 'PUBLISHED' ? 'bg-success-bg text-success' : 'bg-neutral-100 text-neutral-600'">
                {{ replay.status === 'PUBLISHED' ? '已发布' : '草稿' }}
              </span>
              <h2 class="mt-1.5 font-semibold text-neutral-900">{{ replay.title }}</h2>
              <p class="text-xs text-neutral-400">{{ replay.videoPath ? `已上传 ${(replay.fileSize / 1024 / 1024).toFixed(2)} MB` : '未上传视频' }}</p>
            </div>
            <div class="flex flex-wrap gap-2">
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
          <div class="mt-4 grid gap-2 border-t border-border pt-4 lg:grid-cols-[1fr_1fr_120px_auto]">
            <input v-model="editForms[replay.id].title" placeholder="标题" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
            <input v-model="editForms[replay.id].intro" placeholder="简介" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100" />
            <select v-model="editForms[replay.id].status" class="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400">
              <option value="DRAFT">草稿</option>
              <option value="PUBLISHED">发布</option>
            </select>
            <button class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-border px-3 text-sm font-medium text-neutral-700 hover:bg-neutral-100" @click="saveReplay(replay)">
              <Save class="h-4 w-4" />保存
            </button>
          </div>
        </article>
      </div>

      <Empty v-if="replays.length === 0" title="暂无回放记录" :icon="Clapperboard" />
  </div>
</template>
