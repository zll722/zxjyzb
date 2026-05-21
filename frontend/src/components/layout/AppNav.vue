<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { Bell, BookOpen, LayoutDashboard, LogOut, Menu, Megaphone, ShieldCheck, UserRound, Video, X } from 'lucide-vue-next'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const auth = useAuth()
const menuOpen = ref(false)

defineProps<{ unread?: number }>()

function logout() {
  auth.logout()
  router.push('/')
}

const navLinks = [
  { to: '/courses',       label: '课程', icon: BookOpen },
  { to: '/live',          label: '直播', icon: Video },
  { to: '/announcements', label: '公告', icon: Megaphone },
]
</script>

<template>
  <header class="sticky top-0 z-50 w-full border-b border-border bg-white/95 backdrop-blur-sm">
    <div class="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">

      <!-- Logo + Desktop nav (grouped left) -->
      <div class="flex items-center gap-0">
        <RouterLink to="/" class="flex flex-shrink-0 items-center gap-2.5 pr-4">
          <div class="grid h-9 w-9 place-items-center rounded-lg bg-primary-600 text-white shadow-sm">
            <Video class="h-4 w-4" />
          </div>
          <span class="text-base font-semibold tracking-tight text-neutral-900">EduLive</span>
        </RouterLink>

        <!-- Divider -->
        <div class="hidden h-5 w-px bg-neutral-200 md:block" />

        <!-- Desktop nav -->
        <nav class="hidden items-center md:flex">
          <RouterLink
            v-for="link in navLinks"
            :key="link.to"
            :to="link.to"
            class="nav-link group relative flex items-center gap-1.5 px-3.5 py-2 text-sm font-medium text-neutral-500 transition-colors duration-200 hover:text-neutral-900"
            active-class="nav-link--active"
          >
            <component :is="link.icon" class="h-3.5 w-3.5 transition-colors duration-200" />
            {{ link.label }}
            <!-- active underline -->
            <span class="nav-underline absolute bottom-0 left-3 right-3 h-0.5 rounded-full bg-primary-600 opacity-0 transition-all duration-200" />
          </RouterLink>
        </nav>
      </div>

      <!-- Right side -->
      <div class="flex items-center gap-1.5">
        <template v-if="!auth.state.user">
          <RouterLink
            to="/login"
            class="hidden rounded-md px-4 py-2 text-sm font-medium text-neutral-600 transition-colors duration-200 hover:bg-neutral-100 sm:block"
          >登录</RouterLink>
          <RouterLink
            to="/register"
            class="rounded-md bg-primary-600 px-4 py-2 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500"
          >注册</RouterLink>
        </template>

        <template v-else>
          <!-- Bell -->
          <RouterLink
            to="/messages"
            class="relative min-h-[40px] min-w-[40px] grid place-items-center rounded-md text-neutral-500 transition-colors duration-200 hover:bg-neutral-100 hover:text-neutral-700"
          >
            <Bell class="h-5 w-5" />
            <span
              v-if="unread && unread > 0"
              class="absolute right-1 top-1 flex h-4 w-4 items-center justify-center rounded-full bg-danger text-[10px] font-bold text-white"
            >{{ unread > 99 ? '99+' : unread }}</span>
          </RouterLink>

          <!-- Admin: primary-50 background (NOT amber/orange) -->
          <RouterLink
            v-if="auth.state.user.role === 'ADMIN'"
            to="/admin"
            class="hidden items-center gap-1.5 rounded-md bg-primary-50 px-3 py-2 text-sm font-semibold text-primary-600 ring-1 ring-primary-200 transition-all duration-200 hover:bg-primary-100 sm:flex"
          >
            <ShieldCheck class="h-4 w-4" />管理后台
          </RouterLink>

          <!-- Teacher: plain text link (no colored pill) -->
          <RouterLink
            v-else-if="auth.state.user.role === 'TEACHER'"
            to="/teacher/courses"
            class="hidden items-center gap-1.5 rounded-md px-3 py-2 text-sm font-medium text-neutral-600 transition-colors duration-200 hover:bg-neutral-100 hover:text-neutral-900 sm:flex"
          >
            <BookOpen class="h-4 w-4" />教学工作台
          </RouterLink>

          <!-- Student -->
          <RouterLink
            v-else
            to="/dashboard"
            class="hidden items-center gap-1.5 rounded-md px-3 py-2 text-sm font-medium text-neutral-600 transition-colors duration-200 hover:bg-neutral-100 hover:text-neutral-900 sm:flex"
          >
            <LayoutDashboard class="h-4 w-4" />工作台
          </RouterLink>

          <!-- Avatar -->
          <RouterLink to="/profile" class="flex items-center gap-2 rounded-md px-2 py-1.5 transition-colors duration-200 hover:bg-neutral-100">
            <img
              v-if="auth.state.user.avatar"
              :src="auth.state.user.avatar"
              :alt="auth.state.user.username"
              class="h-8 w-8 rounded-full object-cover ring-2 ring-neutral-200"
              loading="lazy"
            />
            <div
              v-else
              class="grid h-8 w-8 place-items-center rounded-full bg-primary-100 text-sm font-semibold text-primary-700 ring-2 ring-neutral-200"
            >
              {{ auth.state.user.username?.slice(0, 1).toUpperCase() }}
            </div>
            <span class="hidden text-sm font-medium text-neutral-700 sm:block">{{ auth.state.user.username }}</span>
          </RouterLink>

          <!-- Logout -->
          <button
            class="min-h-[40px] min-w-[40px] grid place-items-center rounded-md text-neutral-400 transition-colors duration-200 hover:bg-neutral-100 hover:text-neutral-600"
            @click="logout"
          >
            <LogOut class="h-4 w-4" />
          </button>
        </template>

        <!-- Mobile hamburger -->
        <button
          class="min-h-[40px] min-w-[40px] grid place-items-center rounded-md text-neutral-500 transition-colors duration-200 hover:bg-neutral-100 md:hidden"
          @click="menuOpen = !menuOpen"
        >
          <Menu v-if="!menuOpen" class="h-5 w-5" />
          <X v-else class="h-5 w-5" />
        </button>
      </div>
    </div>

    <!-- Mobile menu -->
    <div v-if="menuOpen" class="border-t border-border bg-white px-4 pb-4 md:hidden">
      <nav class="flex flex-col gap-0.5 pt-2">
        <RouterLink
          v-for="link in navLinks"
          :key="link.to"
          :to="link.to"
          class="rounded-md px-3 py-2.5 text-sm font-medium text-neutral-700 transition-colors duration-200 hover:bg-neutral-100"
          @click="menuOpen = false"
        >{{ link.label }}</RouterLink>

        <template v-if="auth.state.user">
          <RouterLink
            v-if="auth.state.user.role === 'ADMIN'"
            to="/admin"
            class="flex items-center gap-2 rounded-md bg-primary-50 px-3 py-2.5 text-sm font-semibold text-primary-600"
            @click="menuOpen = false"
          >
            <ShieldCheck class="h-4 w-4" />管理后台
          </RouterLink>
          <RouterLink
            v-else-if="auth.state.user.role === 'TEACHER'"
            to="/teacher/courses"
            class="flex items-center gap-2 rounded-md px-3 py-2.5 text-sm font-medium text-neutral-700 transition-colors hover:bg-neutral-100"
            @click="menuOpen = false"
          >
            <BookOpen class="h-4 w-4" />教学工作台
          </RouterLink>
          <RouterLink
            v-else
            to="/dashboard"
            class="flex items-center gap-2 rounded-md px-3 py-2.5 text-sm font-medium text-neutral-700 transition-colors hover:bg-neutral-100"
            @click="menuOpen = false"
          >
            <LayoutDashboard class="h-4 w-4" />工作台
          </RouterLink>
          <RouterLink
            to="/profile"
            class="flex items-center gap-2 rounded-md px-3 py-2.5 text-sm font-medium text-neutral-700 transition-colors hover:bg-neutral-100"
            @click="menuOpen = false"
          >
            <UserRound class="h-4 w-4" />个人中心
          </RouterLink>
          <button
            class="flex w-full items-center gap-2 rounded-md px-3 py-2.5 text-sm font-medium text-danger transition-colors hover:bg-danger-bg"
            @click="logout"
          >
            <LogOut class="h-4 w-4" />退出登录
          </button>
        </template>

        <template v-else>
          <RouterLink
            to="/login"
            class="rounded-md px-3 py-2.5 text-sm font-medium text-neutral-700 transition-colors hover:bg-neutral-100"
            @click="menuOpen = false"
          >登录</RouterLink>
          <RouterLink
            to="/register"
            class="rounded-md bg-primary-600 px-3 py-2.5 text-center text-sm font-semibold text-white"
            @click="menuOpen = false"
          >注册</RouterLink>
        </template>
      </nav>
    </div>
  </header>
</template>
