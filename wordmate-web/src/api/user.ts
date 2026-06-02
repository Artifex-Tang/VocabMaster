import { http } from './request'
import type { AuthUser, UserSettings } from './types'

export const getMe = () => http.get<AuthUser>('/user/me')

export const updateMe = (payload: Partial<Pick<AuthUser, 'nickname' | 'avatar_url' | 'timezone'>>) =>
  http.patch<AuthUser>('/user/me', payload)

export const getSettings = () => http.get<UserSettings>('/user/settings')

export const updateSettings = (payload: Partial<UserSettings>) =>
  http.patch<UserSettings>('/user/settings', payload)

export const exportData = (format: 'csv' = 'csv') =>
  http.get<{ url: string; expires_at: string }>('/user/export', { format })

export const deleteAccount = (confirm_code: string) =>
  http.post<null>('/user/delete-account', { confirm_code })
