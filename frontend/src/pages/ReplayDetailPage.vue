<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Clapperboard } from 'lucide-vue-next'
import { replayApi, type LiveReplay } from '@/lib/api'

const route = useRoute()
const replay = ref<LiveReplay | null>(null)

async function load() {
  replay.value = await replayApi.detail(Number(route.params.id))
}

onMounted(load)
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div v-if="replay" class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/replays" class="text-sm text-cyan-200">返回回放中心</RouterLink>
        <RouterLink to="/dashboard" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">工作台</RouterLink>
      </nav>
      <section class="grid gap-6 lg:grid-cols-[1fr_360px]">
        <div class="rounded-[2rem] border border-white/10 bg-black p-4 shadow-2xl">
          <video v-if="replay.playUrl" class="aspect-video w-full rounded-[1.5rem] bg-black" :src="replay.playUrl" controls preload="metadata" />
          <div v-else class="grid aspect-video place-items-center rounded-[1.5rem] bg-gradient-to-br from-slate-900 to-cyan-950 text-center">
            <div>
              <Clapperboard class="mx-auto h-16 w-16 text-cyan-200" />
              <p class="mt-5 text-slate-300">教师尚未上传回放视频</p>
            </div>
          </div>
        </div>
        <aside class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <p class="text-sm text-cyan-200">{{ replay.status }} · {{ replay.roomTitle || '独立回放' }}</p>
          <h1 class="mt-3 text-3xl font-black">{{ replay.title }}</h1>
          <p class="mt-4 text-sm leading-7 text-slate-300">{{ replay.intro || '暂无简介' }}</p>
          <div class="mt-8 space-y-3 rounded-[1.5rem] bg-slate-900 p-5 text-sm text-slate-300">
            <p>讲师：{{ replay.teacherName || '未知' }}</p>
            <p>文件大小：{{ replay.fileSize ? `${(replay.fileSize / 1024 / 1024).toFixed(2)} MB` : '暂无文件' }}</p>
            <p>发布时间：{{ replay.createdAt || '-' }}</p>
          </div>
        </aside>
      </section>
    </div>
  </main>
</template>
