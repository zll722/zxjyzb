<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  BarChart3, Bell, BookMarked, BookOpen, Clapperboard,
  FileQuestion, Heart, LogOut, Megaphone, NotebookPen,
  Radio, ShieldCheck, UserRound,
} from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { messageApi } from '@/lib/api'

const router = useRouter()
const auth = useAuth()
const unreadCount = ref(0)

async function loadUnread() {
  if (!auth.state.user) return
  try { unreadCount.value = await messageApi.unreadCount() } catch { /* ignore */ }
}

function logout() {
  auth.logout()
  router.push('/')
}

onMounted(loadUnread)
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <!-- 工作台头部 -->
      <header class="flex flex-wrap items-center justify-between gap-4 rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
        <div class="flex items-center gap-4">
          <div class="grid h-14 w-14 place-items-center rounded-2xl bg-cyan-400/20 text-2xl font-black text-cyan-300">
            {{ auth.state.user?.username?.slice(0, 1).toUpperCase() || '?' }}
          </div>
          <div>
            <h1 class="text-2xl font-black">{{ auth.state.user?.username || 'EduLive 用户' }}</h1>
            <p class="mt-1 text-sm text-slate-400">
              {{ auth.state.user?.role === 'STUDENT' ? '学生' : auth.state.user?.role === 'TEACHER' ? '教师' : auth.state.user?.role === 'ADMIN' ? '管理员' : '加载中' }}
              · {{ auth.state.user?.email }}
            </p>
          </div>
        </div>
        <div class="flex flex-wrap gap-3">
          <RouterLink to="/profile" class="inline-flex items-center gap-2 rounded-full bg-cyan-300 px-5 py-3 text-sm font-bold text-slate-950 transition hover:bg-cyan-200">
            <UserRound class="h-4 w-4" />个人中心
          </RouterLink>
          <RouterLink to="/messages" class="relative inline-flex items-center gap-2 rounded-full border border-white/10 px-5 py-3 text-sm font-bold transition hover:bg-white/10">
            <Bell class="h-4 w-4" />消息
            <span v-if="unreadCount > 0" class="absolute -right-1 -top-1 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-xs font-bold">
              {{ unreadCount > 99 ? '99+' : unreadCount }}
            </span>
          </RouterLink>
          <button class="inline-flex items-center gap-2 rounded-full border border-white/10 px-5 py-3 text-sm transition hover:bg-white/10" @click="logout">
            <LogOut class="h-4 w-4" />退出登录
          </button>
        </div>
      </header>

      <!-- 通用功能（全角色可见） -->
      <section class="mt-8 grid gap-5 md:grid-cols-2 lg:grid-cols-3">

        <!-- 课程中心：全员可见 -->
        <div class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <BookOpen class="h-8 w-8 text-cyan-200" />
          <h2 class="mt-6 text-2xl font-black">课程中心</h2>
          <p class="mt-2 text-sm text-slate-300">浏览并学习平台上发布的课程。</p>
          <div class="mt-6 flex flex-wrap gap-3">
            <RouterLink to="/courses" class="rounded-full bg-cyan-300 px-4 py-2 text-sm font-bold text-slate-950">浏览课程</RouterLink>
          </div>
        </div>

        <!-- 直播中心：全员可见 -->
        <div class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <Radio class="h-8 w-8 text-cyan-200" />
          <h2 class="mt-6 text-2xl font-black">直播中心</h2>
          <p class="mt-2 text-sm text-slate-300">进入直播课堂，参与聊天、弹幕与投票互动。</p>
          <div class="mt-6 flex flex-wrap gap-3">
            <RouterLink to="/live" class="rounded-full bg-cyan-300 px-4 py-2 text-sm font-bold text-slate-950">直播中心</RouterLink>
            <RouterLink to="/replays" class="rounded-full border border-white/10 px-4 py-2 text-sm font-bold text-white">回放中心</RouterLink>
          </div>
        </div>

        <!-- 公告消息：全员可见 -->
        <div class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <Megaphone class="h-8 w-8 text-cyan-200" />
          <h2 class="mt-6 text-2xl font-black">公告消息</h2>
          <p class="mt-2 text-sm text-slate-300">查看平台公告与个人站内提醒。</p>
          <div class="mt-6 flex flex-wrap gap-3">
            <RouterLink to="/announcements" class="rounded-full bg-cyan-300 px-4 py-2 text-sm font-bold text-slate-950">平台公告</RouterLink>
            <RouterLink to="/messages" class="relative inline-flex items-center gap-2 rounded-full border border-white/10 px-4 py-2 text-sm font-bold text-white">
              <Bell class="h-4 w-4" />我的消息
              <span v-if="unreadCount > 0" class="absolute -right-1 -top-1 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-xs font-bold">{{ unreadCount }}</span>
            </RouterLink>
          </div>
        </div>
      </section>

      <!-- 学生专属 -->
      <section v-if="auth.state.user?.role === 'STUDENT'" class="mt-5 grid gap-5 md:grid-cols-2 lg:grid-cols-3">
        <div class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <BookMarked class="h-8 w-8 text-cyan-200" />
          <h2 class="mt-6 text-2xl font-black">我的学习</h2>
          <p class="mt-2 text-sm text-slate-300">查看学习进度、已购课程与收藏。</p>
          <div class="mt-6 flex flex-wrap gap-3">
            <RouterLink to="/dashboard/my-learning" class="rounded-full bg-cyan-300 px-4 py-2 text-sm font-bold text-slate-950">学习记录</RouterLink>
            <RouterLink to="/dashboard/my-favorites" class="inline-flex items-center gap-1 rounded-full border border-white/10 px-4 py-2 text-sm font-bold text-white">
              <Heart class="h-4 w-4" />我的收藏
            </RouterLink>
          </div>
        </div>
        <div class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <NotebookPen class="h-8 w-8 text-cyan-200" />
          <h2 class="mt-6 text-2xl font-black">作业 & 考试</h2>
          <p class="mt-2 text-sm text-slate-300">提交作业、参与在线考试、查看批改结果。</p>
          <div class="mt-6 flex flex-wrap gap-3">
            <RouterLink to="/homeworks" class="rounded-full bg-cyan-300 px-4 py-2 text-sm font-bold text-slate-950">我的作业</RouterLink>
            <RouterLink to="/exams" class="inline-flex items-center gap-1 rounded-full border border-white/10 px-4 py-2 text-sm font-bold text-white">
              <FileQuestion class="h-4 w-4" />我的考试
            </RouterLink>
          </div>
        </div>
        <div class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <BarChart3 class="h-8 w-8 text-cyan-200" />
          <h2 class="mt-6 text-2xl font-black">学习数据</h2>
          <p class="mt-2 text-sm text-slate-300">查看学习统计、作业提交率与考试平均分。</p>
          <div class="mt-6">
            <RouterLink to="/dashboard/student-stats" class="rounded-full bg-cyan-300 px-4 py-2 text-sm font-bold text-slate-950">我的数据</RouterLink>
          </div>
        </div>
      </section>

      <!-- 教师专属 -->
      <section v-if="auth.state.user?.role === 'TEACHER'" class="mt-5 grid gap-5 md:grid-cols-2 lg:grid-cols-3">
        <div class="rounded-[2rem] border border-cyan-300/20 bg-cyan-300 p-6 text-slate-950 shadow-xl shadow-cyan-500/20">
          <BookOpen class="h-8 w-8" />
          <h2 class="mt-6 text-2xl font-black">课程管理</h2>
          <p class="mt-2 text-sm opacity-75">发布课程、添加章节、上传视频并提交审核。</p>
          <div class="mt-6 flex flex-wrap gap-3">
            <RouterLink to="/teacher/courses" class="rounded-full bg-slate-950 px-4 py-2 text-sm font-bold text-white">我的课程</RouterLink>
          </div>
        </div>
        <div class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <Radio class="h-8 w-8 text-cyan-200" />
          <h2 class="mt-6 text-2xl font-black">直播管理</h2>
          <p class="mt-2 text-sm text-slate-300">创建直播间、发起直播、管理聊天互动。</p>
          <div class="mt-6 flex flex-wrap gap-3">
            <RouterLink to="/teacher/live" class="rounded-full bg-cyan-300 px-4 py-2 text-sm font-bold text-slate-950">直播管理</RouterLink>
            <RouterLink to="/teacher/replays" class="rounded-full border border-white/10 px-4 py-2 text-sm font-bold text-white">
              <Clapperboard class="mr-1 inline h-3 w-3" />回放管理
            </RouterLink>
          </div>
        </div>
        <div class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <NotebookPen class="h-8 w-8 text-cyan-200" />
          <h2 class="mt-6 text-2xl font-black">作业 & 考试</h2>
          <p class="mt-2 text-sm text-slate-300">布置作业、创建考试、批改学生提交结果。</p>
          <div class="mt-6 flex flex-wrap gap-3">
            <RouterLink to="/teacher/homeworks" class="rounded-full bg-cyan-300 px-4 py-2 text-sm font-bold text-slate-950">作业管理</RouterLink>
            <RouterLink to="/teacher/exams" class="inline-flex items-center gap-1 rounded-full border border-white/10 px-4 py-2 text-sm font-bold text-white">
              <FileQuestion class="h-4 w-4" />考试管理
            </RouterLink>
          </div>
        </div>
        <div class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur md:col-span-2 lg:col-span-1">
          <BarChart3 class="h-8 w-8 text-cyan-200" />
          <h2 class="mt-6 text-2xl font-black">教学数据</h2>
          <p class="mt-2 text-sm text-slate-300">查看课程学生数、作业提交率与考试均分。</p>
          <div class="mt-6">
            <RouterLink to="/dashboard/teacher-stats" class="rounded-full bg-cyan-300 px-4 py-2 text-sm font-bold text-slate-950">教学数据</RouterLink>
          </div>
        </div>
      </section>

      <!-- 管理员专属 -->
      <section v-if="auth.state.user?.role === 'ADMIN'" class="mt-5 grid gap-5 md:grid-cols-2 lg:grid-cols-3">
        <div class="rounded-[2rem] border border-white/10 bg-cyan-300 p-6 text-slate-950 shadow-xl shadow-cyan-500/20">
          <ShieldCheck class="h-8 w-8" />
          <h2 class="mt-6 text-2xl font-black">后台管理</h2>
          <p class="mt-2 text-sm opacity-75">用户管理、课程审核、弹幕监控、Banner 配置。</p>
          <div class="mt-6 flex flex-wrap gap-3">
            <RouterLink to="/admin" class="rounded-full bg-slate-950 px-4 py-2 text-sm font-bold text-white">后台首页</RouterLink>
            <RouterLink to="/admin/users" class="rounded-full bg-white/60 px-4 py-2 text-sm font-bold text-slate-950">用户管理</RouterLink>
          </div>
        </div>
        <div class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <BookOpen class="h-8 w-8 text-cyan-200" />
          <h2 class="mt-6 text-2xl font-black">内容管理</h2>
          <p class="mt-2 text-sm text-slate-300">课程审核、公告管理、Banner 轮播配置。</p>
          <div class="mt-6 flex flex-wrap gap-3">
            <RouterLink to="/admin/courses" class="rounded-full bg-cyan-300 px-4 py-2 text-sm font-bold text-slate-950">课程审核</RouterLink>
            <RouterLink to="/admin/announcements" class="rounded-full border border-white/10 px-4 py-2 text-sm font-bold text-white">公告管理</RouterLink>
            <RouterLink to="/admin/banners" class="rounded-full border border-white/10 px-4 py-2 text-sm font-bold text-white">Banner</RouterLink>
          </div>
        </div>
        <div class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <BarChart3 class="h-8 w-8 text-cyan-200" />
          <h2 class="mt-6 text-2xl font-black">平台数据</h2>
          <p class="mt-2 text-sm text-slate-300">用户数、课程数、直播场次等平台统计。</p>
          <div class="mt-6 flex flex-wrap gap-3">
            <RouterLink to="/admin/stats" class="rounded-full bg-cyan-300 px-4 py-2 text-sm font-bold text-slate-950">平台统计</RouterLink>
            <RouterLink to="/admin/barrages" class="rounded-full border border-white/10 px-4 py-2 text-sm font-bold text-white">弹幕管理</RouterLink>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>
