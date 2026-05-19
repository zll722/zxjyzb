<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Clapperboard, Plus, Save, Trash2, UploadCloud } from 'lucide-vue-next'
import { replayApi, type LiveReplay } from '@/lib/api'

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
  form.title = ''
  form.intro = ''
  form.status = 'DRAFT'
  message.value = '回放已创建'
  await load()
}

async function saveReplay(replay: LiveReplay) {
  const item = editForms[replay.id]
  await replayApi.update(replay.id, { roomId: replay.roomId, title: item.title, intro: item.intro, status: item.status })
  message.value = '回放已保存'
  await load()
}

async function removeReplay(id: number) {
  await replayApi.remove(id)
  message.value = '回放已删除'
  await load()
}

async function uploadVideo(replay: LiveReplay, event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  await replayApi.uploadVideo(replay.id, file)
  input.value = ''
  message.value = '视频已上传'
  await load()
}

onMounted(load)
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/dashboard" class="text-sm text-cyan-200">返回工作台</RouterLink>
        <RouterLink to="/replays" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">回放中心</RouterLink>
      </nav>
      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">教师回放管理</p>
        <h1 class="mt-2 text-4xl font-black">上传本地视频并发布回放</h1>
        <p v-if="message" class="mt-4 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <form class="mt-8 grid gap-4 lg:grid-cols-[1fr_1fr_150px_120px]" @submit.prevent="createReplay">
          <input v-model="form.title" required class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="回放标题" />
          <input v-model="form.intro" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="回放简介" />
          <select v-model="form.status" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
            <option value="DRAFT">草稿</option>
            <option value="PUBLISHED">发布</option>
          </select>
          <button class="inline-flex items-center justify-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">
            <Plus class="h-4 w-4" />创建
          </button>
        </form>
      </section>
      <section class="mt-8 space-y-5">
        <article v-for="replay in replays" :key="replay.id" class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p class="text-sm text-cyan-200">{{ replay.status }} · {{ replay.roomTitle || '独立回放' }}</p>
              <h2 class="mt-2 text-2xl font-black">{{ replay.title }}</h2>
              <p class="mt-2 text-sm text-slate-300">{{ replay.videoPath ? `已上传 ${(replay.fileSize / 1024 / 1024).toFixed(2)} MB` : '未上传视频' }}</p>
            </div>
            <div class="flex flex-wrap gap-3">
              <RouterLink :to="`/replays/${replay.id}`" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">预览</RouterLink>
              <label class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-cyan-300 px-5 py-2 text-sm font-bold text-slate-950">
                <UploadCloud class="h-4 w-4" />上传
                <input class="hidden" type="file" accept="video/mp4,video/webm,video/ogg,video/quicktime,video/x-msvideo" @change="uploadVideo(replay, $event)" />
              </label>
              <button class="inline-flex items-center gap-2 rounded-full border border-red-300/30 px-5 py-2 text-sm text-red-100" @click="removeReplay(replay.id)"><Trash2 class="h-4 w-4" />删除</button>
            </div>
          </div>
          <div class="mt-6 grid gap-3 lg:grid-cols-[1fr_1fr_150px_120px]">
            <input v-model="editForms[replay.id].title" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="标题" />
            <input v-model="editForms[replay.id].intro" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="简介" />
            <select v-model="editForms[replay.id].status" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
              <option value="DRAFT">草稿</option>
              <option value="PUBLISHED">发布</option>
            </select>
            <button class="inline-flex items-center justify-center gap-2 rounded-2xl bg-white px-4 py-3 font-bold text-slate-950" @click="saveReplay(replay)"><Save class="h-4 w-4" />保存</button>
          </div>
        </article>
      </section>
      <div v-if="replays.length === 0" class="mt-12 rounded-[2rem] border border-white/10 p-10 text-center text-slate-300">
        <Clapperboard class="mx-auto mb-4 h-10 w-10 text-cyan-200" />
        暂无回放记录
      </div>
    </div>
  </main>
</template>
