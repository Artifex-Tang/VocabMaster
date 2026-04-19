import { http } from './request'
import type { PageResult, WordBank } from './types'

export const listWords = (page = 1, page_size = 20) =>
  http.get<PageResult<WordBank>>('/admin/words', { page, page_size })

export const createWord = (payload: Partial<WordBank>) =>
  http.post<WordBank>('/admin/words', payload)

export const updateWord = (id: number, payload: Partial<WordBank>) =>
  http.patch<WordBank>(`/admin/words/${id}`, payload)

export const importWords = (formData: FormData) =>
  http.post<{ imported: number; failed: number }>('/admin/words/import', formData)

export const auditWord = (id: number, approved: boolean) =>
  http.post<null>(`/admin/words/${id}/audit`, { approved })

export const listUsers = (page = 1, page_size = 20) =>
  http.get<PageResult<unknown>>('/admin/users', { page, page_size })

export const updateUser = (uuid: string, payload: { status: 'active' | 'disabled' }) =>
  http.patch<null>(`/admin/users/${uuid}`, payload)

export const dashboard = () =>
  http.get<unknown>('/admin/dashboard')
