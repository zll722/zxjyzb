<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import Empty from '@/components/Empty.vue'
import { courseApi, type Course, type PageResult } from '@/lib/api'

const page = ref<PageResult<Course>>({ records: [], total: 0, size: 10, current: 1 })
const loading = ref(false)
const actionId = ref<number | null>(null)
const error = ref('')
const message = ref('')

async function load(current = page.value.current) {
  loading.value = true
  error.value = ''
  try {
    page.value = await courseApi.favoriteCourses({ current, size: page.value.size })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '收藏课程加载失败'
  } finally {
    loading.value = false
  }
}

async function unfavorite(course: Course) {
  actionId.value = course.id
  message.value = ''
  error.value = ''
  try {
    await courseApi.unfavorite(course.id)
    message.value = '已取消收藏'
    const nextCurrent = page.value.records.length === 1 && page.value.current > 1 ? page.value.current - 1 : page.value.current
    await load(nextCurrent)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '取消收藏失败'
  } finally {
    actionId.value = null
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
        <RouterLink to="/courses" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">发现课程</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">学生收藏夹</p>
        <h1 class="mt-2 text-4xl font-black">我的收藏</h1>
        <p class="mt-3 max-w-3xl text-slate-300">集中管理已收藏课程，可进入课程详情或取消收藏后刷新列表。</p>
        <p v-if="message" class="mt-4 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <p v-if="loading" class="mt-8 text-slate-300">收藏课程加载中...</p>

      <section v-else class="mt-8 grid gap-5 md:grid-cols-2 lg:grid-cols-3">
        <article v-for="course in page.records" :key="course.id" class="rounded-[2rem] border border-white/10 bg-white/8 p-5 backdrop-blur transition hover:-translate-y-1 hover:bg-white/12">
          <RouterLink :to="`/courses/${course.id}`" class="block">
            <div class="grid h-40 place-items-center rounded-[1.5rem] bg-gradient-to-br from-cyan-300 to-teal-300 text-5xl font-black text-slate-950">
              {{ course.title.slice(0, 1) }}
            </div>
            <div class="mt-5 flex items-center justify-between text-sm text-slate-300">
              <span>{{ course.categoryName || '未分类' }}</span>
              <span>收藏 {{ course.favoritesCount }}</span>
            </div>
            <h2 class="mt-3 text-2xl font-black hover:text-cyan-200">{{ course.title }}</h2>
            <p class="mt-3 line-clamp-2 text-sm leading-6 text-slate-300">{{ course.intro || '暂无简介' }}</p>
            <p class="mt-5 text-sm text-cyan-200">讲师：{{ course.teacherName || '未知讲师' }}</p>
          </RouterLink>
          <button class="mt-5 w-full rounded-full border border-white/10 px-4 py-2 text-sm font-bold text-white hover:bg-white/10 disabled:opacity-40" :disabled="actionId === course.id" @click="unfavorite(course)">
            {{ actionId === course.id ? '处理中...' : '取消收藏' }}
          </button>
        </article>
      </section>

      <Empty v-if="!loading && page.records.length === 0" class="mt-8 rounded-[2rem] border border-white/10 bg-white/8 p-10 text-center text-slate-300" text="暂无收藏课程，去课程中心发现感兴趣的内容吧" />

      <div v-if="page.total > page.size" class="mt-8 flex items-center justify-center gap-4">
        <button class="rounded-full border border-white/10 px-5 py-2 text-sm disabled:opacity-40" :disabled="!canPrev() || loading" @click="load(page.current - 1)">上一页</button>
        <span class="text-sm text-slate-300">第 {{ page.current }} 页 / 共 {{ Math.ceil(page.total / page.size) }} 页</span>
        <button class="rounded-full border border-white/10 px-5 py-2 text-sm disabled:opacity-40" :disabled="!canNext() || loading" @click="load(page.current + 1)">下一页</button>
      </div>
    </div>
  </main>
</template>
