<script setup lang="ts">
import { BookCheck, BarChart3, FolderTree, Images, Megaphone, MessageSquareWarning, Radio, ShieldCheck, Users } from 'lucide-vue-next'

const links = [
  { to: '/admin/users', title: '用户管理', desc: '筛选用户、查看教师资料、启用或禁用账号', icon: Users, class: 'bg-cyan-300 text-slate-950 shadow-cyan-500/20' },
  { to: '/admin/courses', title: '课程审核', desc: '处理待审核课程，支持审核通过和下架', icon: BookCheck, class: 'bg-white/8 text-white' },
  { to: '/admin/categories', title: '分类管理', desc: '维护课程一级分类，支持新增、编辑和删除', icon: FolderTree, class: 'bg-white/8 text-white' },
  { to: '/admin/lives', title: '直播管理', desc: '查看直播状态和历史记录，强制关闭违规直播', icon: Radio, class: 'bg-white/8 text-white' },
  { to: '/admin/barrages', title: '弹幕管理', desc: '筛选弹幕记录、删除违规内容、维护屏蔽词', icon: MessageSquareWarning, class: 'bg-white/8 text-white' },
  { to: '/admin/banners', title: '轮播管理', desc: '配置首页首屏轮播图、跳转链接和展示状态', icon: Images, class: 'bg-white/8 text-white' },
  { to: '/admin/announcements', title: '公告管理', desc: '发布公告、编辑内容、控制前台展示状态', icon: Megaphone, class: 'bg-white/8 text-white' },
  { to: '/admin/stats', title: '平台统计', desc: '查看用户、课程、直播、作业和考试总览', icon: BarChart3, class: 'bg-white/8 text-white' },
]
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/dashboard" class="text-sm text-cyan-200">返回工作台</RouterLink>
        <RouterLink to="/admin/stats" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">平台统计</RouterLink>
      </nav>

      <section class="overflow-hidden rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <div class="flex flex-wrap items-center justify-between gap-6">
          <div>
            <p class="text-sm text-cyan-200">管理员后台</p>
            <h1 class="mt-2 text-4xl font-black">用户、课程与公告管理</h1>
            <p class="mt-3 max-w-3xl text-slate-300">后台入口只在管理员工作台展示，公告、用户和课程管理接口由后端管理员角色做真实权限校验。</p>
          </div>
          <div class="rounded-[2rem] border border-cyan-300/30 bg-cyan-300/10 p-5 text-cyan-100">
            <ShieldCheck class="h-10 w-10" />
          </div>
        </div>
      </section>

      <section class="mt-8 grid gap-5 md:grid-cols-2 lg:grid-cols-3">
        <RouterLink v-for="item in links" :key="item.to" :to="item.to" :class="['rounded-[2rem] border border-white/10 p-6 backdrop-blur transition hover:-translate-y-1 hover:border-cyan-300/40', item.class]">
          <component :is="item.icon" class="h-8 w-8" />
          <h2 class="mt-6 text-2xl font-black">{{ item.title }}</h2>
          <p class="mt-2 text-sm opacity-75">{{ item.desc }}</p>
        </RouterLink>
      </section>
    </div>
  </main>
</template>
