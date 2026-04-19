import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { getDeviceId } from '@/utils/device-id'
import type { ApiResponse } from './types'

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api/v1',
  timeout: 15000,
})

// ── Token refresh hook (set by userStore after init) ──────────────────────────
// Breaks the circular dependency: request.ts ← stores/user.ts ← api/auth.ts ← request.ts
// The store registers its refreshAccessToken callback here instead of being imported directly.

type RefreshFn = () => Promise<void>
type LogoutFn = () => void

let _refresh: RefreshFn | null = null
let _logout: LogoutFn | null = null

export function registerAuthHooks(refresh: RefreshFn, logout: LogoutFn) {
  _refresh = refresh
  _logout = logout
}

// ── Request interceptor ────────────────────────────────────────────────────────

instance.interceptors.request.use(config => {
  // Read token directly from localStorage — no store import needed
  const token = localStorage.getItem('vm_access_token')
  if (token) config.headers['Authorization'] = `Bearer ${token}`
  config.headers['X-Device-Id'] = getDeviceId()
  config.headers['X-Device-Type'] = import.meta.env.VITE_DEVICE_TYPE || 'web'
  config.headers['X-Client-Version'] = import.meta.env.VITE_APP_VERSION || '0.1.0'
  return config
})

// ── Token refresh queue ────────────────────────────────────────────────────────

let isRefreshing = false
let refreshQueue: Array<() => void> = []

// ── Response interceptor ───────────────────────────────────────────────────────

// eslint-disable-next-line @typescript-eslint/no-explicit-any
instance.interceptors.response.use(
  (res): any => {
    const body = res.data as ApiResponse
    if (body.code === 0) return body.data

    if (body.code === 20002) {
      return handleTokenRefresh(res.config)
    }

    ElMessage.error(body.msg || '请求失败')
    return Promise.reject(new Error(body.msg))
  },
  err => {
    const status = err.response?.status
    if (status === 401) {
      _logout?.()
      window.location.href = '/login'
    } else if (status === 429) {
      ElMessage.warning('请求太频繁，请稍后再试')
    } else if (err.code === 'ERR_NETWORK' || err.code === 'ECONNABORTED') {
      // Offline — callers handle queuing
    } else {
      ElMessage.error(err.message || '网络错误')
    }
    return Promise.reject(err)
  },
)

async function handleTokenRefresh(config: AxiosRequestConfig) {
  if (!isRefreshing) {
    isRefreshing = true
    try {
      if (_refresh) await _refresh()
      refreshQueue.forEach(cb => cb())
      refreshQueue = []
    } catch (e) {
      _logout?.()
      window.location.href = '/login'
      return Promise.reject(e)
    } finally {
      isRefreshing = false
    }
  }

  return new Promise(resolve => {
    refreshQueue.push(() => resolve(instance(config)))
  })
}

export default instance

export const http = {
  get: <T>(url: string, params?: Record<string, unknown>) =>
    instance.get<unknown, T>(url, { params }),
  post: <T>(url: string, data?: unknown) =>
    instance.post<unknown, T>(url, data),
  patch: <T>(url: string, data?: unknown) =>
    instance.patch<unknown, T>(url, data),
  del: <T>(url: string) =>
    instance.delete<unknown, T>(url),
}
