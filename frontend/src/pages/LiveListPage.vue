<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Radio } from 'lucide-vue-next'
import { formatLiveStatus, liveApi, type LiveRoom } from '@/lib/api'

const rooms = ref<LiveRoom[]>([])
const status = ref('')

async function load() {
  const page = await liveApi.page({ current: 1, size: 30, status: status.value })
  rooms.value = page.records
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
      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">直播中心</p>
        <h1 class="mt-2 text-4xl font-black">观看实时课堂</h1>
        <div class="mt-8 flex flex-wrap gap-3">
          <button
            v-for="[val, label] in [['', '全部'], ['LIVE', '直播中'], ['SCHEDULED', '待开播'], ['ENDED', '已结束']]"
            :key="val"
            class="rounded-full border px-5 py-2 text-sm transition"
            :class="status === val ? 'border-cyan-300 bg-cyan-300/10 text-cyan-200' : 'border-white/10 hover:bg-white/10 text-slate-300'"
            @click="status = val; load()"
          >
            {{ label }}
          </button>
        </div>
      </section>
      <section class="mt-8 grid gap-5 md:grid-cols-2 lg:grid-cols-3">
        <RouterLink v-for="room in rooms" :key="room.id" :to="`/live/${room.id}`" class="rounded-[2rem] border border-white/10 bg-white/8 p-6 transition hover:-translate-y-1 hover:bg-white/12">
          <div class="flex items-center justify-between">
            <span class="rounded-full px-3 py-1 text-xs font-bold" :class="{
              'bg-green-400/20 text-green-300': room.status === 'LIVE',
              'bg-amber-400/20 text-amber-300': room.status === 'SCHEDULED',
              'bg-slate-600/60 text-slate-400': room.status === 'ENDED',
            }">
              <span v-if="room.status === 'LIVE'" class="mr-1 inline-block h-1.5 w-1.5 animate-pulse rounded-full bg-green-400" />
              {{ formatLiveStatus(room.status) }}
            </span>
            <span class="text-sm text-slate-300">{{ room.onlineCount }} 在线</span>
          </div>
          <Radio class="mt-8 h-10 w-10 text-cyan-200" />
          <h2 class="mt-5 text-2xl font-black">{{ room.title }}</h2>
          <p class="mt-3 line-clamp-2 text-sm leading-6 text-slate-300">{{ room.intro || '暂无简介' }}</p>
          <p class="mt-5 text-sm text-cyan-200">讲师：{{ room.teacherName }}</p>
        </RouterLink>
      </section>
      <div v-if="rooms.length === 0" class="mt-12 rounded-[2rem] border border-white/10 p-10 text-center text-slate-300">暂无直播间</div>
    </div>
  </main>
</template>
