import { http } from './request'
import type { WordBank, WordListSummary, WordListDetail } from './types'

/** 词库广场（默认内置） */
export const square = (source_type = 'builtin') =>
  http.get<WordListSummary[]>('/word-lists', { source_type })

/** 词库详情（含各单元进度 + 当前单元游标） */
export const detail = (id: number) => http.get<WordListDetail>(`/word-lists/${id}`)

/** 订阅词库（幂等） */
export const subscribe = (id: number) =>
  http.post<{ list_id: number; current_unit_no: number }>(`/word-lists/${id}/subscribe`)

/** 拉取某单元的新词（未学词） */
export const learn = (id: number, unit: number, limit = 20) =>
  http.get<WordBank[]>(`/word-lists/${id}/learn`, { unit, limit })

/** 推进单元游标 */
export const advanceUnit = (id: number, unitNo: number) =>
  http.post<number>(`/word-lists/${id}/units/${unitNo}/advance`)
