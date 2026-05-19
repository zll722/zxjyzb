<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import Empty from '@/components/Empty.vue'
import { courseApi, type LearningRecord, type PageResult } from '@/lib/api'

const page = ref<PageResult<LearningRecord>>({ records: [], total: 0, size: 10, current: 1 })
const loading = ref(false)
const error = ref('')

function formatTime(value?: string) {
  if (!value) return '暂无时间'
  return value.replace('T', ' ').slice(0, 16)
}

async function load(current = page.value.current) {
  loading.value = true
  error.value = ''
  try {
    page.value = await courseApi.learningRecords({ current, size: page.value.size })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '学习记录加载失败'
  } finally {
    loading.value = false
  }
}

function canPrev() {
  return page.value.current > 1
}

function canNext() {
  return page.value.current * page.value.size < page.value.total
}

onMounted(() => load(1))
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/dashboard" class="text-sm text-cyan-200">返回工作台</RouterLink>
        <RouterLink to="/courses" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">课程中心</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">学生学习中心</p>
        <h1 class="mt-2 text-4xl font-black">我的学习</h1>
        <p class="mt-3 max-w-3xl text-slate-300">查看课程学习记录、最后观看章节、最后观看时间与学习进度。</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <p v-if="loading" class="mt-8 text-slate-300">学习记录加载中...</p>

      <section v-else class="mt-8 space-y-4">
        <article v-for="record in page.records" :key="`${record.courseId}-${record.chapterId}`" class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p class="text-sm text-cyan-200">最后观看章节：{{ record.chapterTitle || '未知章节' }}</p>
              <h2 class="mt-2 text-2xl font-black">{{ record.courseTitle || '未知课程' }}</h2>
              <p class="mt-3 text-sm text-slate-400">最后学习：{{ formatTime(record.lastWatchTime) }}</p>
            </div>
            <RouterLink :to="`/courses/${record.courseId}`" class="rounded-full bg-cyan-300 px-5 py-2 text-sm font-bold text-slate-950 hover:bg-cyan-200">继续学习</RouterLink>
          </div>
          <div class="mt-6">
            <div class="mb-2 flex items-center justify-between text-sm text-slate-300">
              <span>学习进度</span>
              <span>{{ record.progress || 0 }}%</span>
            </div>
            <div class="h-3 overflow-hidden rounded-full bg-slate-800">
              <div class="h-full rounded-full bg-cyan-300" :style="{ width: `${Math.min(100, Math.max(0, record.progress || 0))}%` }"></div>
            </div>
          </div>
        </article>

        <Empty v-if="page.records.length === 0" class="rounded-[2rem] border border-white/10 bg-white/8 p-10 text-center text-slate-300" text="暂无学习记录，去课程中心开始第一节课吧" />
      </section>

      <div v-if="page.total > page.size" class="mt-8 flex items-center justify-center gap-4">
        <button class="rounded-full border border-white/10 px-5 py-2 text-sm disabled:opacity-40" :disabled="!canPrev() || loading" @click="load(page.current - 1)">上一页</button>
        <span class="text-sm text-slate-300">第 {{ page.current }} 页 / 共 {{ Math.ceil(page.total / page.size) }} 页</span>
        <button class="rounded-full border border-white/10 px-5 py-2 text-sm disabled:opacity-40" :disabled="!canNext() || loading" @click="load(page.current + 1)">下一页</button>
      </div>
    </div>
  </main>
</template>
