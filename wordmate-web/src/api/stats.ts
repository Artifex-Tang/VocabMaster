import { http } from './request'
import type { TodayStats, ForgettingCurveData, LevelOverview } from './types'

export const todayStats = () => http.get<TodayStats>('/stats/today')

export const summary = (period: 'week' | 'month' | 'custom', date?: string) =>
  http.get<unknown>('/stats/summary', { period, date })

export const forgettingCurve = (word_id: number) =>
  http.get<ForgettingCurveData>('/stats/forgetting-curve', { word_id })

export const levelOverview = (level: string) =>
  http.get<LevelOverview>('/stats/level-overview', { level })
