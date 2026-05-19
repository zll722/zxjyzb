<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import Empty from '@/components/Empty.vue'
import { statsApi, type StudentStats } from '@/lib/api'

const stats = ref<StudentStats | null>(null)
const loading = ref(false)
const error = ref('')

const cards = [
  { key: 'learnedCourseCount', label: '学习课程', suffix: '门' },
  { key: 'favoriteCount', label: '收藏课程', suffix: '门' },
  { key: 'homeworkSubmissionCount', label: '作业提交', suffix: '次' },
  { key: 'examParticipationCount', label: '考试参与', suffix: '次' },
  { key: 'averageScore', label: '考试均分', suffix: '分' },
] as const

function formatTime(value?: string) {
  if (!value) return '暂无时间'
  return value.replace('T', ' ').slice(0, 16)
}

async function loadStats() {
  loading.value = true
  error.value = ''
  try {
    stats.value = await statsApi.student()
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
        <RouterLink to="/courses" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">继续学习</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">学生数据看板</p>
        <h1 class="mt-2 text-4xl font-black">我的学习数据</h1>
        <p class="mt-3 max-w-3xl text-slate-300">汇总课程学习、收藏、作业提交、考试参与和近期学习轨迹。</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <p v-if="loading" class="mt-8 text-slate-300">统计加载中...</p>

      <template v-else-if="stats">
        <section class="mt-8 grid gap-5 md:grid-cols-2 lg:grid-cols-5">
          <div v-for="card in cards" :key="card.key" class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
            <p class="text-sm text-slate-400">{{ card.label }}</p>
            <p class="mt-4 text-4xl font-black text-cyan-200">{{ stats[card.key] ?? 0 }}<span class="ml-1 text-base text-slate-400">{{ card.suffix }}</span></p>
          </div>
        </section>

        <section class="mt-8 rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="flex items-center justify-between gap-4">
            <div>
              <p class="text-sm text-cyan-200">最近记录</p>
              <h2 class="mt-1 text-2xl font-black">近期学习轨迹</h2>
            </div>
          </div>

          <div v-if="stats.recentRecords.length" class="mt-6 space-y-4">
            <div v-for="record in stats.recentRecords" :key="`${record.courseId}-${record.chapterId}`" class="rounded-3xl border border-white/10 bg-slate-900/80 p-5">
              <div class="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <p class="text-lg font-bold">{{ record.courseTitle || '未知课程' }}</p>
                  <p class="mt-1 text-sm text-slate-400">{{ record.chapterTitle || '未知章节' }}</p>
                </div>
                <p class="rounded-full bg-cyan-300 px-4 py-2 text-sm font-bold text-slate-950">{{ record.progress }}%</p>
              </div>
              <p class="mt-3 text-sm text-slate-400">最后学习：{{ formatTime(record.lastWatchTime) }}</p>
            </div>
          </div>
          <Empty v-else class="mt-6" text="暂无学习记录，去课程中心开始第一节课吧" />
        </section>
      </template>
    </div>
  </main>
</template>
