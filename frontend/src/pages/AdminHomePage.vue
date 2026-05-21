<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { BarChart3, BookCheck, ChevronRight, FolderTree, Images, Megaphone, MessageSquareWarning, Radio, ShieldCheck, Users } from 'lucide-vue-next'
import { RouterLink } from 'vue-router'
import { statsApi, type PlatformStats } from '@/lib/api'

const stats = ref<PlatformStats | null>(null)
const loading = ref(false)

// All icons unified: primary-50 bg + primary-600 icon
const quickLinks = [
  { to: '/admin/users',         label: '用户管理', icon: Users },
  { to: '/admin/courses',       label: '课程审核', icon: BookCheck },
  { to: '/admin/categories',    label: '分类管理', icon: FolderTree },
  { to: '/admin/lives',         label: '直播管理', icon: Radio },
  { to: '/admin/barrages',      label: '弹幕管理', icon: MessageSquareWarning },
  { to: '/admin/banners',       label: '轮播管理', icon: Images },
  { to: '/admin/announcements', label: '公告管理', icon: Megaphone },
  { to: '/admin/stats',         label: '平台统计', icon: BarChart3 },
]

const statCards = [
  { key: 'userCount'    as const, label: '用户总数',  suffix: '人' },
  { key: 'teacherCount' as const, label: '教师数',    suffix: '人' },
  { key: 'courseCount'  as const, label: '课程数',    suffix: '门' },
  { key: 'liveCount'    as const, label: '直播场次',  suffix: '场' },
]

onMounted(async () => {
  loading.value = true
  try { stats.value = await statsApi.platform() } catch { /* ignore */ }
  finally { loading.value = false }
})
</script>

<template>
  <div class="mx-auto max-w-5xl">

    <!-- Welcome header — no amber, no orange, no shield icon -->
    <div class="mb-6 flex flex-wrap items-center justify-between gap-4 rounded-xl border border-border bg-white p-6 shadow-sm">
      <div>
        <!-- "管理员后台" label: text-tertiary, NOT amber -->
        <p class="text-sm font-medium text-neutral-400">管理员后台</p>
        <h1 class="mt-1 text-2xl font-semibold text-neutral-900">管理概览</h1>
        <p class="mt-1 text-sm text-neutral-500">欢迎回来，平台数据与功能入口均在左侧菜单中。</p>
      </div>
      <!-- Decorative: very light primary geometric, not a solid colored badge -->
      <div class="grid h-14 w-14 place-items-center rounded-xl bg-primary-50 text-primary-600">
        <ShieldCheck class="h-7 w-7" stroke-width="1.5" />
      </div>
    </div>

    <!-- ─────────────────────────────────────────────────────
        Stat cards — icon + label on top, big number + unit in middle
        ───────────────────────────────────────────────────── -->
    <div class="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <template v-if="loading">
        <div v-for="i in 4" :key="i" class="h-28 animate-pulse rounded-xl bg-neutral-100" />
      </template>
      <template v-else-if="stats">
        <div
          v-for="card in statCards"
          :key="card.key"
          class="rounded-xl border border-border bg-white p-6 shadow-sm transition-all duration-200 hover:shadow-md hover:-translate-y-px"
        >
          <!-- Top: icon + label -->
          <div class="mb-3 flex items-center gap-2">
            <div class="grid h-8 w-8 place-items-center rounded-lg bg-primary-50 text-primary-600">
              <BarChart3 class="h-4 w-4" stroke-width="1.75" />
            </div>
            <p class="text-sm text-neutral-500">{{ card.label }}</p>
          </div>
          <!-- Middle: big number + unit -->
          <div class="flex items-baseline gap-1">
            <span class="text-3xl font-semibold text-neutral-900">{{ stats[card.key] ?? 0 }}</span>
            <span class="ml-0.5 text-base text-neutral-400">{{ card.suffix }}</span>
          </div>
        </div>
      </template>
    </div>

    <!-- ─────────────────────────────────────────────────────
        Quick-access links — ALL icons use primary-50/600
        ───────────────────────────────────────────────────── -->
    <div>
      <h2 class="mb-3 text-xs font-semibold uppercase tracking-widest text-neutral-400">快速入口</h2>
      <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <RouterLink
          v-for="item in quickLinks"
          :key="item.to"
          :to="item.to"
          class="group flex items-center gap-3 rounded-xl border border-border bg-white p-4 shadow-sm transition-all duration-200 hover:bg-primary-50 hover:border-primary-200 hover:shadow-md"
        >
          <!-- Unified icon: primary-50 + primary-600, no per-card color -->
          <div class="grid h-10 w-10 flex-shrink-0 place-items-center rounded-lg bg-primary-50 text-primary-600 transition-colors duration-200 group-hover:bg-primary-100">
            <component :is="item.icon" class="h-5 w-5" stroke-width="1.75" />
          </div>
          <span class="flex-1 font-medium text-neutral-800">{{ item.label }}</span>
          <!-- Arrow indicator -->
          <ChevronRight
            class="h-4 w-4 flex-shrink-0 text-neutral-400 transition-transform duration-200 group-hover:translate-x-0.5"
          />
        </RouterLink>
      </div>
    </div>
  </div>
</template>
