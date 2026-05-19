<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { CheckCircle2, ClipboardCheck, FileText } from 'lucide-vue-next'
import { homeworkApi, type Homework, type HomeworkSubmission } from '@/lib/api'

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
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/teacher/homeworks" class="text-sm text-cyan-200">返回作业管理</RouterLink>
        <RouterLink to="/dashboard" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">工作台</RouterLink>
      </nav>
      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">作业批改</p>
        <h1 class="mt-2 text-4xl font-black">{{ homework?.title || '提交列表' }}</h1>
        <p class="mt-3 text-slate-300">{{ homework?.courseTitle || '' }} · 截止 {{ homework?.deadline || '' }}</p>
        <p v-if="message" class="mt-4 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <section class="mt-8 space-y-5">
        <article v-for="submission in submissions" :key="submission.id" class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="grid gap-6 lg:grid-cols-[1fr_360px]">
            <div>
              <div class="flex flex-wrap items-center gap-3">
                <span class="rounded-full bg-cyan-300/10 px-4 py-2 text-sm text-cyan-100">{{ submission.studentName || `学生 #${submission.studentId}` }}</span>
                <span class="rounded-full border border-white/10 px-4 py-2 text-sm text-slate-300">{{ submission.submittedAt }}</span>
                <span v-if="submission.reviewedAt" class="rounded-full border border-emerald-300/30 px-4 py-2 text-sm text-emerald-100">已批改</span>
              </div>
              <div class="mt-5 rounded-2xl bg-slate-900 p-5">
                <p class="mb-3 flex items-center gap-2 text-sm text-cyan-200"><FileText class="h-4 w-4" />提交内容</p>
                <p class="whitespace-pre-wrap text-slate-200">{{ submission.content || '未填写文本内容' }}</p>
              </div>
              <a v-if="submission.attachmentUrl" :href="submission.attachmentUrl" target="_blank" class="mt-4 inline-flex rounded-full bg-white/10 px-4 py-2 text-sm text-cyan-100">下载附件</a>
            </div>
            <form class="rounded-2xl border border-white/10 bg-slate-900 p-5" @submit.prevent="grade(submission)">
              <h2 class="flex items-center gap-2 text-xl font-black"><ClipboardCheck class="h-5 w-5 text-cyan-200" />评分</h2>
              <input v-model.number="gradeForms[submission.id].score" min="0" max="100" step="0.5" type="number" class="mt-5 w-full rounded-2xl border border-white/10 bg-slate-950 px-4 py-3 outline-none focus:border-cyan-300" placeholder="分数" />
              <textarea v-model="gradeForms[submission.id].comment" rows="5" class="mt-4 w-full rounded-2xl border border-white/10 bg-slate-950 px-4 py-3 outline-none focus:border-cyan-300" placeholder="评语" />
              <button class="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200"><CheckCircle2 class="h-4 w-4" />保存批改</button>
            </form>
          </div>
        </article>
      </section>

      <div v-if="submissions.length === 0" class="mt-12 rounded-[2rem] border border-white/10 p-10 text-center text-slate-300">
        <ClipboardCheck class="mx-auto mb-4 h-10 w-10 text-cyan-200" />
        暂无提交记录
      </div>
    </div>
  </main>
</template>
