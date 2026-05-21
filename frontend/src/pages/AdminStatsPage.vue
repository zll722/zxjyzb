<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { statsApi, type PlatformStats } from '@/lib/api'

const stats = ref<PlatformStats | null>(null)
const loading = ref(false)
const error = ref('')

const cards = [
  { key: 'userCount', label: '用户总数', suffix: '人', color: 'brand' },
  { key: 'teacherCount', label: '教师数', suffix: '人', color: 'indigo' },
  { key: 'studentCount', label: '学生数', suffix: '人', color: 'violet' },
  { key: 'courseCount', label: '课程数', suffix: '门', color: 'emerald' },
  { key: 'liveCount', label: '直播数', suffix: '场', color: 'rose' },
  { key: 'replayCount', label: '回放数', suffix: '个', color: 'amber' },
  { key: 'homeworkCount', label: '作业数', suffix: '份', color: 'teal' },
  { key: 'examCount', label: '考试数', suffix: '场', color: 'cyan' },
] as const

async function loadStats() {
  loading.value = true; error.value = ''
  try { stats.value = await statsApi.platform() }
  catch (err) { error.value = err instanceof Error ? err.message : '统计加载失败' }
  finally { loading.value = false }
}

onMounted(loadStats)
</script>

<template>
  <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">管理员统计</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">平台总览</h1>
        <p class="mt-1 text-sm text-neutral-500">汇总平台用户、课程、直播、回放、作业和考试规模，访问权限由后端管理员角色校验。</p>
      </div>

      <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>
      <p v-if="loading" class="text-sm text-neutral-400">统计加载中...</p>

      <div v-else-if="stats" class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div v-for="card in cards" :key="card.key" class="rounded-xl border border-border bg-white p-5 shadow-sm">
          <p class="text-xs text-neutral-500">{{ card.label }}</p>
          <p class="mt-3 text-3xl font-bold text-primary-600">
            {{ stats[card.key] ?? 0 }}<span class="ml-1 text-base font-normal text-neutral-400">{{ card.suffix }}</span>
          </p>
        </div>
      </div>
  </div>
</template>
