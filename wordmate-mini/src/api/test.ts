import { http } from '@/utils/request'
import type { TestData, TestResult, TestAvailability } from './types'

export function generate(mode: string, level: string, source: string, size: number) {
  return http.post<TestData>('/test/generate', { mode, level_code: level, source, size })
}

export function submit(testId: string, answers: Array<{ question_id: string; answer: string; duration_ms: number }>) {
  return http.post<TestResult>('/test/submit', { test_id: testId, answers })
}

export function availability(level: string) {
  return http.get<TestAvailability>('/test/availability', { level_code: level })
}
