<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const auth = useAuth()
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const email = ref('')
const role = ref('STUDENT')
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  if (password.value !== confirmPassword.value) {
    error.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  try {
    await auth.register({ username: username.value, password: password.value, email: email.value, role: role.value })
    router.push('/dashboard')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="grid min-h-screen place-items-center bg-[#07111f] px-6 text-white">
    <form class="w-full max-w-md rounded-[2rem] border border-white/10 bg-white/10 p-8 shadow-2xl backdrop-blur" @submit.prevent="submit">
      <p class="text-sm font-medium text-cyan-200">创建账号</p>
      <h1 class="mt-2 text-3xl font-black">加入 EduLive</h1>
      <label class="mt-8 block text-sm text-slate-300">用户名</label>
      <input v-model="username" required minlength="3" class="mt-2 w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none transition focus:border-cyan-300" />
      <label class="mt-5 block text-sm text-slate-300">邮箱</label>
      <input v-model="email" required type="email" class="mt-2 w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none transition focus:border-cyan-300" />
      <label class="mt-5 block text-sm text-slate-300">密码</label>
      <input v-model="password" required minlength="6" type="password" class="mt-2 w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none transition focus:border-cyan-300" />
      <label class="mt-5 block text-sm text-slate-300">确认密码</label>
      <input v-model="confirmPassword" required minlength="6" type="password" class="mt-2 w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none transition focus:border-cyan-300" :class="confirmPassword && confirmPassword !== password ? 'border-red-400' : ''" />
      <p v-if="confirmPassword && confirmPassword !== password" class="mt-1 text-xs text-red-300">两次密码不一致</p>
      <label class="mt-5 block text-sm text-slate-300">身份</label>
      <select v-model="role" class="mt-2 w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none transition focus:border-cyan-300">
        <option value="STUDENT">学生</option>
        <option value="TEACHER">教师</option>
      </select>
      <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-200">{{ error }}</p>
      <button :disabled="loading" class="mt-8 w-full rounded-2xl bg-cyan-300 px-4 py-3 font-bold text-slate-950 transition hover:bg-cyan-200 disabled:cursor-not-allowed disabled:opacity-60">
        {{ loading ? '注册中...' : '注册并进入' }}
      </button>
      <p class="mt-6 text-center text-sm text-slate-300">
        已有账号？
        <RouterLink to="/login" class="font-semibold text-cyan-200 hover:text-cyan-100">去登录</RouterLink>
      </p>
    </form>
  </main>
</template>
