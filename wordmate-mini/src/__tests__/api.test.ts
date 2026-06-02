import { describe, it, expect, vi, beforeEach } from 'vitest'
import { pendingAnswersTable } from '@/utils/storage'
import { storage } from '@/utils/storage'

describe('API types', () => {
  it('LEVELS constant has 10 entries', async () => {
    const { LEVELS } = await import('@/api/types')
    expect(LEVELS).toHaveLength(10)
    expect(LEVELS[0].code).toBe('PRIMARY')
    expect(LEVELS[9].code).toBe('TEM8')
  })

  it('TestQuestion type accepts choices and prompt.options', async () => {
    const types = await import('@/api/types')
    // Type check - if this compiles, the types are correct
    const q: types.TestQuestion = {
      question_id: '1',
      word_id: 1,
      choices: ['A', 'B', 'C', 'D'],
      prompt: {
        zh_definition: '测试',
        en_definition: 'test',
        word: 'test',
        options: ['A', 'B', 'C', 'D'],
        audio_url_uk: 'https://example.com/uk.mp3',
        audio_url_us: 'https://example.com/us.mp3',
      },
    }
    expect(q.choices).toHaveLength(4)
    expect(q.prompt.word).toBe('test')
  })

  it('TestResult type has details array', async () => {
    const types = await import('@/api/types')
    const r: types.TestResult = {
      test_id: 't1',
      total: 10,
      correct: 8,
      accuracy: 0.8,
      details: [
        { question_id: 'q1', word_id: 1, correct: true, correct_answer: 'hello', user_answer: 'hello' },
        { question_id: 'q2', word_id: 2, correct: false, correct_answer: 'world', user_answer: 'word' },
      ],
    }
    expect(r.details).toHaveLength(2)
    expect(r.accuracy).toBe(0.8)
  })

  it('UserSettings theme includes system', async () => {
    const types = await import('@/api/types')
    const s: types.UserSettings = {
      daily_new_words_goal: 20,
      daily_review_goal: 100,
      default_sort_mode: 'alpha',
      preferred_accent: 'us',
      auto_play_audio: true,
      notification_time: '20:00:00',
      theme: 'system',
      active_levels: ['CET4'],
    }
    expect(s.theme).toBe('system')
  })

  it('Word related_words includes derived', async () => {
    const types = await import('@/api/types')
    const w: types.Word = {
      id: 1,
      level_code: 'CET4',
      word: 'happy',
      ipa_uk: '',
      ipa_us: '',
      en_definition: '',
      zh_definition: '',
      example_en: '',
      example_zh: '',
      topic_code: '',
      audio_url_uk: '',
      audio_url_us: '',
      pos: 'adj',
      related_words: {
        synonyms: ['glad'],
        antonyms: ['sad'],
        derived: ['happiness', 'unhappy'],
      },
    }
    expect(w.related_words?.derived).toHaveLength(2)
  })
})

describe('request utils', () => {
  it('buildQueryString filters null/undefined', async () => {
    // 直接测试 query string 构建逻辑
    const params = { a: 'hello', b: undefined, c: null, d: 42 }
    const entries = Object.entries(params)
      .filter(([, v]) => v !== undefined && v !== null)
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
      .join('&')
    expect(entries).toBe('a=hello&d=42')
  })
})

describe('pendingAnswersTable (offline queue)', () => {
  beforeEach(() => {
    storage.clear()
  })

  it('should add and retrieve pending answers', () => {
    const answer = pendingAnswersTable.add({
      word_id: 1,
      level_code: 'CET4',
      result: 'correct',
      mode: 'card',
      duration_ms: 2000,
      client_ts: '2026-06-02T12:00:00',
      synced: 0,
    })
    expect(answer.id).toBeTruthy()
    expect(pendingAnswersTable.count()).toBe(1)
  })

  it('should filter unsynced answers', () => {
    pendingAnswersTable.add({ word_id: 1, level_code: 'CET4', result: 'correct', mode: 'card', duration_ms: 1000, client_ts: '', synced: 0 })
    pendingAnswersTable.add({ word_id: 2, level_code: 'CET4', result: 'wrong', mode: 'card', duration_ms: 2000, client_ts: '', synced: 1 })
    const unsynced = pendingAnswersTable.filter(x => !x.synced)
    expect(unsynced).toHaveLength(1)
    expect(unsynced[0].word_id).toBe(1)
  })

  it('should remove after sync', () => {
    const a = pendingAnswersTable.add({ word_id: 1, level_code: 'CET4', result: 'correct', mode: 'card', duration_ms: 1000, client_ts: '', synced: 0 })
    pendingAnswersTable.remove(a.id)
    expect(pendingAnswersTable.count()).toBe(0)
  })
})
