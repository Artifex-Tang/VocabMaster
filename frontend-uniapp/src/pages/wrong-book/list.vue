<template>
  <view class="wrong-book">
    <view v-if="words.length === 0 && !loading" class="empty">
      <text class="empty-emoji">🎉</text>
      <text class="empty-text">错词本是空的，继续保持！</text>
    </view>

    <view v-else class="word-list">
      <view v-for="word in words" :key="word.id" class="word-item">
        <view class="word-main">
          <text class="word-text">{{ word.word }}</text>
          <text class="word-pos">{{ word.pos }}</text>
        </view>
        <text class="word-def">{{ word.zh_definition }}</text>
      </view>
    </view>

    <button v-if="words.length > 0" class="btn-review" @click="startReview">
      开始错词复习（{{ words.length }} 词）
    </button>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getWrongWords } from '@/api/word'
import { http } from '@/utils/request'
import { useSettingsStore } from '@/stores/settings'
import type { Word } from '@/api/types'

const words = ref<Word[]>([])
const loading = ref(true)
const settingsStore = useSettingsStore()

onMounted(async () => {
  try {
    const level = settingsStore.settings.active_levels[0]
    const result = await getWrongWords(level)
    words.value = result.items
  } finally {
    loading.value = false
  }
})

async function startReview() {
  const level = settingsStore.settings.active_levels[0] ?? 'CET4'
  uni.showLoading({ title: '加载...' })
  try {
    const data = await http.post<{ test_id: string }>('/wrong-words/review', { level })
    uni.navigateTo({ url: `/pages/study/session?level=${level}&test_id=${data.test_id}` })
  } finally {
    uni.hideLoading()
  }
}
</script>

<style lang="scss" scoped>
.wrong-book {
  min-height: 100vh; background: $color-bg-page; padding: $space-lg;
}
.empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding-top: 200rpx; gap: $space-lg;
}
.empty-emoji { font-size: 120rpx; }
.empty-text { font-size: $font-md; color: $color-text-secondary; }
.word-list { display: flex; flex-direction: column; gap: $space-sm; margin-bottom: $space-xl; }
.word-item {
  background: #fff; border-radius: $radius-lg; padding: $space-md $space-lg;
}
.word-main { display: flex; align-items: baseline; gap: $space-sm; margin-bottom: 4rpx; }
.word-text { font-size: $font-lg; font-weight: 700; color: $color-text-primary; }
.word-pos { font-size: $font-xs; color: $color-primary; }
.word-def { font-size: $font-sm; color: $color-text-secondary; display: block; }
.btn-review {
  width: 100%; height: 96rpx; background: $color-primary; color: #fff;
  font-size: $font-lg; font-weight: 600; border-radius: $radius-xl; border: none;
  position: sticky; bottom: $space-lg;
}
</style>
