<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Clapperboard, PlayCircle } from 'lucide-vue-next'
import { replayApi, type LiveReplay } from '@/lib/api'
import AppLayout from '@/components/layout/AppLayout.vue'
import Empty from '@/components/Empty.vue'

const replays = ref<LiveReplay[]>([])

async function load() {
  const page = await replayApi.page({ current: 1, size: 30 })
  replays.value = page.records
}

onMounted(load)
</script>

<template>
  <AppLayout>
    <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">回放中心</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">错过直播，也能随时补课</h1>
        <p class="mt-1 text-sm text-neutral-500">教师发布后的回放会展示在这里，可直接使用浏览器原生播放器学习。</p>
      </div>

      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <RouterLink
          v-for="replay in replays"
          :key="replay.id"
          :to="`/replays/${replay.id}`"
          class="group rounded-xl border border-border bg-white shadow-sm transition hover:shadow-md"
        >
          <div class="grid h-40 place-items-center overflow-hidden rounded-t-xl bg-gradient-to-br from-neutral-100 to-primary-100">
            <PlayCircle class="h-12 w-12 text-primary-400 transition group-hover:scale-110 group-hover:text-primary-600" />
          </div>
          <div class="p-4">
            <div class="flex items-center justify-between">
              <span class="rounded-full bg-neutral-100 px-2.5 py-0.5 text-xs text-neutral-500">{{ replay.status }}</span>
              <span class="text-xs" :class="replay.videoPath ? 'text-emerald-600' : 'text-neutral-400'">
                {{ replay.videoPath ? '可播放' : '待上传' }}
              </span>
            </div>
            <h2 class="mt-2 font-semibold text-neutral-900 group-hover:text-primary-600">{{ replay.title }}</h2>
            <p class="mt-1 line-clamp-2 text-sm text-neutral-500">{{ replay.intro || '暂无简介' }}</p>
            <p class="mt-2 text-xs text-neutral-400">讲师：{{ replay.teacherName || '未知' }}</p>
          </div>
        </RouterLink>
      </div>

      <Empty v-if="replays.length === 0" title="暂无已发布回放" :icon="Clapperboard" />
    </div>
  </AppLayout>
</template>
