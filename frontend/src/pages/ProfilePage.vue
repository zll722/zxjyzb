<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Camera, IdCard, KeyRound, Save, UserRound } from 'lucide-vue-next'
import { authApi, type TeacherProfile } from '@/lib/api'
import { useAuth } from '@/composables/useAuth'

const auth = useAuth()
const loading = ref(false)
const passwordLoading = ref(false)
const avatarLoading = ref(false)
const teacherLoading = ref(false)
const message = ref('')
const error = ref('')
const avatarInput = ref<HTMLInputElement | null>(null)
const teacherProfile = ref<TeacherProfile | null>(null)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const teacherForm = reactive({
  bio: '',
  teachingYears: 0,
  expertise: '',
})

const isTeacher = computed(() => auth.state.user?.role === 'TEACHER')
const avatarUrl = computed(() => auth.state.user?.avatar || '')

function clearTips() {
  message.value = ''
  error.value = ''
}

async function loadProfile() {
  loading.value = true
  clearTips()
  try {
    await auth.refreshUser()
    if (isTeacher.value) {
      const profile = await authApi.teacherProfile()
      teacherProfile.value = profile
      teacherForm.bio = profile.bio || ''
      teacherForm.teachingYears = profile.teachingYears ?? 0
      teacherForm.expertise = profile.expertise || ''
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '资料加载失败'
  } finally {
    loading.value = false
  }
}

async function changePassword() {
  clearTips()
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    error.value = '两次输入的新密码不一致'
    return
  }
  passwordLoading.value = true
  try {
    await authApi.changePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    message.value = '密码修改成功'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '密码修改失败'
  } finally {
    passwordLoading.value = false
  }
}

async function uploadAvatar(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  clearTips()
  avatarLoading.value = true
  try {
    const avatar = await authApi.uploadAvatar(file)
    auth.setUserAvatar(avatar)
    message.value = '头像上传成功'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '头像上传失败'
  } finally {
    avatarLoading.value = false
    if (avatarInput.value) avatarInput.value.value = ''
  }
}

async function saveTeacherProfile() {
  clearTips()
  teacherLoading.value = true
  try {
    teacherProfile.value = await authApi.updateTeacherProfile({
      bio: teacherForm.bio,
      teachingYears: teacherForm.teachingYears,
      expertise: teacherForm.expertise,
    })
    message.value = '教师资料已保存'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '教师资料保存失败'
  } finally {
    teacherLoading.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-6xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/dashboard" class="text-sm text-cyan-200">返回工作台</RouterLink>
        <RouterLink to="/" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">返回首页</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <div class="flex flex-wrap items-center justify-between gap-6">
          <div>
            <p class="text-sm text-cyan-200">个人中心</p>
            <h1 class="mt-2 text-4xl font-black">账号资料与安全设置</h1>
            <p class="mt-3 max-w-3xl text-slate-300">查看当前账号信息，维护登录密码和头像；教师可维护公开教学资料。</p>
          </div>
          <div class="rounded-[2rem] border border-cyan-300/30 bg-cyan-300/10 p-5 text-cyan-100">
            <UserRound class="h-10 w-10" />
          </div>
        </div>
        <p v-if="loading" class="mt-6 rounded-2xl bg-white/8 px-4 py-3 text-sm text-slate-300">资料加载中...</p>
        <p v-if="message" class="mt-6 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <p v-if="error" class="mt-6 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <section class="mt-8 grid gap-6 lg:grid-cols-[0.85fr_1.15fr]">
        <div class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div class="flex items-center gap-4">
            <div class="flex h-24 w-24 items-center justify-center overflow-hidden rounded-full border border-white/10 bg-slate-900">
              <img v-if="avatarUrl" :src="avatarUrl" class="h-full w-full object-cover" alt="用户头像" />
              <UserRound v-else class="h-10 w-10 text-slate-500" />
            </div>
            <div>
              <h2 class="text-2xl font-black">{{ auth.state.user?.username || '-' }}</h2>
              <p class="mt-1 text-sm text-slate-300">{{ auth.state.user?.email || '-' }}</p>
              <span class="mt-3 inline-flex rounded-full bg-cyan-300/15 px-3 py-1 text-xs font-bold text-cyan-100">{{ auth.state.user?.role || '-' }}</span>
            </div>
          </div>

          <div class="mt-6 grid gap-3 text-sm text-slate-300">
            <div class="flex items-center justify-between rounded-2xl bg-slate-900/70 px-4 py-3">
              <span>账号 ID</span>
              <span class="text-white">{{ auth.state.user?.id || '-' }}</span>
            </div>
            <div class="flex items-center justify-between rounded-2xl bg-slate-900/70 px-4 py-3">
              <span>账号状态</span>
              <span class="text-white">{{ auth.state.user?.status === 1 ? '正常' : '禁用' }}</span>
            </div>
          </div>

          <label class="mt-6 inline-flex cursor-pointer items-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">
            <Camera class="h-4 w-4" />
            {{ avatarLoading ? '上传中...' : '上传头像' }}
            <input ref="avatarInput" type="file" accept="image/jpeg,image/png,image/webp" class="hidden" :disabled="avatarLoading" @change="uploadAvatar" />
          </label>
        </div>

        <form class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur" @submit.prevent="changePassword">
          <div class="flex items-center gap-3">
            <KeyRound class="h-6 w-6 text-cyan-200" />
            <h2 class="text-2xl font-black">修改密码</h2>
          </div>
          <div class="mt-6 grid gap-4">
            <input v-model="passwordForm.oldPassword" type="password" required minlength="6" placeholder="原密码" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
            <input v-model="passwordForm.newPassword" type="password" required minlength="6" placeholder="新密码" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
            <input v-model="passwordForm.confirmPassword" type="password" required minlength="6" placeholder="确认新密码" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" />
          </div>
          <button :disabled="passwordLoading" class="mt-6 inline-flex items-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200 disabled:opacity-50">
            <Save class="h-4 w-4" />{{ passwordLoading ? '保存中...' : '保存新密码' }}
          </button>
        </form>
      </section>

      <form v-if="isTeacher" class="mt-8 rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur" @submit.prevent="saveTeacherProfile">
        <div class="flex items-center gap-3">
          <IdCard class="h-6 w-6 text-cyan-200" />
          <h2 class="text-2xl font-black">教师资料</h2>
        </div>
        <div class="mt-6 grid gap-4 lg:grid-cols-2">
          <label class="grid gap-2 text-sm text-slate-300">
            教龄
            <input v-model.number="teacherForm.teachingYears" type="number" min="0" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 text-white outline-none focus:border-cyan-300" />
          </label>
          <label class="grid gap-2 text-sm text-slate-300">
            擅长方向
            <input v-model="teacherForm.expertise" maxlength="255" placeholder="例如：Java 后端、Vue 前端、直播教学" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 text-white outline-none focus:border-cyan-300" />
          </label>
          <label class="grid gap-2 text-sm text-slate-300 lg:col-span-2">
            个人简介
            <textarea v-model="teacherForm.bio" rows="5" placeholder="介绍教学经历、课程方向与授课特色" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 text-white outline-none focus:border-cyan-300"></textarea>
          </label>
        </div>
        <button :disabled="teacherLoading" class="mt-6 inline-flex items-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200 disabled:opacity-50">
          <Save class="h-4 w-4" />{{ teacherLoading ? '保存中...' : '保存教师资料' }}
        </button>
      </form>
    </div>
  </main>
</template>
