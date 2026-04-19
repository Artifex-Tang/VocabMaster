export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
  request_id?: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  page_size: number
}

// ── Auth ──────────────────────────────────────────────────────────────────────

export interface AuthUser {
  uuid: string
  nickname: string
  email?: string
  phone_masked?: string
  avatar_url?: string
  locale: string
  timezone: string
  bound_providers?: string[]
  created_at?: string
}

export interface AuthResult {
  user: AuthUser
  access_token: string
  refresh_token: string
  expires_in: number
}

// ── User Settings ─────────────────────────────────────────────────────────────

export interface UserSettings {
  daily_new_words_goal: number
  daily_review_goal: number
  default_sort_mode: 'alpha' | 'topic' | 'random'
  preferred_accent: 'uk' | 'us'
  auto_play_audio: boolean
  notification_time: string
  theme: 'light' | 'dark' | 'system'
  active_levels: string[]
}

// ── Word ──────────────────────────────────────────────────────────────────────

export interface WordBank {
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
  audio_url_uk?: string
  audio_url_us?: string
  image_url?: string
  emoji?: string
  pos?: string
  difficulty?: number
  frequency?: number
  related_words?: {
    synonyms?: string[]
    antonyms?: string[]
  }
}

export interface LevelInfo {
  code: string
  name_zh: string
  name_en: string
  target_word_count: number
  sort_order: number
}

// ── Study ─────────────────────────────────────────────────────────────────────

export interface TodayPlan {
  date: string
  review_words: WordBank[]
  new_words: WordBank[]
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

// ── Stats ─────────────────────────────────────────────────────────────────────

export interface TodayStats {
  date: string
  words_learned: number
  words_reviewed: number
  correct_count: number
  accuracy: number
  duration_seconds: number
  goal_progress: { new: string; review: string }
}

export interface ForgettingCurveData {
  word_id: number
  word: string
  reviews: Array<{ ts: string; result: string; stage_after: number }>
  theoretical_curve: { type: string; stages: number[] }
}

export interface LevelOverview {
  level_code: string
  total_words: number
  not_started: number
  learning: number
  mastered: number
  mastery_rate: number
  stage_distribution: Array<{ stage: number; count: number }>
}

// ── Checkin ───────────────────────────────────────────────────────────────────

export interface CheckinResult {
  date: string
  current_streak: number
  longest_streak: number
  total_days: number
  new_achievements: Array<{ code: string; name_zh: string }>
}

export interface CheckinCalendar {
  month: string
  days: Array<{ date: string; checked_in: boolean; words_count?: number }>
  current_streak: number
  longest_streak: number
}

// ── Sync ──────────────────────────────────────────────────────────────────────

export interface SyncPushPayload {
  device_id: string
  answers: AnswerPayload[]
  checkins?: Array<{ checkin_date: string }>
}

export interface SyncPushResult {
  results: Array<{ word_id: number; status: 'accepted' | 'conflict' | 'rejected' }>
}

// ── Test ──────────────────────────────────────────────────────────────────────

export type TestMode = 'spelling' | 'choice' | 'listening'
export type TestSource = 'due' | 'all' | 'wrong_words' | 'custom'

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

export interface TestSession {
  test_id: string
  mode: TestMode
  questions: TestQuestion[]
}

export interface TestSubmitAnswer {
  question_id: string
  answer: string
  duration_ms: number
}

export interface TestResult {
  test_id: string
  mode: TestMode
  total: number
  correct: number
  accuracy: number
  details: Array<{
    question_id: string
    word_id: number
    correct_answer: string
    user_answer: string
    is_correct: boolean
  }>
}
