<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { KeyRound, Plus, RotateCw, Search, Trash2, UserCheck, UserX } from 'lucide-vue-next'
import { adminApi, type AdminUser, type PageResult, type UserRole } from '@/lib/api'

const filters = reactive<{ role: UserRole | ''; status: number | ''; keyword: string }>({ role: '', status: '', keyword: '' })
const page = ref<PageResult<AdminUser>>({ records: [], total: 0, size: 10, current: 1 })
const selected = ref<AdminUser | null>(null)
const profileForm = reactive({ bio: '', teachingYears: 0, expertise: '' })
const createForm = reactive({ role: 'TEACHER' as 'TEACHER' | 'ADMIN', username: '', password: '', email: '', bio: '', teachingYears: 0, expertise: '' })
const resetForm = reactive({ userId: 0, username: '', newPassword: '' })
const loading = ref(false)
const message = ref('')
const error = ref('')

const roleLabels: Record<UserRole, string> = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }

async function load(current = 1) {
  loading.value = true
  error.value = ''
  try {
    page.value = await adminApi.users({ current, size: page.value.size, ...filters })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '用户列表加载失败'
  } finally {
    loading.value = false
  }
}

async function createUser() {
  error.value = ''
  message.value = ''
  try {
    if (createForm.role === 'TEACHER') {
      await adminApi.createTeacher({
        username: createForm.username,
        password: createForm.password,
        email: createForm.email,
        bio: createForm.bio,
        teachingYears: createForm.teachingYears,
        expertise: createForm.expertise,
      })
      message.value = '教师账号已创建'
    } else {
      await adminApi.createAdmin({ username: createForm.username, password: createForm.password, email: createForm.email })
      message.value = '管理员账号已创建'
    }
    createForm.username = ''
    createForm.password = ''
    createForm.email = ''
    createForm.bio = ''
    createForm.teachingYears = 0
    createForm.expertise = ''
    await load(1)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '账号创建失败'
  }
}

async function toggleStatus(user: AdminUser) {
  error.value = ''
  message.value = ''
  try {
    await adminApi.updateUserStatus(user.id, user.status === 1 ? 0 : 1)
    message.value = user.status === 1 ? '用户已禁用' : '用户已启用'
    await load(page.value.current)
    if (selected.value?.id === user.id) selected.value = await adminApi.userDetail(user.id)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '用户状态更新失败'
  }
}

function openReset(user: AdminUser) {
  resetForm.userId = user.id
  resetForm.username = user.username
  resetForm.newPassword = ''
}

async function resetPassword() {
  if (!resetForm.userId) return
  error.value = ''
  message.value = ''
  try {
    await adminApi.resetUserPassword(resetForm.userId, { newPassword: resetForm.newPassword })
    message.value = '密码已重置'
    resetForm.userId = 0
    resetForm.username = ''
    resetForm.newPassword = ''
  } catch (err) {
    error.value = err instanceof Error ? err.message : '密码重置失败'
  }
}

async function deleteUser(user: AdminUser) {
  if (!window.confirm(`确认删除用户 ${user.username}？`)) return
  error.value = ''
  message.value = ''
  try {
    await adminApi.deleteUser(user.id)
    message.value = '用户已删除'
    if (selected.value?.id === user.id) selected.value = null
    if (resetForm.userId === user.id) resetForm.userId = 0
    await load(page.value.current)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '用户删除失败'
  }
}

async function selectUser(user: AdminUser) {
  error.value = ''
  try {
    selected.value = await adminApi.userDetail(user.id)
    profileForm.bio = selected.value.bio || ''
    profileForm.teachingYears = selected.value.teachingYears || 0
    profileForm.expertise = selected.value.expertise || ''
  } catch (err) {
    error.value = err instanceof Error ? err.message : '用户详情加载失败'
  }
}

async function saveProfile() {
  if (!selected.value) return
  error.value = ''
  message.value = ''
  try {
    selected.value = await adminApi.updateTeacherProfile(selected.value.id, profileForm)
    message.value = '教师资料已更新'
    await load(page.value.current)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '教师资料保存失败'
  }
}

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

onMounted(() => load())
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-8 text-white">
    <div class="mx-auto max-w-7xl">
      <nav class="mb-8 flex items-center justify-between">
        <RouterLink to="/admin" class="text-sm text-cyan-200">返回后台</RouterLink>
        <RouterLink to="/dashboard" class="rounded-full border border-white/10 px-5 py-2 text-sm hover:bg-white/10">工作台</RouterLink>
      </nav>

      <section class="rounded-[2rem] border border-white/10 bg-white/8 p-8 backdrop-blur">
        <p class="text-sm text-cyan-200">管理员用户管理</p>
        <h1 class="mt-2 text-4xl font-black">用户、状态与教师资料</h1>
        <form class="mt-8 grid gap-4 lg:grid-cols-[160px_160px_1fr_140px]" @submit.prevent="load(1)">
          <select v-model="filters.role" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
            <option value="">全部角色</option>
            <option value="ADMIN">管理员</option>
            <option value="TEACHER">教师</option>
            <option value="STUDENT">学生</option>
          </select>
          <select v-model="filters.status" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300">
            <option value="">全部状态</option>
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
          <input v-model="filters.keyword" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="用户名或邮箱" />
          <button class="inline-flex items-center justify-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">
            <Search class="h-4 w-4" />筛选
          </button>
        </form>
        <p v-if="message" class="mt-4 rounded-2xl bg-cyan-300/10 px-4 py-3 text-sm text-cyan-100">{{ message }}</p>
        <p v-if="error" class="mt-4 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-100">{{ error }}</p>
      </section>

      <section class="mt-8 grid gap-6 lg:grid-cols-[1fr_380px]">
        <div class="space-y-6">
          <form class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur" @submit.prevent="createUser">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p class="text-sm text-cyan-200">创建账号</p>
                <h2 class="mt-1 text-2xl font-black">教师与子管理员</h2>
              </div>
              <select v-model="createForm.role" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 text-sm outline-none focus:border-cyan-300">
                <option value="TEACHER">教师账号</option>
                <option value="ADMIN">管理员账号</option>
              </select>
            </div>
            <div class="mt-5 grid gap-3 md:grid-cols-3">
              <input v-model="createForm.username" required minlength="3" maxlength="50" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="用户名" />
              <input v-model="createForm.email" required type="email" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="邮箱" />
              <input v-model="createForm.password" required type="password" minlength="6" maxlength="50" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="初始密码" />
            </div>
            <div v-if="createForm.role === 'TEACHER'" class="mt-3 grid gap-3 md:grid-cols-[1fr_150px_1fr]">
              <input v-model="createForm.bio" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="教师简介" />
              <input v-model.number="createForm.teachingYears" type="number" min="0" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="教龄" />
              <input v-model="createForm.expertise" class="rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="擅长方向" />
            </div>
            <button class="mt-4 inline-flex items-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">
              <Plus class="h-4 w-4" />{{ createForm.role === 'TEACHER' ? '创建教师' : '创建管理员' }}
            </button>
          </form>

          <div v-if="resetForm.userId" class="rounded-[2rem] border border-cyan-300/20 bg-cyan-300/10 p-6 backdrop-blur">
            <p class="text-sm text-cyan-100">重置 {{ resetForm.username }} 的密码</p>
            <form class="mt-4 flex flex-col gap-3 md:flex-row" @submit.prevent="resetPassword">
              <input v-model="resetForm.newPassword" required type="password" minlength="6" maxlength="50" class="min-w-0 flex-1 rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="新密码" />
              <button class="inline-flex items-center justify-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">
                <KeyRound class="h-4 w-4" />确认重置
              </button>
            </form>
          </div>

          <div class="overflow-hidden rounded-[2rem] border border-white/10 bg-white/8 backdrop-blur">
            <div class="grid grid-cols-[1.2fr_1fr_120px_120px_240px] gap-4 border-b border-white/10 px-6 py-4 text-sm text-slate-400">
              <span>用户</span><span>邮箱</span><span>角色</span><span>状态</span><span>操作</span>
            </div>
            <p v-if="loading" class="p-6 text-slate-300">用户加载中...</p>
            <p v-else-if="!page.records.length" class="p-6 text-slate-300">暂无符合条件的用户</p>
            <article v-for="user in page.records" v-else :key="user.id" class="grid grid-cols-[1.2fr_1fr_120px_120px_240px] items-center gap-4 border-b border-white/10 px-6 py-4 last:border-b-0">
              <button class="text-left" @click="selectUser(user)">
                <p class="font-bold text-white">{{ user.username }}</p>
                <p class="text-xs text-slate-400">ID {{ user.id }} · {{ formatTime(user.createdAt) }}</p>
              </button>
              <p class="text-sm text-slate-300">{{ user.email }}</p>
              <span class="text-sm text-cyan-100">{{ roleLabels[user.role] }}</span>
              <span :class="['w-fit rounded-full px-3 py-1 text-xs font-bold', user.status === 1 ? 'bg-emerald-400/15 text-emerald-200' : 'bg-red-400/15 text-red-200']">{{ user.status === 1 ? '启用' : '禁用' }}</span>
              <div class="flex flex-wrap gap-2">
                <button class="rounded-full border border-white/10 px-3 py-1 text-xs hover:bg-white/10" @click="selectUser(user)">详情</button>
                <button class="inline-flex items-center gap-1 rounded-full border border-white/10 px-3 py-1 text-xs hover:bg-white/10" @click="toggleStatus(user)">
                  <component :is="user.status === 1 ? UserX : UserCheck" class="h-3 w-3" />{{ user.status === 1 ? '禁用' : '启用' }}
                </button>
                <button class="inline-flex items-center gap-1 rounded-full border border-white/10 px-3 py-1 text-xs hover:bg-white/10" @click="openReset(user)">
                  <KeyRound class="h-3 w-3" />重置
                </button>
                <button class="inline-flex items-center gap-1 rounded-full border border-red-300/30 px-3 py-1 text-xs text-red-100 hover:bg-red-500/10" @click="deleteUser(user)">
                  <Trash2 class="h-3 w-3" />删除
                </button>
              </div>
            </article>
            <div class="flex items-center justify-between px-6 py-4 text-sm text-slate-300">
              <span>共 {{ page.total }} 条</span>
              <div class="flex gap-2">
                <button :disabled="page.current <= 1" class="rounded-full border border-white/10 px-4 py-2 disabled:opacity-40" @click="load(page.current - 1)">上一页</button>
                <button :disabled="page.current * page.size >= page.total" class="rounded-full border border-white/10 px-4 py-2 disabled:opacity-40" @click="load(page.current + 1)">下一页</button>
              </div>
            </div>
          </div>
        </div>

        <aside class="rounded-[2rem] border border-white/10 bg-white/8 p-6 backdrop-blur">
          <div v-if="selected">
            <p class="text-sm text-cyan-200">用户详情</p>
            <h2 class="mt-2 text-2xl font-black">{{ selected.username }}</h2>
            <dl class="mt-5 space-y-3 text-sm text-slate-300">
              <div class="flex justify-between gap-4"><dt>邮箱</dt><dd>{{ selected.email }}</dd></div>
              <div class="flex justify-between gap-4"><dt>角色</dt><dd>{{ roleLabels[selected.role] }}</dd></div>
              <div class="flex justify-between gap-4"><dt>状态</dt><dd>{{ selected.status === 1 ? '启用' : '禁用' }}</dd></div>
            </dl>
            <form v-if="selected.role === 'TEACHER'" class="mt-6 space-y-3" @submit.prevent="saveProfile">
              <textarea v-model="profileForm.bio" class="h-24 w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="教师简介" />
              <input v-model.number="profileForm.teachingYears" type="number" min="0" class="w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="教学年限" />
              <input v-model="profileForm.expertise" class="w-full rounded-2xl border border-white/10 bg-slate-900 px-4 py-3 outline-none focus:border-cyan-300" placeholder="专长" />
              <button class="inline-flex items-center gap-2 rounded-2xl bg-cyan-300 px-5 py-3 font-bold text-slate-950 hover:bg-cyan-200">
                <RotateCw class="h-4 w-4" />保存资料
              </button>
            </form>
            <p v-else class="mt-6 rounded-2xl bg-slate-900 px-4 py-3 text-sm text-slate-300">该用户不是教师，无教师资料需要维护。</p>
          </div>
          <p v-else class="text-sm text-slate-300">点击左侧用户查看详情</p>
        </aside>
      </section>
    </div>
  </main>
</template>
