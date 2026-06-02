import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import { registerAuthHooks } from '@/api/request'
import type { AuthUser } from '@/api/types'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref<string>(localStorage.getItem('vm_access_token') ?? '')
  const refreshToken = ref<string>(localStorage.getItem('vm_refresh_token') ?? '')
  const user = ref<AuthUser | null>(null)
  const isAdmin = ref(false)

  const isLoggedIn = computed(() => !!accessToken.value)

  async function login(type: 'email' | 'phone', identifier: string, password: string) {
    const data = await authApi.login({ type, identifier, password })
    setAuth(data)
  }

  async function loginByCode(type: 'phone' | 'email', identifier: string, code: string) {
    const data = await authApi.loginByCode({ type, identifier, code })
    setAuth(data)
  }

  async function refreshAccessToken() {
    const data = await authApi.refresh({ refresh_token: refreshToken.value })
    accessToken.value = data.access_token
    localStorage.setItem('vm_access_token', data.access_token)
  }

  function setAuth(data: { access_token: string; refresh_token: string; user: AuthUser }) {
    accessToken.value = data.access_token
    refreshToken.value = data.refresh_token
    user.value = data.user
    localStorage.setItem('vm_access_token', data.access_token)
    localStorage.setItem('vm_refresh_token', data.refresh_token)
  }

  function logout() {
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    isAdmin.value = false
    localStorage.removeItem('vm_access_token')
    localStorage.removeItem('vm_refresh_token')
  }

  async function fetchMe() {
    const data = await authApi.me()
    user.value = data
  }

  // Register hooks so request.ts can trigger refresh/logout without importing this store
  registerAuthHooks(refreshAccessToken, logout)

  return {
    accessToken,
    refreshToken,
    user,
    isAdmin,
    isLoggedIn,
    login,
    loginByCode,
    logout,
    fetchMe,
    refreshAccessToken,
    setAuth,
  }
})
