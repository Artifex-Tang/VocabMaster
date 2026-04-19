import { http } from '@/utils/request'
import type { TodayPlan, AnswerPayload, AnswerResult } from './types'

export function getToday(levelCode: string) {
  return http.get<TodayPlan>('/study/today', { level: levelCode })
}

export function answer(payload: AnswerPayload) {
  return http.post<AnswerResult>('/study/answer', payload)
}

export function answerBatch(answers: AnswerPayload[]) {
  return http.post<AnswerResult[]>('/study/answer-batch', { answers })
}

export function resetProgress(wordId: number) {
  return http.post<null>('/study/reset', { word_id: wordId })
}

export function markMastered(wordId: number) {
  return http.post<null>('/study/mark-mastered', { word_id: wordId })
}
