import { http } from '@/utils/request'
import type { AuthData } from './types'

export function loginByPassword(type: 'email' | 'phone', identifier: string, password: string) {
  return http.post<AuthData>('/auth/login', { type, identifier, password })
}

export function loginByCode(type: 'phone' | 'email', identifier: string, code: string) {
  return http.post<AuthData>('/auth/login-by-code', { type, identifier, code })
}

export function loginWechat(code: string, userInfo?: { nickname: string; avatar_url: string }) {
  return http.post<AuthData>('/auth/login-wechat', { code, user_info: userInfo })
}

export function register(
  type: 'email' | 'phone',
  identifier: string,
  password: string,
  code: string,
  nickname?: string,
) {
  return http.post<AuthData>('/auth/register', { type, identifier, password, code, nickname })
}

export function sendCode(type: 'phone' | 'email', identifier: string, scene: string) {
  return http.post<{ expires_in: number }>('/auth/send-code', { type, identifier, scene })
}

export function refreshToken(refresh_token: string) {
  return http.post<AuthData>('/auth/refresh', { refresh_token })
}

export function logout() {
  return http.post<null>('/auth/logout')
}

// 微信小程序一键登录
export function wechatLoginMiniProgram(): Promise<AuthData> {
  return new Promise((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: ({ code }) => {
        loginWechat(code).then(resolve).catch(reject)
      },
      fail: reject,
    })
  })
}
