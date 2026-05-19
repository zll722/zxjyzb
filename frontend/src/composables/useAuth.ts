import { reactive, readonly } from 'vue'
import type { UserInfo } from '@/lib/api'
import { authApi, clearToken, getToken, setToken } from '@/lib/api'

const USER_KEY = 'edu_user'

function saveUser(user: UserInfo | null) {
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
  else localStorage.removeItem(USER_KEY)
}

const state = reactive({
  user: null as UserInfo | null,
  initialized: false,
})

export function useAuth() {
  async function login(username: string, password: string) {
    const result = await authApi.login({ username, password })
    setToken(result.token)
    state.user = result.user
    saveUser(result.user)
    return result.user
  }

  async function register(payload: { username: string; password: string; email: string; role: string }) {
    await authApi.register(payload)
    return login(payload.username, payload.password)
  }

  async function init() {
    if (state.initialized) return
    state.initialized = true
    if (!getToken()) return
    try {
      state.user = await authApi.me()
      saveUser(state.user)
    } catch {
      clearToken()
      state.user = null
      saveUser(null)
    }
  }

  function setUserAvatar(avatar: string) {
    if (state.user) state.user.avatar = avatar
  }

  async function refreshUser() {
    state.user = await authApi.me()
    saveUser(state.user)
    return state.user
  }

  function logout() {
    clearToken()
    state.user = null
    saveUser(null)
  }

  return {
    state: readonly(state),
    login,
    register,
    init,
    refreshUser,
    setUserAvatar,
    logout,
  }
}
