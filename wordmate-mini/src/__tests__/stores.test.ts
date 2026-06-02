import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useStudyStore } from '@/stores/study'
import { useSettingsStore } from '@/stores/settings'
import { storage } from '@/utils/storage'
import type { TodayPlan } from '@/api/types'

describe('study store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should initialize with defaults', () => {
    const store = useStudyStore()
    expect(store.queue).toHaveLength(0)
    expect(store.currentIdx).toBe(0)
    expect(store.correctCount).toBe(0)
  })

  it('should init session', () => {
    const store = useStudyStore()
    const plan: TodayPlan = {
      date: '2026-06-02',
      review_words: [{ id: 1, word: 'hello', level_code: 'CET4', ipa_uk: '', ipa_us: '', en_definition: '', zh_definition: '你好', example_en: '', example_zh: '', topic_code: '', audio_url_uk: '', audio_url_us: '', pos: '' }],
      new_words: [{ id: 2, word: 'world', level_code: 'CET4', ipa_uk: '', ipa_us: '', en_definition: '', zh_definition: '世界', example_en: '', example_zh: '', topic_code: '', audio_url_uk: '', audio_url_us: '', pos: '' }],
      review_count: 1,
      new_count: 1,
      estimated_minutes: 5,
    }
    store.initSession(plan, 'CET4')
    expect(store.queue).toHaveLength(2)
    expect(store.currentLevel).toBe('CET4')
    expect(store.currentIdx).toBe(0)
    expect(store.correctCount).toBe(0)
  })

  it('should nextWord', () => {
    const store = useStudyStore()
    const plan: TodayPlan = {
      date: '2026-06-02',
      review_words: [{ id: 1, word: 'a', level_code: 'CET4', ipa_uk: '', ipa_us: '', en_definition: '', zh_definition: '', example_en: '', example_zh: '', topic_code: '', audio_url_uk: '', audio_url_us: '', pos: '' }],
      new_words: [{ id: 2, word: 'b', level_code: 'CET4', ipa_uk: '', ipa_us: '', en_definition: '', zh_definition: '', example_en: '', example_zh: '', topic_code: '', audio_url_uk: '', audio_url_us: '', pos: '' }],
      review_count: 1,
      new_count: 1,
      estimated_minutes: 2,
    }
    store.initSession(plan, 'CET4')
    store.nextWord()
    expect(store.currentIdx).toBe(1)
  })

  it('should markCorrect', () => {
    const store = useStudyStore()
    store.markCorrect()
    store.markCorrect()
    expect(store.correctCount).toBe(2)
  })

  it('should reset', () => {
    const store = useStudyStore()
    const plan: TodayPlan = {
      date: '2026-06-02',
      review_words: [],
      new_words: [{ id: 1, word: 'a', level_code: 'CET4', ipa_uk: '', ipa_us: '', en_definition: '', zh_definition: '', example_en: '', example_zh: '', topic_code: '', audio_url_uk: '', audio_url_us: '', pos: '' }],
      review_count: 0,
      new_count: 1,
      estimated_minutes: 1,
    }
    store.initSession(plan, 'CET4')
    store.markCorrect()
    store.reset()
    expect(store.queue).toHaveLength(0)
    expect(store.correctCount).toBe(0)
    expect(store.currentIdx).toBe(0)
  })
})

describe('settings store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should initialize with defaults', () => {
    const store = useSettingsStore()
    expect(store.settings.daily_new_words_goal).toBe(20)
    expect(store.settings.preferred_accent).toBe('uk')
    expect(store.settings.theme).toBe('light')
    expect(store.settings.active_levels).toEqual([])
  })

  it('should sync computed refs', () => {
    const store = useSettingsStore()
    store.sync()
    expect(store.preferredAccent).toBe('uk')
    expect(store.autoPlayAudio).toBe(true)
  })

  it('should load from storage', () => {
    const store = useSettingsStore()
    storage.set('user_settings', { daily_new_words_goal: 50, preferred_accent: 'us' })
    store.load()
    expect(store.settings.daily_new_words_goal).toBe(50)
    expect(store.settings.preferred_accent).toBe('us')
    // Other fields keep defaults
    expect(store.settings.daily_review_goal).toBe(100)
  })
})
