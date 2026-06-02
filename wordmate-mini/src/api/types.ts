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
  theme: 'light' | 'dark' | 'system'
  active_levels: string[]
}

// ---- 等级 ----
export interface LevelInfo {
  code: string
  name_zh: string
  name_en: string
  target_word_count: number
  sort_order: number
}

// ---- 词库 ----
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
    derived: string[]
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
  choices?: string[]
  prompt: {
    zh_definition?: string
    en_definition?: string
    word?: string
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

export interface TestResult {
  test_id: string
  total: number
  correct: number
  accuracy: number
  details: Array<{
    question_id: string
    word_id: number
    correct: boolean
    correct_answer?: string
    user_answer?: string
  }>
}

export interface TestAvailability {
  all: number
  due: number
  wrong_words: number
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

// ---- 等级常量 ----
export const LEVELS: LevelInfo[] = [
  { code: 'PRIMARY', name_zh: '小学', name_en: 'Primary', target_word_count: 800, sort_order: 0 },
  { code: 'KET', name_zh: 'KET', name_en: 'KET', target_word_count: 1445, sort_order: 1 },
  { code: 'JUNIOR', name_zh: '初中', name_en: 'Junior High', target_word_count: 1597, sort_order: 2 },
  { code: 'PET', name_zh: 'PET', name_en: 'PET', target_word_count: 2904, sort_order: 3 },
  { code: 'SENIOR', name_zh: '高中', name_en: 'Senior High', target_word_count: 3674, sort_order: 4 },
  { code: 'CET4', name_zh: '四级', name_en: 'CET-4', target_word_count: 3837, sort_order: 5 },
  { code: 'FCE', name_zh: 'FCE', name_en: 'FCE', target_word_count: 5000, sort_order: 6 },
  { code: 'CET6', name_zh: '六级', name_en: 'CET-6', target_word_count: 5396, sort_order: 7 },
  { code: 'CAE', name_zh: 'CAE', name_en: 'CAE', target_word_count: 7500, sort_order: 8 },
  { code: 'TEM8', name_zh: '专八', name_en: 'TEM-8', target_word_count: 10378, sort_order: 9 },
]
