<template>
  <view class="test-home">
    <text class="page-title">测试模式</text>
    <text class="page-sub">检验你的掌握程度</text>

    <view class="mode-list">
      <view
        v-for="mode in modes"
        :key="mode.key"
        class="mode-card"
        @click="selectMode(mode)"
      >
        <text class="mode-emoji">{{ mode.emoji }}</text>
        <view class="mode-info">
          <text class="mode-name">{{ mode.name }}</text>
          <text class="mode-desc">{{ mode.desc }}</text>
        </view>
        <text class="mode-arrow">›</text>
      </view>
    </view>

    <view class="config-card">
      <text class="config-title">设置</text>
      <view class="config-row">
        <text class="config-label">题目数量</text>
        <view class="size-options">
          <text
            v-for="s in [10, 20, 30]"
            :key="s"
            class="size-opt"
            :class="{ active: size === s }"
            @click="size = s"
          >{{ s }}</text>
        </view>
      </view>
      <view class="config-row">
        <text class="config-label">题目来源</text>
        <view class="size-options">
          <text
            v-for="src in sources"
            :key="src.key"
            class="size-opt"
            :class="{ active: source === src.key }"
            @click="source = src.key as 'due' | 'all' | 'wrong_words'"
          >{{ src.label }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useSettingsStore } from '@/stores/settings'
import { http } from '@/utils/request'

const settingsStore = useSettingsStore()
const size = ref(20)
const source = ref<'due' | 'all' | 'wrong_words'>('due')

const modes = [
  { key: 'spelling', emoji: '✏️', name: '拼写测试', desc: '看中文释义，拼出英文单词', page: '/pages/test/spelling' },
  { key: 'choice',   emoji: '📋', name: '选择题',   desc: '四选一词义匹配',             page: '/pages/test/choice' },
  { key: 'listening',emoji: '🎧', name: '听写模式', desc: '听发音，写出单词',            page: '/pages/test/listening' },
]

const sources = [
  { key: 'due',        label: '到期' },
  { key: 'all',        label: '全部' },
  { key: 'wrong_words',label: '错词' },
]

async function selectMode(mode: typeof modes[number]) {
  const level = settingsStore.settings.active_levels[0] ?? 'CET4'
  uni.showLoading({ title: '生成题目...' })
  try {
    const data = await http.post<{ test_id: string }>('/test/generate', {
      level_code: level,
      mode: mode.key,
      size: size.value,
      source: source.value,
    })
    uni.navigateTo({ url: `${mode.page}?test_id=${data.test_id}&level=${level}` })
  } finally {
    uni.hideLoading()
  }
}
</script>

<style lang="scss" scoped>
.test-home {
  min-height: 100vh;
  background: $color-bg-page;
  padding: $space-lg;
  padding-bottom: calc(#{$tabbar-height} + $space-lg);
}

.page-title { font-size: $font-xl; font-weight: 700; color: $color-text-primary; display: block; margin-bottom: 4rpx; }
.page-sub { font-size: $font-sm; color: $color-text-secondary; display: block; margin-bottom: $space-xl; }

.mode-list { display: flex; flex-direction: column; gap: $space-md; margin-bottom: $space-lg; }
.mode-card {
  background: #fff;
  border-radius: $radius-xl;
  padding: $space-lg;
  display: flex;
  align-items: center;
  gap: $space-md;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.05);
}
.mode-emoji { font-size: 72rpx; flex-shrink: 0; }
.mode-info { flex: 1; }
.mode-name { font-size: $font-lg; font-weight: 600; color: $color-text-primary; display: block; }
.mode-desc { font-size: $font-sm; color: $color-text-secondary; margin-top: 4rpx; display: block; }
.mode-arrow { font-size: $font-xl; color: $color-text-placeholder; }

.config-card {
  background: #fff;
  border-radius: $radius-xl;
  padding: $space-lg;
  .config-title { font-size: $font-md; font-weight: 600; color: $color-text-primary; display: block; margin-bottom: $space-md; }
}
.config-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: $space-sm; }
.config-label { font-size: $font-sm; color: $color-text-regular; }
.size-options { display: flex; gap: $space-sm; }
.size-opt {
  padding: 8rpx 24rpx;
  border-radius: $radius-md;
  font-size: $font-sm;
  color: $color-text-secondary;
  background: $color-bg-page;
  border: 2rpx solid transparent;
  &.active { color: $color-primary; border-color: $color-primary; background: #fff; }
}
</style>
