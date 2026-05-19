<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Clapperboard, PlayCircle } from 'lucide-vue-next'
import { replayApi, type LiveReplay } from '@/lib/api'

const replays = ref<LiveReplay[]>([])

async function load() {
  const page = await replayApi.page({ current: 1, size: 30 })
  replays.value = page.records
}

onMounted(load)
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/" class="text-xl font-black">EduLive</RouterLink>
        <RouterLink to="/dashboard" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">工作台</RouterLink>
      </nav>
      <section class="relative overflow-hidden rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <div class="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(34,211,238,0.22),transparent_32%),radial-gradient(circle_at_80%_0%,rgba(14,165,233,0.16),transparent_30%)]" />
        <div class="relative">
          <p class="text-sm text-cyan-200">回放中心</p>
          <h1 class="mt-2 text-4xl font-black">错过直播，也能随时补课</h1>
          <p class="mt-4 max-w-2xl text-sm leading-7 text-slate-300">教师发布后的回放会展示在这里，学生可直接使用浏览器原生播放器学习。</p>
        </div>
      </section>
      <section class="mt-8 grid gap-5 md:grid-cols-2 lg:grid-cols-3">
        <RouterLink v-for="replay in replays" :key="replay.id" :to="`/replays/${replay.id}`" class="group rounded-[2rem] border border-white/10 bg-white/8 p-6 transition hover:-translate-y-1 hover:bg-white/12">
          <div class="flex items-center justify-between">
            <span class="rounded-full bg-cyan-300/10 px-3 py-1 text-sm text-cyan-100">{{ replay.status }}</span>
            <span class="text-sm text-slate-300">{{ replay.videoPath ? '可播放' : '待上传' }}</span>
          </div>
          <div class="mt-8 grid h-36 place-items-center rounded-[1.5rem] bg-gradient-to-br from-slate-900 to-cyan-950">
            <PlayCircle class="h-14 w-14 text-cyan-200 transition group-hover:scale-110" />
          </div>
          <h2 class="mt-5 text-2xl font-black">{{ replay.title }}</h2>
          <p class="mt-3 line-clamp-2 text-sm leading-6 text-slate-300">{{ replay.intro || '暂无简介' }}</p>
          <p class="mt-5 text-sm text-cyan-200">讲师：{{ replay.teacherName || '未知' }}</p>
        </RouterLink>
      </section>
      <div v-if="replays.length === 0" class="mt-12 rounded-[2rem] border border-white/10 p-10 text-center text-slate-300">
        <Clapperboard class="mx-auto mb-4 h-10 w-10 text-cyan-200" />
        暂无已发布回放
      </div>
    </div>
  </main>
</template>
