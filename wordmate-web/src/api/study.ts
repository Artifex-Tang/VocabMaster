import { http } from './request'
import type { TodayPlan, AnswerPayload, AnswerResult } from './types'

export const today = (level: string) =>
  http.get<TodayPlan>('/study/today', { level })

export const answer = (payload: AnswerPayload) =>
  http.post<AnswerResult>('/study/answer', payload)

export const answerBatch = (answers: AnswerPayload[]) =>
  http.post<Array<AnswerResult & { status: string }>>('/study/answer-batch', { answers })

export const resetProgress = (word_id: number, level_code?: string) =>
  http.post<null>('/study/reset', level_code ? { level_code } : { word_id })

export const markMastered = (word_id: number) =>
  http.post<null>('/study/mark-mastered', { word_id })
