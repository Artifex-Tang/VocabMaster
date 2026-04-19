import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as userApi from '@/api/user'
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

  const topicMap = ref<Record<string, string>>({})

  async function fetch() {
    const data = await userApi.getSettings()
    settings.value = data
  }

  async function update(patch: Partial<UserSettings>) {
    const data = await userApi.updateSettings(patch)
    settings.value = data
  }

  function setTopicMap(map: Record<string, string>) {
    topicMap.value = map
  }

  return { settings, topicMap, fetch, update, setTopicMap }
})
