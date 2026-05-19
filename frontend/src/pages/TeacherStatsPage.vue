<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import Empty from '@/components/Empty.vue'
import { statsApi, type TeacherCourseStats, type TeacherStats } from '@/lib/api'

const stats = ref<TeacherStats | null>(null)
const courseStats = ref<TeacherCourseStats[]>([])
const loading = ref(false)
const error = ref('')

const cards = [
  { key: 'courseCount', label: '课程数', suffix: '门' },
  { key: 'liveCount', label: '直播数', suffix: '场' },
  { key: 'replayCount', label: '回放数', suffix: '个' },
  { key: 'homeworkCount', label: '作业数', suffix: '份' },
  { key: 'examCount', label: '考试数', suffix: '场' },
  { key: 'studentSubmissionCount', label: '学生提交', suffix: '次' },
  { key: 'pendingHomeworkCount', label: '待批作业', suffix: '份' },
  { key: 'pendingExamCount', label: '待阅考试', suffix: '份' },
] as const

async function loadStats() {
  loading.value = true
  error.value = ''
  try {
    const [overview, courses] = await Promise.all([statsApi.teacher(), statsApi.teacherCourses()])
    stats.value = overview
    courseStats.value = courses
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
        <RouterLink to="/teacher/courses" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">课程管理</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">教师数据看板</p>
        <h1 class="mt-2 text-4xl font-black">教学运营概览</h1>
        <p class="mt-3 max-w-3xl text-slate-300">聚合作业、考试、直播与回放数据，快速定位待批改和课程维度表现。</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <p v-if="loading" class="mt-8 text-slate-300">统计加载中...</p>

      <template v-else-if="stats">
        <section class="mt-8 grid gap-5 md:grid-cols-2 lg:grid-cols-4">
          <div v-for="card in cards" :key="card.key" class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
            <p class="text-sm text-slate-400">{{ card.label }}</p>
            <p class="mt-4 text-4xl font-black text-cyan-200">{{ stats[card.key] ?? 0 }}<span class="ml-1 text-base text-slate-400">{{ card.suffix }}</span></p>
          </div>
        </section>

        <section class="mt-8 rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="flex flex-wrap items-end justify-between gap-4">
            <div>
              <p class="text-sm text-cyan-200">课程维度</p>
              <h2 class="mt-1 text-2xl font-black">课程统计明细</h2>
            </div>
            <p class="text-sm text-slate-400">提交率 = 已提交 / 学习人数 × 任务数</p>
          </div>

          <div v-if="courseStats.length" class="mt-6 overflow-x-auto">
            <table class="min-w-full text-left text-sm">
              <thead class="text-slate-400">
                <tr class="border-b border-white/10">
                  <th class="py-3 pr-4">课程</th>
                  <th class="py-3 pr-4">学习人数</th>
                  <th class="py-3 pr-4">作业提交率</th>
                  <th class="py-3 pr-4">考试提交率</th>
                  <th class="py-3 pr-4">考试均分</th>
                  <th class="py-3 pr-4">直播</th>
                  <th class="py-3 pr-4">回放</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in courseStats" :key="item.courseId" class="border-b border-white/5 text-slate-200">
                  <td class="py-4 pr-4 font-bold text-white">{{ item.courseTitle || '未命名课程' }}</td>
                  <td class="py-4 pr-4">{{ item.studentCount }}</td>
                  <td class="py-4 pr-4">{{ item.homeworkSubmissionRate }}%</td>
                  <td class="py-4 pr-4">{{ item.examSubmissionRate }}%</td>
                  <td class="py-4 pr-4">{{ item.examAverageScore }}</td>
                  <td class="py-4 pr-4">{{ item.liveCount }}</td>
                  <td class="py-4 pr-4">{{ item.replayCount }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <Empty v-else class="mt-6" text="暂无课程统计，创建课程后会自动汇总数据" />
        </section>
      </template>
    </div>
  </main>
</template>
