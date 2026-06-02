import { http } from './request'
import type { AuthResult } from './types'

export const login = (payload: {
  type: 'email' | 'phone'
  identifier: string
  password: string
}) => http.post<AuthResult>('/auth/login', payload)

export const loginByCode = (payload: {
  type: 'phone' | 'email'
  identifier: string
  code: string
}) => http.post<AuthResult>('/auth/login-by-code', payload)

export const register = (payload: {
  type: 'email' | 'phone'
  identifier: string
  password: string
  code?: string
  nickname?: string
}) => http.post<AuthResult>('/auth/register', payload)

export const sendCode = (payload: {
  type: 'phone' | 'email'
  identifier: string
  scene: 'register' | 'login' | 'reset_password' | 'bind'
}) => http.post<{ expires_in: number }>('/auth/send-code', payload)

export const refresh = (payload: { refresh_token: string }) =>
  http.post<{ access_token: string; expires_in: number }>('/auth/refresh', payload)

export const logout = () => http.post<null>('/auth/logout')

export const resetPassword = (payload: {
  type: 'email' | 'phone'
  identifier: string
  code: string
  new_password: string
}) => http.post<null>('/auth/reset-password', payload)

export const me = () =>
  http.get<import('./types').AuthUser>('/user/me')
