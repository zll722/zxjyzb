<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ChevronLeft, FileUp, Send, Sparkles } from 'lucide-vue-next'
import { formatDeadline, homeworkApi, type Homework, type HomeworkSubmission } from '@/lib/api'
import AppLayout from '@/components/layout/AppLayout.vue'

const route = useRoute()
const homework = ref<Homework | null>(null)
const submissions = ref<HomeworkSubmission[]>([])
const content = ref('')
const file = ref<File | undefined>()
const message = ref('')
const error = ref('')

const homeworkId = computed(() => Number(route.params.id))
const latestSubmission = computed(() => submissions.value.find((item) => item.homeworkId === homeworkId.value))

async function load() {
  error.value = ''
  try {
    homework.value = await homeworkApi.detail(homeworkId.value)
    const page = await homeworkApi.mySubmissions({ current: 1, size: 50 })
    submissions.value = page.records
    content.value = latestSubmission.value?.content || ''
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  }
}

async function submitHomework() {
  message.value = ''; error.value = ''
  try {
    await homeworkApi.submit(homeworkId.value, { content: content.value, file: file.value })
    file.value = undefined
    message.value = '作业已提交'
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '提交失败'
  }
}

function chooseFile(event: Event) {
  const input = event.target as HTMLInputElement
  file.value = input.files?.[0]
}

onMounted(load)
</script>

<template>
  <AppLayout>
    <div class="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <RouterLink to="/homeworks" class="mb-6 inline-flex items-center gap-1 text-sm text-neutral-500 hover:text-primary-600">
        <ChevronLeft class="h-4 w-4" />返回作业中心
      </RouterLink>

      <!-- Homework info -->
      <div v-if="homework" class="mb-6 rounded-xl border border-border bg-white p-5 shadow-sm">
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="text-xs text-primary-600">{{ homework.courseTitle || `课程 #${homework.courseId}` }} · {{ homework.teacherName || '教师' }}</p>
            <h1 class="mt-1 text-xl font-bold text-neutral-900">{{ homework.title }}</h1>
            <p class="mt-2 whitespace-pre-wrap text-sm text-neutral-600">{{ homework.description || '暂无作业说明' }}</p>
          </div>
          <div class="flex flex-col gap-1.5 text-right">
            <span
              class="rounded-full px-2.5 py-0.5 text-xs font-medium"
              :class="{
                'bg-neutral-100 text-neutral-600': homework.submissionStatus === 'PENDING' || !homework.submissionStatus,
                'bg-primary-50 text-primary-700': homework.submissionStatus === 'SUBMITTED',
                'bg-success-bg text-success': homework.submissionStatus === 'REVIEWED',
              }"
            >
              {{ homework.submissionStatus === 'REVIEWED' ? '已批改' : homework.submissionStatus === 'SUBMITTED' ? '已提交' : '待提交' }}
            </span>
            <span
              class="text-xs"
              :class="homework.deadline && new Date(homework.deadline) < new Date() ? 'text-red-500' : 'text-neutral-400'"
            >
              截止：{{ formatDeadline(homework.deadline) }}
            </span>
          </div>
        </div>
      </div>

      <div v-if="message" class="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</div>
      <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

      <div class="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        <!-- Submit form -->
        <form class="rounded-xl border border-border bg-white p-5 shadow-sm" @submit.prevent="submitHomework">
          <div class="mb-4 flex items-center gap-2">
            <Send class="h-5 w-5 text-primary-600" />
            <h2 class="font-semibold text-neutral-900">提交作业</h2>
          </div>
          <textarea
            v-model="content"
            rows="10"
            placeholder="输入作业文本内容..."
            class="w-full rounded-lg border border-border bg-white px-3 py-2.5 text-sm text-neutral-900 outline-none transition placeholder:text-neutral-400 focus:border-primary-400 focus:ring-2 focus:ring-primary-100"
          />
          <label class="mt-3 flex cursor-pointer items-center justify-between rounded-lg border border-dashed border-border bg-neutral-100 px-4 py-3 text-sm text-neutral-500 transition hover:border-primary-400 hover:text-primary-600">
            <span class="inline-flex items-center gap-2">
              <FileUp class="h-4 w-4" />{{ file?.name || '选择附件（可选）' }}
            </span>
            <input class="hidden" type="file" @change="chooseFile" />
          </label>
          <button type="submit" class="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-lg bg-primary-600 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-primary-700">
            <Send class="h-4 w-4" />提交作业
          </button>
        </form>

        <!-- Review result -->
        <aside class="rounded-xl border border-border bg-white p-5 shadow-sm">
          <div class="mb-4 flex items-center gap-2">
            <Sparkles class="h-5 w-5 text-primary-600" />
            <h2 class="font-semibold text-neutral-900">批改结果</h2>
          </div>
          <div v-if="latestSubmission" class="space-y-3">
            <p class="text-xs text-neutral-400">
              提交时间：{{ latestSubmission.submittedAt ? new Date(latestSubmission.submittedAt).toLocaleString('zh-CN') : '--' }}
            </p>
            <a v-if="latestSubmission.attachmentUrl" :href="latestSubmission.attachmentUrl" target="_blank" class="inline-flex rounded-lg border border-border px-3 py-1.5 text-sm text-primary-600 hover:bg-primary-50">查看附件</a>
            <div class="rounded-lg bg-neutral-100 px-4 py-3">
              <p class="text-xs text-neutral-400">得分</p>
              <p class="mt-1 text-3xl font-bold text-neutral-900">{{ latestSubmission.score ?? '待批改' }}</p>
            </div>
            <div class="rounded-lg bg-neutral-100 px-4 py-3">
              <p class="text-xs text-neutral-400">评语</p>
              <p class="mt-1 whitespace-pre-wrap text-sm text-neutral-700">{{ latestSubmission.comment || '暂无评语' }}</p>
            </div>
          </div>
          <p v-else class="text-sm text-neutral-400">还没有提交记录。</p>
        </aside>
      </div>
    </div>
  </AppLayout>
</template>
