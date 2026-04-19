import { getDeviceId } from '@/utils/device-id'
import { getPlatform } from '@/utils/platform'

const BASE_URL = (import.meta.env.VITE_API_BASE as string | undefined) ?? 'http://localhost:8080/api/v1'
const CLIENT_VERSION = '1.0.0'

export interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE'
  data?: unknown
  params?: Record<string, unknown>
  header?: Record<string, string>
  noAuth?: boolean
}

interface ApiResponse<T> {
  code: number
  msg: string
  data: T
  request_id?: string
}

function buildQueryString(params: Record<string, unknown>): string {
  return Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null)
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
    .join('&')
}

// 延迟导入 userStore，避免 Pinia 未初始化时的循环引用
function getUserStore() {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const { useUserStore } = require('@/stores/user') as { useUserStore: () => {
    accessToken: string | null
    refreshAccessToken: () => Promise<void>
    logout: () => void
  } }
  return useUserStore()
}

export function request<T = unknown>(opts: RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const userStore = getUserStore()

    const header: Record<string, string> = {
      'Content-Type': 'application/json',
      'X-Device-Id': getDeviceId(),
      'X-Device-Type': getPlatform(),
      'X-Client-Version': CLIENT_VERSION,
      ...opts.header,
    }

    if (!opts.noAuth && userStore.accessToken) {
      header.Authorization = `Bearer ${userStore.accessToken}`
    }

    let url = BASE_URL + opts.url
    if (opts.params) {
      const qs = buildQueryString(opts.params)
      if (qs) url += '?' + qs
    }

    uni.request({
      url,
      method: opts.method ?? 'GET',
      data: opts.data as Record<string, unknown>,
      header,
      timeout: 15000,
      success: async (res) => {
        const body = res.data as ApiResponse<T>

        if (body.code === 0) return resolve(body.data)

        if (body.code === 20002) {
          try {
            await userStore.refreshAccessToken()
            const retry = await request<T>(opts)
            return resolve(retry)
          } catch {
            userStore.logout()
            uni.reLaunch({ url: '/pages/auth/login' })
            return reject(new Error('token refresh failed'))
          }
        }

        if (body.code === 20001) {
          userStore.logout()
          uni.reLaunch({ url: '/pages/auth/login' })
        } else {
          uni.showToast({ title: body.msg, icon: 'none', duration: 2000 })
        }

        reject(new Error(body.msg))
      },
      fail: (err) => {
        uni.showToast({ title: '网络错误，请检查连接', icon: 'none', duration: 2000 })
        reject(err)
      },
    })
  })
}

export const http = {
  get: <T>(url: string, params?: Record<string, unknown>) =>
    request<T>({ url, method: 'GET', params }),
  post: <T>(url: string, data?: unknown) =>
    request<T>({ url, method: 'POST', data }),
  patch: <T>(url: string, data?: unknown) =>
    request<T>({ url, method: 'PATCH', data }),
  del: <T>(url: string) =>
    request<T>({ url, method: 'DELETE' }),
}
