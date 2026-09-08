import { defineStore } from 'pinia'
import { ref } from 'vue'
import { storage } from '@/utils/storage'
import { http } from '@/utils/request'
import type { UserSettings } from '@/api/types'

const DEFAULTS: UserSettings = {
  daily_new_words_goal: 20,
  daily_review_goal: 100,
  default_sort_mode: 'alpha',
  preferred_accent: 'uk',
  auto_play_audio: true,
  notification_time: '20:00:00',
  theme: 'light',
  active_levels: [],
}

export const useSettingsStore = defineStore('settings', () => {
  const settings = ref<UserSettings>({ ...DEFAULTS })

  function load() {
    const cached = storage.get<UserSettings>('user_settings')
    if (cached) settings.value = { ...DEFAULTS, ...cached }
  }

  async function fetch() {
    const data = await http.get<UserSettings>('/user/settings')
    settings.value = { ...DEFAULTS, ...data }
    storage.set('user_settings', settings.value)
  }

  async function update(patch: Partial<UserSettings>) {
    const data = await http.patch<UserSettings>('/user/settings', patch)
    settings.value = { ...settings.value, ...data }
    storage.set('user_settings', settings.value)
  }

  /**
   * 整页保存：只提交设置页可编辑字段。
   * 不发 active_levels（后端 DTO 是 String，前端是 string[]，数组会 400），
   * 也不发 default_sort_mode / notification_time（本页无编辑入口）。
   */
  async function save() {
    const s = settings.value
    return update({
      daily_new_words_goal: s.daily_new_words_goal,
      daily_review_goal: s.daily_review_goal,
      preferred_accent: s.preferred_accent,
      auto_play_audio: s.auto_play_audio,
      theme: s.theme,
    })
  }

  // 快捷 getter
  const preferredAccent = ref<'uk' | 'us'>('uk')
  const autoPlayAudio = ref(true)

  function sync() {
    preferredAccent.value = settings.value.preferred_accent
    autoPlayAudio.value = settings.value.auto_play_audio
  }

  return { settings, load, fetch, update, save, preferredAccent, autoPlayAudio, sync }
})
