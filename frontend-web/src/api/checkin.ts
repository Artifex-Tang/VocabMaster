import { http } from './request'
import type { CheckinResult, CheckinCalendar } from './types'

export const checkinToday = () => http.post<CheckinResult>('/checkin/today')

export const calendar = (month: string) =>
  http.get<CheckinCalendar>('/checkin/calendar', { month })

export const achievements = () =>
  http.get<{ unlocked: unknown[]; locked: unknown[] }>('/checkin/achievements')
