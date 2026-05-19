<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { FileUp, Send, Sparkles } from 'lucide-vue-next'
import { formatDeadline, homeworkApi, type Homework, type HomeworkSubmission } from '@/lib/api'

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
  message.value = ''
  error.value = ''
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
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-5xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/homeworks" class="text-sm text-cyan-200">返回作业中心</RouterLink>
        <RouterLink v-if="homework" :to="`/courses/${homework.courseId}`" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">查看课程</RouterLink>
      </nav>

      <section v-if="homework" class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">{{ homework.courseTitle || `课程 #${homework.courseId}` }} · {{ homework.teacherName || '教师' }}</p>
        <h1 class="mt-2 text-4xl font-black">{{ homework.title }}</h1>
        <p class="mt-4 whitespace-pre-wrap text-slate-300">{{ homework.description || '暂无作业说明' }}</p>
        <div class="mt-6 flex flex-wrap gap-3 text-sm">
          <span class="rounded-full bg-cyan-300/10 px-4 py-2 text-cyan-100" :class="{ 'bg-red-400/10 text-red-300': homework.deadline && new Date(homework.deadline) < new Date() }">
            截止：{{ formatDeadline(homework.deadline) }}
          </span>
          <span class="rounded-full border border-white/10 px-4 py-2 text-slate-300">状态：{{ homework.submissionStatus === 'REVIEWED' ? '已批改' : homework.submissionStatus === 'SUBMITTED' ? '已提交' : '待提交' }}</span>
        </div>
      </section>

      <p v-if="message" class="mt-6 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
      <p v-if="error" class="mt-6 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>

      <section class="mt-8 grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        <form class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur" @submit.prevent="submitHomework">
          <div class="flex items-center gap-3">
            <Send class="h-6 w-6 text-cyan-200" />
            <h2 class="text-2xl font-black">提交作业</h2>
          </div>
          <textarea v-model="content" rows="10" class="mt-6 w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="输入作业文本内容" />
          <label class="mt-4 flex cursor-pointer items-center justify-between rounded-2xl border border-dashed border-cyan-300/30 bg-cyan-300/5 px-4 py-4 text-sm text-cyan-100">
            <span class="inline-flex items-center gap-2"><FileUp class="h-4 w-4" />{{ file?.name || '选择附件' }}</span>
            <input class="hidden" type="file" @change="chooseFile" />
          </label>
          <button class="mt-5 inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">
            <Send class="h-4 w-4" />提交
          </button>
        </form>

        <aside class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="flex items-center gap-3">
            <Sparkles class="h-6 w-6 text-cyan-200" />
            <h2 class="text-2xl font-black">批改结果</h2>
          </div>
          <div v-if="latestSubmission" class="mt-6 space-y-4 text-sm text-slate-300">
            <p>提交时间：{{ latestSubmission.submittedAt ? new Date(latestSubmission.submittedAt).toLocaleString('zh-CN') : '--' }}</p>
            <a v-if="latestSubmission.attachmentUrl" :href="latestSubmission.attachmentUrl" target="_blank" class="inline-flex rounded-full bg-white/10 px-4 py-2 text-cyan-100">查看附件</a>
            <div class="rounded-2xl bg-slate-900 p-4">
              <p class="text-cyan-200">分数</p>
              <p class="mt-2 text-3xl font-black text-white">{{ latestSubmission.score ?? '待批改' }}</p>
            </div>
            <div class="rounded-2xl bg-slate-900 p-4">
              <p class="text-cyan-200">评语</p>
              <p class="mt-2 whitespace-pre-wrap">{{ latestSubmission.comment || '暂无评语' }}</p>
            </div>
          </div>
          <p v-else class="mt-6 text-sm text-slate-300">还没有提交记录。</p>
        </aside>
      </section>
    </div>
  </main>
</template>
