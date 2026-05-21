<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { GraduationCap, Video } from 'lucide-vue-next'
import { useAuth } from '@/composables/useAuth'

const router          = useRouter()
const auth            = useAuth()
const username        = ref('')
const password        = ref('')
const confirmPassword = ref('')
const email           = ref('')
const role            = ref('STUDENT')
const error           = ref('')
const loading         = ref(false)

async function submit() {
  error.value = ''
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (username.value.trim().length < 3)            { error.value = '用户名至少需要 3 个字符'; return }
  if (!emailPattern.test(email.value))              { error.value = '请输入有效的邮箱地址'; return }
  if (password.value.length < 6)                   { error.value = '密码至少需要 6 个字符'; return }
  if (password.value !== confirmPassword.value)    { error.value = '两次输入的密码不一致'; return }
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

const inputCls = 'mt-1.5 h-10 w-full rounded-md border border-border bg-white px-3 text-sm text-neutral-900 outline-none transition-all duration-200 placeholder:text-neutral-400 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15'
</script>

<template>
  <div class="grid min-h-screen lg:grid-cols-2">

    <!-- Left brand panel -->
    <div class="hidden flex-col justify-between bg-primary-600 p-10 text-white lg:flex">
      <RouterLink to="/" class="flex items-center gap-2.5">
        <div class="grid h-9 w-9 place-items-center rounded-lg bg-white/20">
          <Video class="h-4 w-4" />
        </div>
        <span class="text-lg font-semibold">EduLive</span>
      </RouterLink>

      <div>
        <div class="mb-8 grid h-16 w-16 place-items-center rounded-2xl bg-white/15">
          <GraduationCap class="h-8 w-8" stroke-width="1.5" />
        </div>
        <h2 class="text-3xl font-semibold leading-snug">加入我们<br />开始您的学习之旅</h2>
        <p class="mt-4 text-base leading-relaxed text-white/70">
          无论您是学生还是教师，EduLive 为您提供完整的在线教育生态，让学习和教学都变得更高效。
        </p>
        <div class="mt-10 grid grid-cols-2 gap-4">
          <div class="rounded-xl bg-white/10 p-4">
            <p class="text-sm text-white/60">学生</p>
            <p class="mt-1 text-sm text-white">选课、直播、完成作业与考试</p>
          </div>
          <div class="rounded-xl bg-white/10 p-4">
            <p class="text-sm text-white/60">教师</p>
            <p class="mt-1 text-sm text-white">发布课程、直播授课、批改作业</p>
          </div>
        </div>
      </div>

      <p class="text-sm text-white/40">© 2025 EduLive · 让学习更高效，让知识有价值。</p>
    </div>

    <!-- Right form panel -->
    <div class="flex flex-col justify-center bg-neutral-50 px-6 py-12 sm:px-12 lg:px-16">
      <RouterLink to="/" class="mb-10 flex items-center gap-2 lg:hidden">
        <div class="grid h-8 w-8 place-items-center rounded-lg bg-primary-600 text-white">
          <Video class="h-4 w-4" />
        </div>
        <span class="font-semibold text-neutral-900">EduLive</span>
      </RouterLink>

      <div class="mx-auto w-full max-w-sm">
        <h1 class="text-2xl font-semibold text-neutral-900">创建账号</h1>
        <p class="mt-1.5 text-sm text-neutral-500">填写以下信息完成注册</p>

        <form class="mt-8 space-y-4" @submit.prevent="submit">
          <div>
            <label for="register-username" class="block text-sm font-medium text-neutral-700">用户名</label>
            <input id="register-username" v-model="username" name="username" autocomplete="username" placeholder="至少 3 个字符" :class="inputCls" />
          </div>
          <div>
            <label for="register-email" class="block text-sm font-medium text-neutral-700">邮箱</label>
            <input id="register-email" v-model="email" name="email" type="email" autocomplete="email" placeholder="your@email.com" :class="inputCls" />
          </div>
          <div>
            <label for="register-password" class="block text-sm font-medium text-neutral-700">密码</label>
            <input id="register-password" v-model="password" name="password" type="password" autocomplete="new-password" placeholder="至少 6 个字符" :class="inputCls" />
          </div>
          <div>
            <label for="register-confirm" class="block text-sm font-medium text-neutral-700">确认密码</label>
            <input
              id="register-confirm"
              v-model="confirmPassword"
              name="confirmPassword"
              type="password"
              autocomplete="new-password"
              placeholder="再次输入密码"
              :class="[
                'mt-1.5 h-10 w-full rounded-md border bg-white px-3 text-sm text-neutral-900 outline-none transition-all duration-200 placeholder:text-neutral-400',
                confirmPassword && confirmPassword !== password
                  ? 'border-danger focus:ring-2 focus:ring-danger/20'
                  : 'border-border focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15',
              ]"
            />
            <p v-if="confirmPassword && confirmPassword !== password" class="mt-1 text-xs text-danger">两次密码不一致</p>
          </div>
          <div>
            <label for="register-role" class="block text-sm font-medium text-neutral-700">注册身份</label>
            <select id="register-role" v-model="role" name="role" :class="inputCls">
              <option value="STUDENT">学生</option>
              <option value="TEACHER">教师</option>
            </select>
          </div>

          <div v-if="error" class="rounded-md bg-danger-bg px-4 py-3 text-sm text-danger">{{ error }}</div>

          <button
            type="submit"
            :disabled="loading"
            class="h-10 w-full rounded-md bg-primary-600 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <span v-if="loading" class="inline-flex items-center justify-center gap-2">
              <span class="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />注册中…
            </span>
            <span v-else>注册并进入</span>
          </button>
        </form>

        <p class="mt-6 text-center text-sm text-neutral-500">
          已有账号？
          <RouterLink to="/login" class="font-semibold text-primary-600 transition-colors hover:text-primary-500">去登录</RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>
