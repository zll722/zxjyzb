<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ChevronLeft } from 'lucide-vue-next'
import Empty from '@/components/Empty.vue'
import { courseApi, type Course, type PageResult } from '@/lib/api'
import AppLayout from '@/components/layout/AppLayout.vue'

const page = ref<PageResult<Course>>({ records: [], total: 0, size: 10, current: 1 })
const loading = ref(false)
const actionId = ref<number | null>(null)
const error = ref('')
const message = ref('')

async function load(current = page.value.current) {
  loading.value = true; error.value = ''
  try { page.value = await courseApi.favoriteCourses({ current, size: page.value.size }) }
  catch (err) { error.value = err instanceof Error ? err.message : '收藏课程加载失败' }
  finally { loading.value = false }
}

async function unfavorite(course: Course) {
  actionId.value = course.id; message.value = ''; error.value = ''
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

onMounted(() => load(1))
</script>

<template>
  <AppLayout>
    <div class="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <RouterLink to="/dashboard" class="mb-6 inline-flex items-center gap-1 text-sm text-neutral-500 hover:text-primary-600">
        <ChevronLeft class="h-4 w-4" />返回工作台
      </RouterLink>
      <div class="mb-6">
        <p class="text-sm font-medium text-primary-600">我的收藏</p>
        <h1 class="mt-1 text-2xl font-bold text-neutral-900">收藏的课程</h1>
      </div>

      <div v-if="message" class="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</div>
      <div v-if="error" class="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{{ error }}</div>

      <div v-if="loading" class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div v-for="i in 6" :key="i" class="h-56 animate-pulse rounded-xl border border-border bg-white" />
      </div>

      <div v-else class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <article v-for="course in page.records" :key="course.id" class="rounded-xl border border-border bg-white shadow-sm transition hover:shadow-md">
          <RouterLink :to="`/courses/${course.id}`" class="block">
            <div class="grid h-36 place-items-center overflow-hidden rounded-t-xl bg-gradient-to-br from-primary-100 to-primary-200 text-4xl font-bold text-primary-600">
              {{ course.title.slice(0, 1) }}
            </div>
            <div class="p-4">
              <p class="text-xs text-neutral-400">{{ course.categoryName || '未分类' }}</p>
              <h2 class="mt-1 font-semibold text-neutral-900 hover:text-primary-600">{{ course.title }}</h2>
              <p class="mt-1.5 line-clamp-2 text-xs leading-5 text-neutral-500">{{ course.intro || '暂无简介' }}</p>
              <p class="mt-2 text-xs text-neutral-400">讲师：{{ course.teacherName || '未知讲师' }}</p>
            </div>
          </RouterLink>
          <div class="border-t border-border px-4 pb-4">
            <button
              class="mt-3 w-full rounded-lg border border-border py-2 text-sm font-medium text-neutral-600 transition hover:bg-red-50 hover:border-red-200 hover:text-red-600 disabled:opacity-40"
              :disabled="actionId === course.id"
              @click="unfavorite(course)"
            >
              {{ actionId === course.id ? '处理中...' : '取消收藏' }}
            </button>
          </div>
        </article>
      </div>

      <Empty v-if="!loading && page.records.length === 0" title="暂无收藏课程，去课程中心发现感兴趣的内容吧" />

      <div v-if="page.total > page.size" class="mt-6 flex items-center justify-center gap-3 text-sm">
        <button class="rounded-lg border border-border px-4 py-2 text-neutral-600 transition hover:bg-neutral-100 disabled:opacity-40" :disabled="page.current <= 1 || loading" @click="load(page.current - 1)">上一页</button>
        <span class="text-neutral-400">第 {{ page.current }} 页 / 共 {{ Math.ceil(page.total / page.size) }} 页</span>
        <button class="rounded-lg border border-border px-4 py-2 text-neutral-600 transition hover:bg-neutral-100 disabled:opacity-40" :disabled="page.current * page.size >= page.total || loading" @click="load(page.current + 1)">下一页</button>
      </div>
    </div>
  </AppLayout>
</template>
