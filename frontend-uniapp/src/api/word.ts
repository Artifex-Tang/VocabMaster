import { http } from '@/utils/request'
import type { LevelInfo, Word, PageResult } from './types'

export function getLevels() {
  return http.get<LevelInfo[]>('/words/levels')
}

export function getWord(id: number) {
  return http.get<Word>(`/words/${id}`)
}

export function searchWords(q: string, level?: string, page = 1, pageSize = 20) {
  return http.get<PageResult<Word>>('/words/search', { q, level, page, page_size: pageSize })
}

export function downloadLevel(levelCode: string, since?: string) {
  return http.get<{ level_code: string; version: string; total: number; words: Word[] }>(
    '/words/download',
    { level: levelCode, since },
  )
}

export function getWrongWords(levelCode?: string, page = 1, pageSize = 20) {
  return http.get<PageResult<Word>>('/wrong-words', { level: levelCode, resolved: 0, page, page_size: pageSize })
}
