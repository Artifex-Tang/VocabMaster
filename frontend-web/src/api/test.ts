import { http } from './request'
import type { TestSession, TestSubmitAnswer, TestResult, TestMode, TestSource } from './types'

export const generate = (payload: {
  level_code: string
  mode: TestMode
  size?: number
  source?: TestSource
}) => http.post<TestSession>('/test/generate', payload)

export const submit = (test_id: string, answers: TestSubmitAnswer[]) =>
  http.post<TestResult>('/test/submit', { test_id, answers })
