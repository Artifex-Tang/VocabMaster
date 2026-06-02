import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { storage } from '@/utils/storage'
import { refreshToken } from '@/api/auth'
import type { UserInfo } from '@/api/types'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null)
  const accessToken = ref<string | null>(null)
  const refreshTokenValue = ref<string | null>(null)
  const tokenExpiresAt = ref<number | null>(null)

  const isLoggedIn = computed(() => !!accessToken.value && !!userInfo.value)

  function initFromStorage() {
    accessToken.value = storage.get<string>('access_token')
    refreshTokenValue.value = storage.get<string>('refresh_token')
    tokenExpiresAt.value = storage.get<number>('token_expires_at')
    userInfo.value = storage.get<UserInfo>('user_info')
  }

  function setAuth(data: { user: UserInfo; access_token: string; refresh_token: string; expires_in: number }) {
    accessToken.value = data.access_token
    refreshTokenValue.value = data.refresh_token
    tokenExpiresAt.value = Date.now() + data.expires_in * 1000
    userInfo.value = data.user

    storage.set('access_token', data.access_token)
    storage.set('refresh_token', data.refresh_token)
    storage.set('token_expires_at', tokenExpiresAt.value)
    storage.set('user_info', data.user)
  }

  async function refreshAccessToken() {
    if (!refreshTokenValue.value) throw new Error('no refresh token')
    const data = await refreshToken(refreshTokenValue.value)
    setAuth(data)
  }

  function logout() {
    accessToken.value = null
    refreshTokenValue.value = null
    tokenExpiresAt.value = null
    userInfo.value = null
    storage.remove('access_token')
    storage.remove('refresh_token')
    storage.remove('token_expires_at')
    storage.remove('user_info')
  }

  return {
    userInfo,
    accessToken,
    isLoggedIn,
    initFromStorage,
    setAuth,
    refreshAccessToken,
    logout,
  }
})
