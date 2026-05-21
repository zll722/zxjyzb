<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { KeyRound, Plus, RotateCw, Search, Trash2, UserCheck, UserX } from 'lucide-vue-next'
import { adminApi, type AdminUser, type PageResult, type UserRole } from '@/lib/api'

const filters    = reactive<{ role: UserRole | ''; status: number | ''; keyword: string }>({ role: '', status: '', keyword: '' })
const page       = ref<PageResult<AdminUser>>({ records: [], total: 0, size: 10, current: 1 })
const selected   = ref<AdminUser | null>(null)
const profileForm = reactive({ bio: '', teachingYears: 0, expertise: '' })
const createForm  = reactive({ role: 'TEACHER' as 'TEACHER' | 'ADMIN', username: '', password: '', email: '', bio: '', teachingYears: 0, expertise: '' })
const resetForm   = reactive({ userId: 0, username: '', newPassword: '' })
const loading     = ref(false)
const message     = ref('')
const error       = ref('')

const roleLabels: Record<UserRole, string> = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }
const inputCls = 'h-10 rounded-md border border-border bg-white px-3 text-sm outline-none transition-all duration-200 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15'

async function load(current = 1) {
  loading.value = true; error.value = ''
  try { page.value = await adminApi.users({ current, size: page.value.size, ...filters }) }
  catch (err) { error.value = err instanceof Error ? err.message : '用户列表加载失败' }
  finally { loading.value = false }
}

async function createUser() {
  error.value = ''; message.value = ''
  try {
    if (createForm.role === 'TEACHER') {
      await adminApi.createTeacher({ username: createForm.username, password: createForm.password, email: createForm.email, bio: createForm.bio, teachingYears: createForm.teachingYears, expertise: createForm.expertise })
      message.value = '教师账号已创建'
    } else {
      await adminApi.createAdmin({ username: createForm.username, password: createForm.password, email: createForm.email })
      message.value = '管理员账号已创建'
    }
    Object.assign(createForm, { username: '', password: '', email: '', bio: '', teachingYears: 0, expertise: '' })
    await load(1)
  } catch (err) { error.value = err instanceof Error ? err.message : '账号创建失败' }
}

async function toggleStatus(user: AdminUser) {
  error.value = ''; message.value = ''
  try {
    await adminApi.updateUserStatus(user.id, user.status === 1 ? 0 : 1)
    message.value = user.status === 1 ? '用户已禁用' : '用户已启用'
    await load(page.value.current)
    if (selected.value?.id === user.id) selected.value = await adminApi.userDetail(user.id)
  } catch (err) { error.value = err instanceof Error ? err.message : '用户状态更新失败' }
}

function openReset(user: AdminUser) {
  resetForm.userId = user.id; resetForm.username = user.username; resetForm.newPassword = ''
}

async function resetPassword() {
  if (!resetForm.userId) return
  error.value = ''; message.value = ''
  try {
    await adminApi.resetUserPassword(resetForm.userId, { newPassword: resetForm.newPassword })
    message.value = '密码已重置'
    Object.assign(resetForm, { userId: 0, username: '', newPassword: '' })
  } catch (err) { error.value = err instanceof Error ? err.message : '密码重置失败' }
}

async function deleteUser(user: AdminUser) {
  if (!window.confirm(`确认删除用户 ${user.username}？`)) return
  error.value = ''; message.value = ''
  try {
    await adminApi.deleteUser(user.id)
    message.value = '用户已删除'
    if (selected.value?.id === user.id) selected.value = null
    if (resetForm.userId === user.id) resetForm.userId = 0
    await load(page.value.current)
  } catch (err) { error.value = err instanceof Error ? err.message : '用户删除失败' }
}

async function selectUser(user: AdminUser) {
  error.value = ''
  try {
    selected.value = await adminApi.userDetail(user.id)
    profileForm.bio = selected.value.bio || ''
    profileForm.teachingYears = selected.value.teachingYears || 0
    profileForm.expertise = selected.value.expertise || ''
  } catch (err) { error.value = err instanceof Error ? err.message : '用户详情加载失败' }
}

async function saveProfile() {
  if (!selected.value) return
  error.value = ''; message.value = ''
  try {
    selected.value = await adminApi.updateTeacherProfile(selected.value.id, profileForm)
    message.value = '教师资料已更新'
    await load(page.value.current)
  } catch (err) { error.value = err instanceof Error ? err.message : '教师资料保存失败' }
}

function formatTime(value?: string) { return value ? new Date(value).toLocaleString() : '-' }

onMounted(() => load())
</script>

<template>
  <div class="mx-auto max-w-7xl">
    <div class="mb-6">
      <p class="text-sm font-medium text-neutral-400">管理员用户管理</p>
      <h1 class="mt-1 text-2xl font-semibold text-neutral-900">用户、状态与教师资料</h1>
    </div>

    <!-- Filter bar -->
    <div class="mb-6 rounded-xl border border-border bg-white p-4 shadow-sm">
      <form class="flex flex-wrap gap-3" @submit.prevent="load(1)">
        <select v-model="filters.role" :class="inputCls + ' w-36'">
          <option value="">全部角色</option>
          <option value="ADMIN">管理员</option>
          <option value="TEACHER">教师</option>
          <option value="STUDENT">学生</option>
        </select>
        <select v-model="filters.status" :class="inputCls + ' w-36'">
          <option value="">全部状态</option>
          <option :value="1">启用</option>
          <option :value="0">禁用</option>
        </select>
        <input v-model="filters.keyword" :class="inputCls + ' min-w-40 flex-1'" placeholder="用户名或邮箱" />
        <button class="inline-flex h-10 items-center gap-1.5 rounded-md bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500">
          <Search class="h-4 w-4" />筛选
        </button>
      </form>
      <div v-if="message" class="mt-3 rounded-md bg-success-bg px-3 py-2 text-sm text-success">{{ message }}</div>
      <div v-if="error"   class="mt-3 rounded-md bg-danger-bg  px-3 py-2 text-sm text-danger">{{ error }}</div>
    </div>

    <div class="grid gap-6 lg:grid-cols-[1fr_360px]">
      <div class="space-y-4">

        <!-- Create form -->
        <form class="rounded-xl border border-border bg-white p-5 shadow-sm" @submit.prevent="createUser">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p class="text-xs text-neutral-400">创建账号</p>
              <h2 class="font-semibold text-neutral-900">教师与子管理员</h2>
            </div>
            <select v-model="createForm.role" :class="inputCls + ' w-36'">
              <option value="TEACHER">教师账号</option>
              <option value="ADMIN">管理员账号</option>
            </select>
          </div>
          <div class="mt-4 grid gap-3 md:grid-cols-3">
            <input v-model="createForm.username" required minlength="3" maxlength="50" :class="inputCls" placeholder="用户名" />
            <input v-model="createForm.email"    required type="email"  :class="inputCls" placeholder="邮箱" />
            <input v-model="createForm.password" required type="password" minlength="6" maxlength="50" :class="inputCls" placeholder="初始密码" />
          </div>
          <div v-if="createForm.role === 'TEACHER'" class="mt-3 grid gap-3 md:grid-cols-3">
            <input v-model="createForm.bio"                          :class="inputCls" placeholder="教师简介" />
            <input v-model.number="createForm.teachingYears" type="number" min="0" :class="inputCls" placeholder="教龄（年）" />
            <input v-model="createForm.expertise"                    :class="inputCls" placeholder="擅长方向" />
          </div>
          <button class="mt-4 inline-flex h-10 items-center gap-1.5 rounded-md bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500">
            <Plus class="h-4 w-4" />{{ createForm.role === 'TEACHER' ? '创建教师' : '创建管理员' }}
          </button>
        </form>

        <!-- Reset password — primary-50 border instead of amber -->
        <div v-if="resetForm.userId" class="rounded-xl border border-primary-200 bg-primary-50 p-4">
          <p class="text-sm font-medium text-primary-800">重置「{{ resetForm.username }}」的密码</p>
          <form class="mt-3 flex gap-2" @submit.prevent="resetPassword">
            <input v-model="resetForm.newPassword" required type="password" minlength="6" maxlength="50" :class="inputCls + ' flex-1 bg-white'" placeholder="新密码" />
            <button class="inline-flex h-10 items-center gap-1.5 rounded-md bg-primary-600 px-3 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500">
              <KeyRound class="h-4 w-4" />确认
            </button>
          </form>
        </div>

        <!-- User table -->
        <div class="overflow-hidden rounded-xl border border-border bg-white shadow-sm">
          <div class="grid grid-cols-[1fr_1fr_100px_90px_auto] gap-4 border-b border-border bg-neutral-50 px-4 py-3 text-xs font-medium text-neutral-500">
            <span>用户</span><span>邮箱</span><span>角色</span><span>状态</span><span>操作</span>
          </div>
          <p v-if="loading" class="px-4 py-6 text-sm text-neutral-400">用户加载中…</p>
          <p v-else-if="!page.records.length" class="px-4 py-6 text-sm text-neutral-400">暂无符合条件的用户</p>
          <article
            v-for="user in page.records"
            v-else
            :key="user.id"
            class="grid grid-cols-[1fr_1fr_100px_90px_auto] items-center gap-4 border-b border-border px-4 py-3 last:border-b-0 hover:bg-neutral-50/80"
          >
            <button class="text-left" @click="selectUser(user)">
              <p class="font-medium text-neutral-900">{{ user.username }}</p>
              <p class="text-xs text-neutral-400">ID {{ user.id }} · {{ formatTime(user.createdAt) }}</p>
            </button>
            <p class="truncate text-sm text-neutral-500">{{ user.email }}</p>
            <span class="text-sm font-medium text-primary-600">{{ roleLabels[user.role] }}</span>
            <span
              class="w-fit rounded-full px-2 py-0.5 text-xs font-medium"
              :class="user.status === 1 ? 'bg-success-bg text-success' : 'bg-danger-bg text-danger'"
            >{{ user.status === 1 ? '启用' : '禁用' }}</span>
            <div class="flex flex-wrap gap-1.5">
              <button class="min-h-[28px] rounded-md border border-border px-2 py-1 text-xs transition-colors hover:bg-neutral-100" @click="selectUser(user)">详情</button>
              <button
                class="inline-flex min-h-[28px] items-center gap-1 rounded-md border border-border px-2 py-1 text-xs transition-colors hover:bg-neutral-100"
                @click="toggleStatus(user)"
              >
                <component :is="user.status === 1 ? UserX : UserCheck" class="h-3 w-3" />
                {{ user.status === 1 ? '禁用' : '启用' }}
              </button>
              <button
                class="inline-flex min-h-[28px] items-center gap-1 rounded-md border border-border px-2 py-1 text-xs transition-colors hover:bg-neutral-100"
                @click="openReset(user)"
              >
                <KeyRound class="h-3 w-3" />重置
              </button>
              <button
                class="inline-flex min-h-[28px] items-center gap-1 rounded-md border border-danger/30 px-2 py-1 text-xs text-danger transition-colors hover:bg-danger-bg"
                @click="deleteUser(user)"
              >
                <Trash2 class="h-3 w-3" />删除
              </button>
            </div>
          </article>
          <div class="flex items-center justify-between px-4 py-3 text-xs text-neutral-400">
            <span>共 {{ page.total }} 条</span>
            <div class="flex gap-1.5">
              <button :disabled="page.current <= 1"                        class="rounded-md border border-border px-3 py-1.5 text-xs disabled:opacity-40 hover:bg-neutral-100 transition-colors" @click="load(page.current - 1)">上一页</button>
              <button :disabled="page.current * page.size >= page.total"   class="rounded-md border border-border px-3 py-1.5 text-xs disabled:opacity-40 hover:bg-neutral-100 transition-colors" @click="load(page.current + 1)">下一页</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Detail sidebar -->
      <aside class="rounded-xl border border-border bg-white p-5 shadow-sm">
        <div v-if="selected">
          <p class="text-xs text-neutral-400">用户详情</p>
          <h2 class="mt-1 font-semibold text-neutral-900">{{ selected.username }}</h2>
          <dl class="mt-4 space-y-2 text-sm">
            <div class="flex justify-between gap-4">
              <dt class="text-neutral-400">邮箱</dt>
              <dd class="text-neutral-700">{{ selected.email }}</dd>
            </div>
            <div class="flex justify-between gap-4">
              <dt class="text-neutral-400">角色</dt>
              <dd class="text-neutral-700">{{ roleLabels[selected.role] }}</dd>
            </div>
            <div class="flex justify-between gap-4">
              <dt class="text-neutral-400">状态</dt>
              <dd>
                <span
                  class="rounded-full px-2 py-0.5 text-xs font-medium"
                  :class="selected.status === 1 ? 'bg-success-bg text-success' : 'bg-danger-bg text-danger'"
                >{{ selected.status === 1 ? '启用' : '禁用' }}</span>
              </dd>
            </div>
          </dl>

          <form v-if="selected.role === 'TEACHER'" class="mt-5 space-y-3" @submit.prevent="saveProfile">
            <h3 class="text-xs font-medium text-neutral-500">教师资料</h3>
            <textarea
              v-model="profileForm.bio"
              class="h-20 w-full rounded-md border border-border bg-white px-3 py-2 text-sm outline-none transition-all duration-200 focus:border-primary-600 focus:ring-2 focus:ring-primary-600/15"
              placeholder="教师简介"
            />
            <input v-model.number="profileForm.teachingYears" type="number" min="0" :class="'w-full ' + inputCls" placeholder="教学年限" />
            <input v-model="profileForm.expertise" :class="'w-full ' + inputCls" placeholder="专长" />
            <button class="inline-flex h-10 items-center gap-1.5 rounded-md bg-primary-600 px-4 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-primary-500">
              <RotateCw class="h-4 w-4" />保存资料
            </button>
          </form>
          <p v-else class="mt-5 rounded-md bg-neutral-100 px-3 py-2 text-sm text-neutral-500">
            该用户不是教师，无教师资料需要维护。
          </p>
        </div>
        <p v-else class="text-sm text-neutral-400">点击左侧用户查看详情</p>
      </aside>
    </div>
  </div>
</template>
