<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { CheckCircle2, ClipboardCheck, FileText } from 'lucide-vue-next'
import { homeworkApi, type Homework, type HomeworkSubmission } from '@/lib/api'
import Empty from '@/components/Empty.vue'

const route = useRoute()
const homework = ref<Homework | null>(null)
const submissions = ref<HomeworkSubmission[]>([])
const gradeForms = reactive<Record<number, { score: number; comment: string }>>({})
const message = ref('')
const error = ref('')
const homeworkId = computed(() => Number(route.params.id))

async function load() {
  error.value = ''
  try {
    const [detail, page] = await Promise.all([
      homeworkApi.detail(homeworkId.value),
      homeworkApi.submissions(homeworkId.value, { current: 1, size: 100 }),
    ])
    homework.value = detail
    submissions.value = page.records
    submissions.value.forEach((submission) => {
      gradeForms[submission.id] = { score: submission.score ?? 0, comment: submission.comment || '' }
    })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  }
}

async function grade(submission: HomeworkSubmission) {
  const item = gradeForms[submission.id]
  await homeworkApi.grade(homeworkId.value, submission.id, { score: Number(item.score), comment: item.comment })
  message.value = '批改已保存'
  await load()
}

onMounted(load)
</script>

<template>
  <div class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">

      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">作业批改</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">{{ homework?.title || '提交列表' }}</h1>
        <p class="mt-1 text-sm text-neutral-400">{{ homework?.courseTitle }} · 截止 {{ homework?.deadline }}</p>
      </div>

      <div v-if="message" class="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</div>
      <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

      <div class="space-y-4">
        <article v-for="submission in submissions" :key="submission.id" class="rounded-xl border border-border bg-white shadow-sm">
          <div class="border-b border-border px-5 py-4">
            <div class="flex flex-wrap items-center gap-2">
              <span class="font-medium text-neutral-900">{{ submission.studentName || `学生 #${submission.studentId}` }}</span>
              <span class="text-xs text-neutral-400">{{ submission.submittedAt }}</span>
              <span v-if="submission.reviewedAt" class="rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-medium text-emerald-700">已批改</span>
            </div>
          </div>
          <div class="grid gap-5 p-5 lg:grid-cols-[1fr_320px]">
            <!-- Submission content -->
            <div>
              <div class="rounded-lg bg-neutral-100 p-4">
                <div class="mb-2 flex items-center gap-2 text-xs text-neutral-400">
                  <FileText class="h-3.5 w-3.5" />提交内容
                </div>
                <p class="whitespace-pre-wrap text-sm text-neutral-700">{{ submission.content || '未填写文本内容' }}</p>
              </div>
              <a v-if="submission.attachmentUrl" :href="submission.attachmentUrl" target="_blank" class="mt-3 inline-flex rounded-lg border border-border px-3 py-1.5 text-sm text-primary-600 hover:bg-primary-50">下载附件</a>
            </div>

            <!-- Grade form -->
            <form class="rounded-lg border border-border p-4" @submit.prevent="grade(submission)">
              <div class="mb-3 flex items-center gap-2">
                <ClipboardCheck class="h-4 w-4 text-primary-600" />
                <h2 class="font-semibold text-neutral-900">评分</h2>
              </div>
              <input
                v-model.number="gradeForms[submission.id].score"
                min="0" max="100" step="0.5" type="number"
                placeholder="分数 (0-100)"
                class="mb-3 h-9 w-full rounded-lg border border-border bg-white px-3 text-sm outline-none transition focus:border-primary-400 focus:ring-2 focus:ring-primary-100"
              />
              <textarea
                v-model="gradeForms[submission.id].comment"
                rows="4"
                placeholder="评语（可选）"
                class="mb-3 w-full rounded-lg border border-border bg-white px-3 py-2 text-sm outline-none transition placeholder:text-neutral-400 focus:border-primary-400 focus:ring-2 focus:ring-primary-100"
              />
              <button type="submit" class="inline-flex w-full items-center justify-center gap-2 rounded-lg bg-primary-600 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700">
                <CheckCircle2 class="h-4 w-4" />保存批改
              </button>
            </form>
          </div>
        </article>
      </div>

      <Empty v-if="submissions.length === 0" title="暂无提交记录" :icon="ClipboardCheck" />
  </div>
</template>
