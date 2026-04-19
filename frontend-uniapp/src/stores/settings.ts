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

  // 快捷 getter
  const preferredAccent = ref<'uk' | 'us'>('uk')
  const autoPlayAudio = ref(true)

  function sync() {
    preferredAccent.value = settings.value.preferred_accent
    autoPlayAudio.value = settings.value.auto_play_audio
  }

  return { settings, load, fetch, update, preferredAccent, autoPlayAudio, sync }
})
