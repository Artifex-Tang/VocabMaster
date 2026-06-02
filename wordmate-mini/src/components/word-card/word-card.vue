<template>
  <view class="word-card" @click="handleFlip">
    <view v-if="!flipped" class="face front">
      <text v-if="word.emoji" class="emoji">{{ word.emoji }}</text>
      <image
        v-else-if="word.image_url"
        :src="word.image_url"
        class="image"
        mode="aspectFit"
        lazy-load
      />
      <text class="word">{{ word.word }}</text>
      <view class="ipa-row">
        <text class="ipa">{{ preferredAccent === 'uk' ? word.ipa_uk : word.ipa_us }}</text>
        <view class="audio-btn" @click.stop="playAudio">🔊</view>
      </view>
      <text class="pos">{{ word.pos }}</text>
      <text class="hint">点击查看释义</text>
    </view>

    <view v-else class="face back">
      <view class="section">
        <text class="label">English</text>
        <text class="content">{{ word.en_definition }}</text>
      </view>
      <view class="section">
        <text class="label">中文</text>
        <text class="content zh-def">{{ word.zh_definition }}</text>
      </view>
      <view v-if="word.example_en" class="section">
        <text class="label">Example</text>
        <text class="content example-en">"{{ word.example_en }}"</text>
        <text class="example-zh">{{ word.example_zh }}</text>
      </view>
      <view v-if="word.related_words?.synonyms?.length" class="section">
        <text class="label">近义词</text>
        <text class="content">{{ word.related_words.synonyms.join(' / ') }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useSettingsStore } from '@/stores/settings'
import { speak } from '@/utils/tts'
import type { Word } from '@/api/types'

const props = defineProps<{ word: Word }>()
const emit = defineEmits<{
  flip: []
  audioError: []
}>()

const flipped = ref(false)
const settingsStore = useSettingsStore()
const preferredAccent = computed(() => settingsStore.preferredAccent)

function handleFlip() {
  if (flipped.value) return
  flipped.value = true
  emit('flip')
  if (settingsStore.autoPlayAudio) playAudio()
}

function playAudio() {
  const audioUrl =
    preferredAccent.value === 'uk' ? props.word.audio_url_uk : props.word.audio_url_us
  speak({ text: props.word.word, accent: preferredAccent.value, audioUrl }).catch(() => {
    // 降级：UK → US → 静默
    const fallback = preferredAccent.value === 'uk' ? props.word.audio_url_us : undefined
    if (fallback) {
      speak({ text: props.word.word, accent: 'us', audioUrl: fallback }).catch(() => {
        emit('audioError')
      })
    } else {
      emit('audioError')
    }
  })
}

defineExpose({ reset: () => (flipped.value = false) })
</script>

<style lang="scss" scoped>
.word-card {
  width: 680rpx;
  min-height: 800rpx;
  background: #fff;
  border-radius: $radius-xl;
  box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.08);
  padding: $space-xl;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.front {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  gap: $space-sm;

  .emoji { font-size: 160rpx; line-height: 1; }
  .image { width: 360rpx; height: 280rpx; }
  .word { font-size: $font-3xl; font-weight: 700; color: $color-text-primary; margin-top: $space-sm; }
  .ipa-row { display: flex; align-items: center; gap: $space-sm; }
  .ipa { font-size: $font-md; color: $color-text-secondary; font-family: serif; }
  .audio-btn {
    padding: 6rpx 18rpx;
    background: $color-bg-page;
    border-radius: $radius-lg;
    font-size: $font-lg;
  }
  .pos { font-size: $font-sm; color: $color-primary; }
  .hint { font-size: $font-sm; color: $color-text-placeholder; margin-top: $space-md; }
}

.back {
  display: flex;
  flex-direction: column;
  width: 100%;
  gap: $space-lg;

  .section { width: 100%; }
  .label {
    font-size: $font-xs;
    color: $color-text-placeholder;
    text-transform: uppercase;
    letter-spacing: 2rpx;
    margin-bottom: $space-xs;
    display: block;
  }
  .content { font-size: $font-md; color: $color-text-primary; line-height: 1.6; display: block; }
  .zh-def { font-size: $font-lg; font-weight: 600; }
  .example-en { font-style: italic; color: $color-text-regular; display: block; }
  .example-zh { font-size: $font-sm; color: $color-text-secondary; margin-top: $space-xs; display: block; }
}
</style>
