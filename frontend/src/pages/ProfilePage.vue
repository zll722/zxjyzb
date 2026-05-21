<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Camera, ChevronLeft, IdCard, KeyRound, Save, UserRound } from 'lucide-vue-next'
import { RouterLink } from 'vue-router'
import { authApi, type TeacherProfile } from '@/lib/api'
import { useAuth } from '@/composables/useAuth'
import AppLayout from '@/components/layout/AppLayout.vue'

const auth            = useAuth()
const loading         = ref(false)
const passwordLoading = ref(false)
const avatarLoading   = ref(false)
const teacherLoading  = ref(false)
const message         = ref('')
const error           = ref('')
const avatarInput     = ref<HTMLInputElement | null>(null)
const teacherProfile  = ref<TeacherProfile | null>(null)

const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const teacherForm  = reactive({ bio: '', teachingYears: 0, expertise: '' })

const isTeacher  = computed(() => auth.state.user?.role === 'TEACHER')
const avatarUrl  = computed(() => auth.state.user?.avatar || '')

const inputCls = 'h-10 w-full rounded-md border border-border bg-white px-3 text-sm text-neutral-900 outline-none transition-all duration-200 placeholder:text-neutral-400 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15'

function clearTips() { message.value = ''; error.value = '' }

async function loadProfile() {
  loading.value = true; clearTips()
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
  if (passwordForm.newPassword !== passwordForm.confirmPassword) { error.value = '两次输入的新密码不一致'; return }
  passwordLoading.value = true
  try {
    await authApi.changePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    passwordForm.oldPassword = ''; passwordForm.newPassword = ''; passwordForm.confirmPassword = ''
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
  clearTips(); avatarLoading.value = true
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
  clearTips(); teacherLoading.value = true
  try {
    teacherProfile.value = await authApi.updateTeacherProfile({ bio: teacherForm.bio, teachingYears: teacherForm.teachingYears, expertise: teacherForm.expertise })
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
  <AppLayout>
    <div class="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">

      <!-- Breadcrumb -->
      <RouterLink to="/dashboard" class="mb-6 inline-flex items-center gap-1 text-sm text-neutral-500 transition-colors hover:text-primary-600">
        <ChevronLeft class="h-4 w-4" />返回工作台
      </RouterLink>

      <div class="mb-6">
        <h1 class="text-2xl font-semibold text-neutral-900">个人中心</h1>
        <p class="mt-1 text-sm text-neutral-500">账号资料、安全设置与教师公开资料管理。</p>
      </div>

      <!-- Status messages -->
      <div v-if="loading" class="mb-4 rounded-md bg-neutral-100 px-4 py-3 text-sm text-neutral-500">资料加载中…</div>
      <div v-if="message" class="mb-4 rounded-md bg-success-bg px-4 py-3 text-sm text-success">{{ message }}</div>
      <div v-if="error"   class="mb-4 rounded-md bg-danger-bg  px-4 py-3 text-sm text-danger">{{ error }}</div>

      <div class="grid gap-6 lg:grid-cols-[0.85fr_1.15fr]">
        <!-- Left: Avatar + basic info -->
        <div class="rounded-xl border border-border bg-white p-5 shadow-sm">
          <div class="flex flex-col items-center gap-3 text-center">
            <div class="relative">
              <div class="flex h-20 w-20 items-center justify-center overflow-hidden rounded-full border-2 border-border bg-neutral-100">
                <img v-if="avatarUrl" :src="avatarUrl" class="h-full w-full object-cover" alt="用户头像" loading="lazy" />
                <UserRound v-else class="h-10 w-10 text-neutral-400" />
              </div>
            </div>
            <div>
              <h2 class="text-lg font-semibold text-neutral-900">{{ auth.state.user?.username || '-' }}</h2>
              <p class="text-sm text-neutral-500">{{ auth.state.user?.email || '-' }}</p>
              <span class="mt-1.5 inline-flex rounded-full bg-primary-50 px-2.5 py-0.5 text-xs font-medium text-primary-700">
                {{ auth.state.user?.role === 'STUDENT' ? '学生' : auth.state.user?.role === 'TEACHER' ? '教师' : '管理员' }}
              </span>
            </div>
          </div>

          <div class="mt-5 space-y-2 text-sm">
            <div class="flex items-center justify-between rounded-md bg-neutral-50 px-3 py-2.5">
              <span class="text-neutral-500">账号 ID</span>
              <span class="font-medium text-neutral-800">{{ auth.state.user?.id || '-' }}</span>
            </div>
            <div class="flex items-center justify-between rounded-md bg-neutral-50 px-3 py-2.5">
              <span class="text-neutral-500">账号状态</span>
              <span class="font-medium" :class="auth.state.user?.status === 1 ? 'text-success' : 'text-danger'">
                {{ auth.state.user?.status === 1 ? '正常' : '禁用' }}
              </span>
            </div>
          </div>

          <label
            for="profile-avatar"
            class="mt-5 flex min-h-[40px] cursor-pointer items-center justify-center gap-2 rounded-md border border-border px-4 text-sm font-medium text-neutral-700 transition-all duration-200 hover:bg-neutral-100"
          >
            <Camera class="h-4 w-4" />
            {{ avatarLoading ? '上传中…' : '更换头像' }}
            <input id="profile-avatar" ref="avatarInput" name="avatar" type="file" accept="image/jpeg,image/png,image/webp" class="hidden" :disabled="avatarLoading" @change="uploadAvatar" />
          </label>
        </div>

        <!-- Right: Change password -->
        <form class="rounded-xl border border-border bg-white p-5 shadow-sm" @submit.prevent="changePassword">
          <div class="mb-5 flex items-center gap-2">
            <KeyRound class="h-5 w-5 text-primary-600" />
            <h2 class="font-semibold text-neutral-900">修改密码</h2>
          </div>
          <div class="space-y-3">
            <div>
              <label for="profile-old-password" class="block text-sm font-medium text-neutral-700">原密码</label>
              <input id="profile-old-password" v-model="passwordForm.oldPassword" name="oldPassword" autocomplete="current-password" type="password" required minlength="6" placeholder="请输入原密码" :class="'mt-1.5 ' + inputCls" />
            </div>
            <div>
              <label for="profile-new-password" class="block text-sm font-medium text-neutral-700">新密码</label>
              <input id="profile-new-password" v-model="passwordForm.newPassword" name="newPassword" autocomplete="new-password" type="password" required minlength="6" placeholder="至少 6 个字符" :class="'mt-1.5 ' + inputCls" />
            </div>
            <div>
              <label for="profile-confirm-password" class="block text-sm font-medium text-neutral-700">确认新密码</label>
              <input id="profile-confirm-password" v-model="passwordForm.confirmPassword" name="confirmPassword" autocomplete="new-password" type="password" required minlength="6" placeholder="再次输入新密码" :class="'mt-1.5 ' + inputCls" />
            </div>
          </div>
          <button
            type="submit"
            :disabled="passwordLoading"
            class="mt-5 inline-flex min-h-[40px] items-center gap-2 rounded-md bg-primary-600 px-5 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500 disabled:opacity-60"
          >
            <Save class="h-4 w-4" />{{ passwordLoading ? '保存中…' : '保存新密码' }}
          </button>
        </form>
      </div>

      <!-- Teacher profile -->
      <form v-if="isTeacher" class="mt-6 rounded-xl border border-border bg-white p-5 shadow-sm" @submit.prevent="saveTeacherProfile">
        <div class="mb-5 flex items-center gap-2">
          <IdCard class="h-5 w-5 text-primary-600" />
          <h2 class="font-semibold text-neutral-900">教师公开资料</h2>
        </div>
        <div class="grid gap-4 lg:grid-cols-2">
          <div>
            <label for="teacher-years" class="block text-sm font-medium text-neutral-700">教龄（年）</label>
            <input id="teacher-years" v-model.number="teacherForm.teachingYears" name="teachingYears" type="number" min="0" :class="'mt-1.5 ' + inputCls" />
          </div>
          <div>
            <label for="teacher-expertise" class="block text-sm font-medium text-neutral-700">擅长方向</label>
            <input id="teacher-expertise" v-model="teacherForm.expertise" name="expertise" maxlength="255" placeholder="例如：Java 后端、Vue 前端" :class="'mt-1.5 ' + inputCls" />
          </div>
          <div class="lg:col-span-2">
            <label for="teacher-bio" class="block text-sm font-medium text-neutral-700">个人简介</label>
            <textarea
              id="teacher-bio"
              v-model="teacherForm.bio"
              name="bio"
              rows="4"
              placeholder="介绍教学经历、课程方向与授课特色"
              class="mt-1.5 w-full rounded-md border border-border bg-white px-3 py-2.5 text-sm text-neutral-900 outline-none transition-all duration-200 placeholder:text-neutral-400 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15"
            />
          </div>
        </div>
        <button
          type="submit"
          :disabled="teacherLoading"
          class="mt-5 inline-flex min-h-[40px] items-center gap-2 rounded-md bg-primary-600 px-5 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500 disabled:opacity-60"
        >
          <Save class="h-4 w-4" />{{ teacherLoading ? '保存中…' : '保存教师资料' }}
        </button>
      </form>
    </div>
  </AppLayout>
</template>
