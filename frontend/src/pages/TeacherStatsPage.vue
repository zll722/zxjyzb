<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { BookOpen } from 'lucide-vue-next'
import Empty from '@/components/Empty.vue'
import { statsApi, type TeacherCourseStats, type TeacherStats } from '@/lib/api'

const stats      = ref<TeacherStats | null>(null)
const courseStats = ref<TeacherCourseStats[]>([])
const loading    = ref(false)
const error      = ref('')

// All icons: primary-50 bg + primary-600 icon — no per-card color
const cards = [
  { key: 'courseCount'            as const, label: '课程数',    suffix: '门' },
  { key: 'liveCount'              as const, label: '直播数',    suffix: '场' },
  { key: 'replayCount'            as const, label: '回放数',    suffix: '个' },
  { key: 'homeworkCount'          as const, label: '作业数',    suffix: '份' },
  { key: 'examCount'              as const, label: '考试数',    suffix: '场' },
  { key: 'studentSubmissionCount' as const, label: '学生提交',  suffix: '次' },
  { key: 'pendingHomeworkCount'   as const, label: '待批作业',  suffix: '份' },
  { key: 'pendingExamCount'       as const, label: '待阅考试',  suffix: '份' },
]

async function loadStats() {
  loading.value = true; error.value = ''
  try {
    const [overview, courses] = await Promise.all([statsApi.teacher(), statsApi.teacherCourses()])
    stats.value = overview; courseStats.value = courses
  } catch (err) {
    error.value = err instanceof Error ? err.message : '统计加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<template>
  <div class="mx-auto max-w-7xl">

    <div class="mb-6">
      <p class="text-sm font-medium text-neutral-400">教师数据看板</p>
      <h1 class="mt-1 text-2xl font-semibold text-neutral-900">教学运营概览</h1>
    </div>

    <div v-if="error" class="mb-4 rounded-md bg-danger-bg px-4 py-3 text-sm text-danger">{{ error }}</div>

    <!-- Skeleton -->
    <div v-if="loading" class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <div v-for="i in 8" :key="i" class="h-24 animate-pulse rounded-xl border border-border bg-white shadow-sm" />
    </div>

    <template v-else-if="stats">
      <!-- Stats grid — unified primary color -->
      <div class="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div
          v-for="card in cards"
          :key="card.key"
          class="rounded-xl border border-border bg-white p-5 shadow-sm transition-all duration-200 hover:shadow-md hover:-translate-y-px"
        >
          <p class="text-sm text-neutral-500">{{ card.label }}</p>
          <div class="mt-2 flex items-baseline gap-1">
            <span class="text-2xl font-semibold text-neutral-900">{{ stats[card.key] ?? 0 }}</span>
            <span class="text-sm text-neutral-400">{{ card.suffix }}</span>
          </div>
        </div>
      </div>

      <!-- Course detail table -->
      <div class="overflow-hidden rounded-xl border border-border bg-white shadow-sm">
        <div class="border-b border-border px-5 py-4">
          <h2 class="font-semibold text-neutral-900">课程统计明细</h2>
          <p class="mt-0.5 text-xs text-neutral-400">提交率 = 已提交 / 学习人数 × 任务数</p>
        </div>
        <div v-if="courseStats.length" class="overflow-x-auto">
          <table class="min-w-full text-sm">
            <thead>
              <tr class="border-b border-border bg-neutral-50 text-left text-xs font-medium text-neutral-500">
                <th class="px-5 py-3">课程</th>
                <th class="px-5 py-3">学习人数</th>
                <th class="px-5 py-3">作业提交率</th>
                <th class="px-5 py-3">考试提交率</th>
                <th class="px-5 py-3">考试均分</th>
                <th class="px-5 py-3">直播</th>
                <th class="px-5 py-3">回放</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-border">
              <tr
                v-for="item in courseStats"
                :key="item.courseId"
                class="text-neutral-600 hover:bg-neutral-50/80"
              >
                <td class="px-5 py-3 font-medium text-neutral-900">{{ item.courseTitle || '未命名课程' }}</td>
                <td class="px-5 py-3">{{ item.studentCount }}</td>
                <td class="px-5 py-3">{{ item.homeworkSubmissionRate }}%</td>
                <td class="px-5 py-3">{{ item.examSubmissionRate }}%</td>
                <td class="px-5 py-3">{{ item.examAverageScore }}</td>
                <td class="px-5 py-3">{{ item.liveCount }}</td>
                <td class="px-5 py-3">{{ item.replayCount }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <Empty
          v-else
          :icon="BookOpen"
          title="暂无课程统计"
          description="创建课程并开始教学后，数据会自动汇总"
        />
      </div>
    </template>
  </div>
</template>
