import { http } from './request'
import type { SyncPushPayload, SyncPushResult } from './types'

export const pull = (since: string, device_id: string) =>
  http.get<unknown>('/sync/pull', { since, device_id })

export const push = (payload: SyncPushPayload) =>
  http.post<SyncPushResult>('/sync/push', payload)
