<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const auth = useAuth()
const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    router.push('/dashboard')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="grid min-h-screen place-items-center bg-slate-950 px-6 text-white">
    <form class="w-full max-w-md rounded-[2rem] border border-white/10 bg-white/10 p-8 shadow-2xl backdrop-blur" @submit.prevent="submit">
      <p class="text-sm font-medium text-cyan-200">欢迎回来</p>
      <h1 class="mt-2 text-3xl font-black">登录 EduLive</h1>
      <label class="mt-8 block text-sm text-slate-300">用户名</label>
      <input v-model="username" required class="mt-2 w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none transition focus:border-cyan-300" placeholder="请输入用户名" />
      <label class="mt-5 block text-sm text-slate-300">密码</label>
      <input v-model="password" required type="password" class="mt-2 w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none transition focus:border-cyan-300" placeholder="请输入密码" />
      <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-200">{{ error }}</p>
      <button :disabled="loading" class="mt-8 w-full rounded-2xl bg-cyan-300 px-4 py-3 font-bold text-slate-950 transition hover:bg-cyan-200 disabled:cursor-not-allowed disabled:opacity-60">
        {{ loading ? '登录中...' : '登录' }}
      </button>
      <p class="mt-6 text-center text-sm text-slate-300">
        还没有账号？
        <RouterLink to="/register" class="font-semibold text-cyan-200 hover:text-cyan-100">立即注册</RouterLink>
      </p>
    </form>
  </main>
</template>
