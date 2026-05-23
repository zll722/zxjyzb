<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Clapperboard, ChevronLeft } from 'lucide-vue-next'
import { replayApi, getToken, formatReplayStatus, formatDateTime, type LiveReplay } from '@/lib/api'
import AppLayout from '@/components/layout/AppLayout.vue'

const route = useRoute()
const replay = ref<LiveReplay | null>(null)

const videoSrc = computed(() => {
  if (!replay.value?.playUrl) return null
  const token = getToken()
  return token ? `${replay.value.playUrl}?token=${encodeURIComponent(token)}` : replay.value.playUrl
})

async function load() {
  replay.value = await replayApi.detail(Number(route.params.id))
}

onMounted(load)
</script>

<template>
  <AppLayout>
    <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <RouterLink to="/replays" class="mb-6 inline-flex items-center gap-1 text-sm text-neutral-500 hover:text-primary-600">
        <ChevronLeft class="h-4 w-4" />返回回放中心
      </RouterLink>

      <div v-if="replay" class="grid gap-6 lg:grid-cols-[1fr_360px]">
        <!-- Video player -->
        <div class="overflow-hidden rounded-xl border border-border bg-black shadow-sm">
          <video v-if="videoSrc" class="aspect-video w-full bg-black" :src="videoSrc" controls preload="metadata" />
          <div v-else class="grid aspect-video place-items-center bg-neutral-950 text-center">
            <div>
              <Clapperboard class="mx-auto h-14 w-14 text-neutral-600" />
              <p class="mt-4 text-sm text-neutral-400">教师尚未上传回放视频</p>
            </div>
          </div>
        </div>

        <!-- Meta info -->
        <aside class="rounded-xl border border-border bg-white p-5 shadow-sm">
          <div class="flex items-center gap-2">
            <span class="rounded-full px-2.5 py-0.5 text-xs font-medium"
              :class="{
                'bg-success-bg text-success': replay.status === 'PUBLISHED',
                'bg-neutral-100 text-neutral-500': replay.status !== 'PUBLISHED',
              }">
              {{ formatReplayStatus(replay.status) }}
            </span>
            <span v-if="replay.roomTitle" class="text-xs text-neutral-400">{{ replay.roomTitle }}</span>
          </div>
          <h1 class="mt-3 text-xl font-bold text-neutral-900">{{ replay.title }}</h1>
          <p class="mt-3 text-sm leading-7 text-neutral-500">{{ replay.intro || '暂无简介' }}</p>
          <dl class="mt-6 space-y-3 rounded-lg bg-neutral-100 p-4 text-sm">
            <div class="flex justify-between">
              <dt class="text-neutral-500">讲师</dt>
              <dd class="font-medium text-neutral-800">{{ replay.teacherName || '未知' }}</dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-neutral-500">文件大小</dt>
              <dd class="font-medium text-neutral-800">{{ replay.fileSize ? `${(replay.fileSize / 1024 / 1024).toFixed(2)} MB` : '暂无文件' }}</dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-neutral-500">发布时间</dt>
              <dd class="font-medium text-neutral-800">{{ formatDateTime(replay.createdAt) }}</dd>
            </div>
          </dl>
        </aside>
      </div>

      <div v-else class="py-20 text-center text-sm text-neutral-400">加载中...</div>
    </div>
  </AppLayout>
</template>
