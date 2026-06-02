import { http } from './request'
import type { PageResult, WordBank } from './types'

export const list = (level?: string, resolved = 0, page = 1, page_size = 20) =>
  http.get<PageResult<WordBank>>('/wrong-words', { level, resolved, page, page_size })

export const startReview = (level?: string) =>
  http.post<{ review_words: WordBank[] }>('/wrong-words/review', { level })

export const resolve = (word_id: number) =>
  http.post<null>('/wrong-words/resolve', { word_id })
