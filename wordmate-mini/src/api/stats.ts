import { http } from '@/utils/request'
import type { TodayStats, SummaryStats, CalendarData, CheckinResult } from './types'

export function getTodayStats() {
  return http.get<TodayStats>('/stats/today')
}

export function getSummary(period: 'week' | 'month', date?: string) {
  return http.get<SummaryStats>('/stats/summary', { period, date })
}

export function getForgettingCurve(wordId: number) {
  return http.get<{
    word_id: number
    word: string
    reviews: Array<{ ts: string; result: string; stage_after: number }>
    theoretical_curve: { type: string; stages: number[] }
  }>('/stats/forgetting-curve', { word_id: wordId })
}

export function getLevelOverview(levelCode: string) {
  return http.get<{
    level_code: string
    total_words: number
    not_started: number
    learning: number
    mastered: number
    mastery_rate: number
    stage_distribution: Array<{ stage: number; count: number }>
  }>('/stats/level-overview', { level: levelCode })
}

export function checkinToday() {
  return http.post<CheckinResult>('/checkin/today')
}

export function getCalendar(month: string) {
  return http.get<CalendarData>('/checkin/calendar', { month })
}

export function getAchievements() {
  return http.get<{
    unlocked: Array<{ code: string; name_zh: string; icon: string; achieved_at: string }>
    locked: Array<{ code: string; name_zh: string; progress: string }>
  }>('/checkin/achievements')
}
