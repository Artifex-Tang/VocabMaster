import { http } from './request'
import type { WordBank, LevelInfo, PageResult } from './types'

export const getLevels = () => http.get<LevelInfo[]>('/words/levels')

export const getTopics = () => http.get<Array<{ code: string; name_zh: string }>>('/words/topics')

export const getWord = (id: number) => http.get<WordBank>(`/words/${id}`)

export const getWordBySpelling = (level: string, word: string) =>
  http.get<WordBank>('/words/by-word', { level, word })

export const downloadLevel = (level: string, since?: string) =>
  http.get<{ level_code: string; version: string; total: number; words: WordBank[] }>(
    '/words/download',
    since ? { level, since } : { level },
  )

export const searchWords = (q: string, level?: string, page = 1, page_size = 20) =>
  http.get<PageResult<WordBank>>('/words/search', { q, level, page, page_size })
