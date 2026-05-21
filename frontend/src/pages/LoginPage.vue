<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { BookOpen, Video } from 'lucide-vue-next'
import { useAuth } from '@/composables/useAuth'

const route    = useRoute()
const router   = useRouter()
const auth     = useAuth()
const username = ref('')
const password = ref('')
const error    = ref('')
const loading  = ref(false)

async function submit() {
  error.value = ''
  if (!username.value.trim()) { error.value = '请输入用户名'; return }
  if (!password.value)        { error.value = '请输入密码'; return }
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.push(redirect)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="grid min-h-screen lg:grid-cols-2">

    <!-- Left brand panel — primary-600 bg -->
    <div class="hidden flex-col justify-between bg-primary-600 p-10 text-white lg:flex">
      <RouterLink to="/" class="flex items-center gap-2.5">
        <div class="grid h-9 w-9 place-items-center rounded-lg bg-white/20 backdrop-blur-sm">
          <Video class="h-4 w-4" />
        </div>
        <span class="text-lg font-semibold">EduLive</span>
      </RouterLink>

      <div>
        <div class="mb-8 grid h-16 w-16 place-items-center rounded-2xl bg-white/15">
          <BookOpen class="h-8 w-8" stroke-width="1.5" />
        </div>
        <h2 class="text-3xl font-semibold leading-snug">探索知识<br />开启直播学习新体验</h2>
        <p class="mt-4 text-base leading-relaxed text-white/70">
          优质课程与实时互动直播结合，随时随地学习，与优秀的人一起进步。
        </p>
        <div class="mt-10 flex gap-8 text-sm text-white/60">
          <div><span class="block text-2xl font-semibold text-white">500+</span>精品课程</div>
          <div><span class="block text-2xl font-semibold text-white">200+</span>直播讲师</div>
          <div><span class="block text-2xl font-semibold text-white">10K+</span>学习用户</div>
        </div>
      </div>

      <p class="text-sm text-white/40">© 2025 EduLive · 让学习更高效，让知识有价值。</p>
    </div>

    <!-- Right form panel -->
    <div class="flex flex-col justify-center bg-neutral-50 px-6 py-12 sm:px-12 lg:px-16">
      <!-- Mobile logo -->
      <RouterLink to="/" class="mb-10 flex items-center gap-2 lg:hidden">
        <div class="grid h-8 w-8 place-items-center rounded-lg bg-primary-600 text-white">
          <Video class="h-4 w-4" />
        </div>
        <span class="font-semibold text-neutral-900">EduLive</span>
      </RouterLink>

      <div class="mx-auto w-full max-w-sm">
        <h1 class="text-2xl font-semibold text-neutral-900">欢迎回来</h1>
        <p class="mt-1.5 text-sm text-neutral-500">登录您的账户继续学习</p>

        <form class="mt-8 space-y-5" @submit.prevent="submit">
          <div>
            <label for="login-username" class="block text-sm font-medium text-neutral-700">用户名</label>
            <input
              id="login-username"
              v-model="username"
              name="username"
              autocomplete="username"
              placeholder="请输入用户名"
              class="mt-1.5 h-10 w-full rounded-md border border-border bg-white px-3 text-sm text-neutral-900 outline-none transition-all duration-200 placeholder:text-neutral-400 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15"
            />
          </div>
          <div>
            <label for="login-password" class="block text-sm font-medium text-neutral-700">密码</label>
            <input
              id="login-password"
              v-model="password"
              name="password"
              autocomplete="current-password"
              type="password"
              placeholder="请输入密码"
              class="mt-1.5 h-10 w-full rounded-md border border-border bg-white px-3 text-sm text-neutral-900 outline-none transition-all duration-200 placeholder:text-neutral-400 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15"
            />
          </div>

          <div v-if="error" class="rounded-md bg-danger-bg px-4 py-3 text-sm text-danger">
            {{ error }}
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="h-10 w-full rounded-md bg-primary-600 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <span v-if="loading" class="inline-flex items-center justify-center gap-2">
              <span class="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
              登录中…
            </span>
            <span v-else>登录</span>
          </button>
        </form>

        <p class="mt-6 text-center text-sm text-neutral-500">
          还没有账号？
          <RouterLink to="/register" class="font-semibold text-primary-600 transition-colors hover:text-primary-500">
            立即注册
          </RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>
