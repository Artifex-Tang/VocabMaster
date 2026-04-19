// ---- 通用 ----
export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  page_size: number
}

// ---- 认证 ----
export interface AuthData {
  user: UserInfo
  access_token: string
  refresh_token: string
  expires_in: number
}

export interface UserInfo {
  uuid: string
  nickname: string
  avatar_url: string
  email?: string
  phone_masked?: string
  timezone: string
  locale: string
  bound_providers: string[]
  created_at: string
}

export interface UserSettings {
  daily_new_words_goal: number
  daily_review_goal: number
  default_sort_mode: 'alpha' | 'topic' | 'random'
  preferred_accent: 'uk' | 'us'
  auto_play_audio: boolean
  notification_time: string
  theme: 'light' | 'dark'
  active_levels: string[]
}

// ---- 词库 ----
export interface LevelInfo {
  code: string
  name_zh: string
  name_en: string
  target_word_count: number
  sort_order: number
}

export interface Word {
  id: number
  level_code: string
  word: string
  ipa_uk: string
  ipa_us: string
  en_definition: string
  zh_definition: string
  example_en: string
  example_zh: string
  topic_code: string
  audio_url_uk: string
  audio_url_us: string
  image_url?: string
  emoji?: string
  pos: string
  related_words?: {
    synonyms: string[]
    antonyms: string[]
  }
}

// ---- 学习 ----
export interface TodayPlan {
  date: string
  review_words: Word[]
  new_words: Word[]
  review_count: number
  new_count: number
  estimated_minutes: number
}

export interface AnswerPayload {
  word_id: number
  level_code: string
  result: 'correct' | 'wrong' | 'skip'
  mode: 'card' | 'spelling' | 'choice' | 'listening'
  duration_ms: number
  client_ts: string
}

export interface AnswerResult {
  word_id: number
  stage_before: number
  stage_after: number
  next_review_at: string
  mastered: boolean
}

// ---- 测试 ----
export interface TestQuestion {
  question_id: string
  word_id: number
  prompt: {
    zh_definition?: string
    audio_url_uk?: string
    audio_url_us?: string
    options?: string[]
  }
}

export interface TestData {
  test_id: string
  mode: 'spelling' | 'choice' | 'listening'
  questions: TestQuestion[]
}

// ---- 统计 ----
export interface TodayStats {
  date: string
  words_learned: number
  words_reviewed: number
  correct_count: number
  accuracy: number
  duration_seconds: number
  goal_progress: { new: string; review: string }
}

export interface SummaryStats {
  period: string
  start_date: string
  end_date: string
  days_active: number
  total_learned: number
  total_reviewed: number
  avg_accuracy: number
  daily_breakdown: Array<{ date: string; learned: number; reviewed: number; accuracy: number }>
  level_breakdown: Array<{ level_code: string; mastered: number; learning: number }>
  topic_heatmap: Array<{ topic_code: string; mastered: number; total: number }>
}

// ---- 打卡 ----
export interface CheckinResult {
  date: string
  current_streak: number
  longest_streak: number
  total_days: number
  new_achievements: Array<{ code: string; name_zh: string }>
}

export interface CalendarData {
  month: string
  days: Array<{ date: string; checked_in: boolean; words_count?: number }>
  current_streak: number
  longest_streak: number
}
