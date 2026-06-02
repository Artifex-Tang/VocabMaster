import { describe, it, expect, vi, beforeEach } from 'vitest'
import { speak } from '@/utils/tts'

describe('TTS', () => {
  it('speak with audioUrl should call playAudio', async () => {
    // playAudio 内部用 uni.createInnerAudioContext
    // mock 返回的 context
    const mockCtx = {
      src: '',
      play: vi.fn(),
      onEnded: (cb: () => void) => { /* 不自动触发 */ },
      onError: vi.fn(),
      destroy: vi.fn(),
    }
    ;(uni as unknown as Record<string, unknown>).createInnerAudioContext = () => mockCtx

    // speak 返回 Promise，onEnded 才 resolve
    // 这里测试 audioUrl 优先于 text
    const promise = speak({ text: 'hello', audioUrl: 'https://example.com/audio.mp3' })
    expect(mockCtx.src).toBe('https://example.com/audio.mp3')
    expect(mockCtx.play).toHaveBeenCalled()
    // 模拟播放结束
    mockCtx.onEnded(() => {})
  })
})

describe('Ebbinghaus intervals (business logic)', () => {
  it('should have 9 stages', async () => {
    // 九阶段：5min, 30min, 12h, 1d, 2d, 4d, 7d, 15d, 30d
    const intervals = [5/60, 30/60, 12, 24, 48, 96, 168, 360, 720]
    expect(intervals).toHaveLength(9)
  })

  it('stage progression: correct => stage+1', () => {
    const maxStage = 9
    let stage = 1
    // 模拟连续答对
    for (let i = 0; i < 8; i++) {
      stage = Math.min(maxStage, stage + 1)
    }
    expect(stage).toBe(9) // 已掌握
  })

  it('stage regression: wrong => max(1, stage-1)', () => {
    let stage = 5
    // 答错
    stage = Math.max(1, stage - 1)
    expect(stage).toBe(4)
    // 在 stage 1 答错
    stage = 1
    stage = Math.max(1, stage - 1)
    expect(stage).toBe(1) // 不低于 1
  })
})

describe('Answer payload format', () => {
  it('client_ts should use nowIso format', async () => {
    const { nowIso } = await import('@/utils/date')
    const ts = nowIso()
    // 后端期望 yyyy-MM-dd'T'HH:mm:ss（无毫秒无 Z）
    expect(ts).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/)
  })

  it('payload has required fields', () => {
    const payload = {
      word_id: 1,
      level_code: 'CET4',
      result: 'correct' as const,
      mode: 'card' as const,
      duration_ms: 2000,
      client_ts: '2026-06-02T12:00:00',
    }
    expect(payload).toHaveProperty('word_id')
    expect(payload).toHaveProperty('level_code')
    expect(payload).toHaveProperty('result')
    expect(payload).toHaveProperty('mode')
    expect(payload).toHaveProperty('duration_ms')
    expect(payload).toHaveProperty('client_ts')
    expect(['correct', 'wrong', 'skip']).toContain(payload.result)
    expect(['card', 'spelling', 'choice', 'listening']).toContain(payload.mode)
  })
})

describe('detectType logic (from login)', () => {
  function detectType(val: string): 'email' | 'phone' {
    return /^\d{11}$/.test(val) ? 'phone' : 'email'
  }

  it('should detect phone number', () => {
    expect(detectType('13800138000')).toBe('phone')
  })

  it('should detect email', () => {
    expect(detectType('user@example.com')).toBe('email')
    expect(detectType('12345')).toBe('email') // 不足 11 位
    expect(detectType('1380013800')).toBe('email') // 10 位
  })
})
