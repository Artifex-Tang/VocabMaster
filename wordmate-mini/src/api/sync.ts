import { http } from '@/utils/request'
import type { AnswerPayload } from './types'

export function push(payload: { device_id: string; answers: AnswerPayload[] }) {
  return http.post<{ results: Array<{ status: 'accepted' | 'conflict' | 'rejected' }> }>(
    '/sync/push',
    payload,
  )
}

export function pull(since: string, deviceId: string) {
  return http.get<{
    server_ts: string
    changes: {
      progress: unknown[]
      checkin: unknown[]
      settings: Record<string, unknown>
    }
  }>('/sync/pull', { since, device_id: deviceId })
}
