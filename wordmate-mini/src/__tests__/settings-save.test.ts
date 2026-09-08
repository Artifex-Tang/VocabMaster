import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { http } from '@/utils/request'
import { useSettingsStore } from '@/stores/settings'

vi.mock('@/utils/request', () => ({
  http: {
    get: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
  },
}))

describe('settings store save() — 只提交可编辑字段', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(http.patch).mockClear()
  })

  it('save() 的 PATCH payload 不含 active_levels/default_sort_mode/notification_time', async () => {
    const store = useSettingsStore()
    await store.save()

    expect(http.patch).toHaveBeenCalledTimes(1)
    const [url, payload] = vi.mocked(http.patch).mock.calls[0]
    expect(url).toBe('/user/settings')
    expect(payload).toEqual({
      daily_new_words_goal: 20,
      daily_review_goal: 100,
      preferred_accent: 'uk',
      auto_play_audio: true,
      theme: 'light',
    })
  })

  it('save() 提交修改后的当前值', async () => {
    const store = useSettingsStore()
    store.settings.daily_new_words_goal = 35
    store.settings.theme = 'dark'
    await store.save()

    const payload = vi.mocked(http.patch).mock.calls[0][1]
    expect(payload).toMatchObject({
      daily_new_words_goal: 35,
      theme: 'dark',
    })
  })
})
