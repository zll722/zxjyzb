<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { statsApi, type PlatformStats } from '@/lib/api'

const stats = ref<PlatformStats | null>(null)
const loading = ref(false)
const error = ref('')

const cards = [
  { key: 'userCount', label: '用户总数', suffix: '人' },
  { key: 'teacherCount', label: '教师数', suffix: '人' },
  { key: 'studentCount', label: '学生数', suffix: '人' },
  { key: 'courseCount', label: '课程数', suffix: '门' },
  { key: 'liveCount', label: '直播数', suffix: '场' },
  { key: 'replayCount', label: '回放数', suffix: '个' },
  { key: 'homeworkCount', label: '作业数', suffix: '份' },
  { key: 'examCount', label: '考试数', suffix: '场' },
] as const

async function loadStats() {
  loading.value = true
  error.value = ''
  try {
    stats.value = await statsApi.platform()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '统计加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/dashboard" class="text-sm text-cyan-200">返回工作台</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">管理员统计</p>
        <h1 class="mt-2 text-4xl font-black">平台总览</h1>
        <p class="mt-3 max-w-3xl text-slate-300">汇总平台用户、课程、直播、回放、作业和考试规模，访问权限由后端管理员角色校验。</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <p v-if="loading" class="mt-8 text-slate-300">统计加载中...</p>

      <section v-else-if="stats" class="mt-8 grid gap-5 md:grid-cols-2 lg:grid-cols-4">
        <div v-for="card in cards" :key="card.key" class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <p class="text-sm text-slate-400">{{ card.label }}</p>
          <p class="mt-4 text-4xl font-black text-cyan-200">{{ stats[card.key] ?? 0 }}<span class="ml-1 text-base text-slate-400">{{ card.suffix }}</span></p>
        </div>
      </section>
    </div>
  </main>
</template>
