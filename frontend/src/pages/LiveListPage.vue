<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Radio, Users } from 'lucide-vue-next'
import { formatLiveStatus, liveApi, type LiveRoom } from '@/lib/api'
import AppLayout from '@/components/layout/AppLayout.vue'
import Empty from '@/components/Empty.vue'

const rooms   = ref<LiveRoom[]>([])
const status  = ref('LIVE')
const loading = ref(false)
const error   = ref('')

async function load() {
  loading.value = true
  error.value   = ''
  try {
    const page = await liveApi.page({ current: 1, size: 30, status: status.value })
    rooms.value = page.records
  } catch (e) {
    error.value = e instanceof Error ? e.message : '直播间加载失败'
  } finally {
    loading.value = false
  }
}

function setStatus(val: string) { status.value = val; load() }

onMounted(load)
</script>

<template>
  <AppLayout>
    <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">

      <!-- Page header -->
      <div class="mb-6">
        <p class="text-sm font-medium text-neutral-400">直播中心</p>
        <h1 class="mt-1 text-2xl font-semibold text-neutral-900">观看实时课堂</h1>
      </div>

      <!-- Filter tabs -->
      <div class="mb-8 flex flex-wrap gap-2">
        <button
          v-for="[val, label] in [['', '全部'], ['LIVE', '直播中'], ['SCHEDULED', '待开播'], ['ENDED', '已结束']]"
          :key="val"
          class="inline-flex h-9 items-center gap-1.5 rounded-full px-4 text-sm font-medium transition-all duration-200"
          :class="status === val
            ? 'bg-primary-600 text-white shadow-sm'
            : 'border border-border text-neutral-600 hover:bg-neutral-100'"
          @click="setStatus(val)"
        >
          <span v-if="val === 'LIVE'" class="live-dot" />
          {{ label }}
        </button>
      </div>

      <!-- Skeleton -->
      <div v-if="loading" class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div v-for="idx in 6" :key="idx" class="overflow-hidden rounded-xl border border-border bg-white shadow-sm">
          <div class="aspect-video animate-pulse bg-neutral-100" />
          <div class="space-y-2 p-4">
            <div class="h-4 animate-pulse rounded bg-neutral-100" />
            <div class="h-3 w-2/3 animate-pulse rounded bg-neutral-100" />
          </div>
        </div>
      </div>

      <!-- Error -->
      <div
        v-else-if="error"
        class="rounded-xl border border-danger/20 bg-danger-bg p-6 text-center text-sm text-danger"
      >{{ error }}</div>

      <!-- Room grid -->
      <div v-else-if="rooms.length > 0" class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <RouterLink
          v-for="room in rooms"
          :key="room.id"
          :to="`/live/${room.id}`"
          class="group overflow-hidden rounded-xl border border-border bg-white shadow-sm transition-all duration-200 hover:shadow-md hover:-translate-y-px"
        >
          <!-- Cover with status badge -->
          <div class="relative aspect-video overflow-hidden bg-neutral-100">
            <img
              v-if="room.cover"
              :src="room.cover"
              :alt="room.title"
              class="h-full w-full object-cover transition-transform duration-300 group-hover:scale-[1.03]"
              loading="lazy"
            />
            <div
              v-else
              class="flex h-full w-full items-center justify-center bg-gradient-to-br from-neutral-100 to-neutral-200"
            >
              <Radio class="h-8 w-8 text-neutral-400" stroke-width="1.5" />
            </div>

            <!-- LIVE badge -->
            <div
              v-if="room.status === 'LIVE'"
              class="absolute left-2 top-2 flex items-center gap-1.5 rounded-md bg-danger px-2 py-0.5 text-[11px] font-bold text-white"
            >
              <span class="live-dot" style="background:white;" />LIVE
            </div>
            <div
              v-else-if="room.status === 'SCHEDULED'"
              class="absolute left-2 top-2 rounded-md bg-warning px-2 py-0.5 text-[11px] font-bold text-white"
            >即将开始</div>
            <div
              v-else
              class="absolute left-2 top-2 rounded-md bg-neutral-500/80 px-2 py-0.5 text-[11px] font-medium text-white"
            >已结束</div>

            <!-- Online count -->
            <div class="absolute bottom-2 right-2 flex items-center gap-1 rounded-md bg-black/50 px-2 py-0.5 text-[11px] text-white/90">
              <Users class="h-3 w-3" />{{ room.onlineCount }}
            </div>
          </div>

          <div class="p-4">
            <h2 class="font-semibold text-neutral-900 transition-colors group-hover:text-primary-600">
              {{ room.title }}
            </h2>
            <p class="mt-1.5 line-clamp-2 text-sm leading-6 text-neutral-500">{{ room.intro || '暂无简介' }}</p>
            <p class="mt-3 text-xs text-neutral-400">讲师：{{ room.teacherName }}</p>
          </div>
        </RouterLink>
      </div>

      <Empty v-else :icon="Radio" title="暂无直播" description="当前没有符合条件的直播间" />

    </div>
  </AppLayout>
</template>
