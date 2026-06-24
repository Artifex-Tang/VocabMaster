import { http } from '@/utils/request'
import type { Word, WordListSummary, WordListDetail } from './types'

// 词库广场（builtin 内置 + 我的）
export function square(sourceType = 'builtin') {
  return http.get<WordListSummary[]>('/word-lists', { source_type: sourceType })
}

// 词库详情（含单元列表 + 我的进度）
export function detail(id: number) {
  return http.get<WordListDetail>(`/word-lists/${id}`)
}

// 订阅（建 subscription，current_unit_no=1）
export function subscribe(id: number) {
  return http.post<{ list_id: number; current_unit_no: number }>(`/word-lists/${id}/subscribe`)
}

// 拉当前单元新词（反连接 user_word_progress，stage=0 未学过的）
export function learn(id: number, unit: number, limit = 20) {
  return http.get<Word[]>(`/word-lists/${id}/learn`, { unit, limit })
}

// 推进单元游标 current_unit_no
export function advanceUnit(id: number, unitNo: number) {
  return http.post<number>(`/word-lists/${id}/units/${unitNo}/advance`)
}
