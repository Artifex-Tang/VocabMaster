<template>
  <view class="settings">
    <!-- 学习偏好 -->
    <view class="card">
      <text class="card-title">学习偏好</text>

      <view class="row">
        <text class="label">每日新词目标</text>
        <view class="stepper">
          <text class="stepper-btn" @click="adjust('daily_new_words_goal', -5)">-</text>
          <text class="stepper-val">{{ settings.daily_new_words_goal }}</text>
          <text class="stepper-btn" @click="adjust('daily_new_words_goal', 5)">+</text>
        </view>
      </view>

      <view class="row">
        <text class="label">每日复习目标</text>
        <view class="stepper">
          <text class="stepper-btn" @click="adjust('daily_review_goal', -10)">-</text>
          <text class="stepper-val">{{ settings.daily_review_goal }}</text>
          <text class="stepper-btn" @click="adjust('daily_review_goal', 10)">+</text>
        </view>
      </view>

      <view class="row">
        <text class="label">发音偏好</text>
        <view class="toggle-group">
          <text :class="['toggle', settings.preferred_accent === 'uk' && 'active']" @click="setAccent('uk')">英音</text>
          <text :class="['toggle', settings.preferred_accent === 'us' && 'active']" @click="setAccent('us')">美音</text>
        </view>
      </view>

      <view class="row">
        <text class="label">自动播放音频</text>
        <switch :checked="settings.auto_play_audio" @change="toggleAutoPlay" color="#1890ff" />
      </view>
    </view>

    <!-- 主题 -->
    <view class="card">
      <text class="card-title">外观</text>
      <view class="row">
        <text class="label">主题</text>
        <view class="toggle-group">
          <text
            v-for="t in ['light', 'dark', 'system']"
            :key="t"
            :class="['toggle', settings.theme === t && 'active']"
            @click="setTheme(t as 'light' | 'dark' | 'system')"
          >{{ themeLabel(t) }}</text>
        </view>
      </view>
    </view>

    <button class="btn-save" :disabled="saving" @click="saveSettings">
      {{ saving ? '保存中...' : '保存设置' }}
    </button>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useSettingsStore } from '@/stores/settings'
import type { UserSettings } from '@/api/types'

const settingsStore = useSettingsStore()
const saving = ref(false)
const settings = computed(() => settingsStore.settings)

onShow(() => {
  settingsStore.load()
})

function adjust(key: 'daily_new_words_goal' | 'daily_review_goal', delta: number) {
  const val = Math.max(0, settings.value[key] + delta)
  settingsStore.update({ [key]: val })
}

function setAccent(accent: 'uk' | 'us') {
  settingsStore.update({ preferred_accent: accent })
}

function toggleAutoPlay(e: { detail: { value: boolean } }) {
  settingsStore.update({ auto_play_audio: e.detail.value })
}

function setTheme(theme: 'light' | 'dark' | 'system') {
  settingsStore.update({ theme })
}

function themeLabel(t: string) {
  return t === 'light' ? '浅色' : t === 'dark' ? '深色' : '跟随系统'
}

async function saveSettings() {
  saving.value = true
  try {
    await settingsStore.update(settings.value)
    uni.showToast({ title: '已保存', icon: 'success' })
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.settings {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32rpx;
}

.card {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}
.card-title { font-size: 28rpx; font-weight: 600; color: #1f2937; display: block; margin-bottom: 24rpx; }

.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}
.label { font-size: 28rpx; color: #4b5563; }

.stepper { display: flex; align-items: center; gap: 16rpx; }
.stepper-btn {
  width: 56rpx; height: 56rpx; border-radius: 50%;
  background: #f5f7fa; text-align: center; line-height: 56rpx;
  font-size: 32rpx; color: #1890ff; font-weight: 700;
}
.stepper-val { font-size: 28rpx; font-weight: 600; color: #1f2937; min-width: 48rpx; text-align: center; }

.toggle-group { display: flex; gap: 8rpx; }
.toggle {
  padding: 8rpx 20rpx; border-radius: 12rpx; font-size: 24rpx;
  color: #9ca3af; background: #f5f7fa;
}
.toggle.active { color: #1890ff; background: #e6f7ff; font-weight: 600; }

.btn-save {
  width: 100%; height: 96rpx; background: #1890ff; color: #fff;
  font-size: 32rpx; font-weight: 600; border-radius: 48rpx; border: none;
  &[disabled] { opacity: 0.5; }
}
</style>
