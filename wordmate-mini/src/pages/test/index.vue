<template>
  <view class="test-home">
    <text class="page-title">测试练习</text>
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

      <!-- 等级选择 -->
      <view class="config-row">
        <text class="config-label">等级</text>
        <picker :range="levelNames" :value="levelIdx" @change="onLevelChange">
          <text class="picker-val">{{ selectedLevelName }} ▾</text>
        </picker>
      </view>

      <!-- 题目数量 -->
      <view class="config-row">
        <text class="config-label">题目数量</text>
        <view class="size-options">
          <text
            v-for="s in [5, 10, 20, 30]"
            :key="s"
            class="size-opt"
            :class="{ active: size === s }"
            @click="size = s"
          >{{ s }}</text>
        </view>
      </view>

      <!-- 题目来源 -->
      <view class="config-row">
        <text class="config-label">题目来源</text>
        <view class="size-options">
          <text
            v-for="src in sources"
            :key="src.key"
            class="size-opt"
            :class="{ active: source === src.key, disabled: !src.available }"
            @click="src.available && (source = src.key)"
          >{{ src.label }}{{ src.count != null ? ` (${src.count})` : '' }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useSettingsStore } from '@/stores/settings'
import { generate, availability } from '@/api/test'
import { LEVELS } from '@/api/types'
import type { TestAvailability } from '@/api/types'

const settingsStore = useSettingsStore()
const size = ref(20)
const source = ref<'due' | 'all' | 'wrong_words'>('all')
const avail = ref<TestAvailability | null>(null)

const activeLevels = computed(() => settingsStore.settings.active_levels)
const levelIdx = ref(0)
const selectedLevel = computed(() => activeLevels.value[levelIdx.value] ?? LEVELS[0].code)
const selectedLevelName = computed(() => {
  const l = LEVELS.find(lv => lv.code === selectedLevel.value)
  return l ? `${l.name_en}` : selectedLevel.value
})
const levelNames = computed(() => activeLevels.value.map(code => {
  const l = LEVELS.find(lv => lv.code === code)
  return l ? `${l.name_en} (${l.name_zh})` : code
}))

const modes = [
  { key: 'spelling', emoji: '✏️', name: '拼写测试', desc: '看中文释义，拼出英文单词', page: '/pages/test/spelling' },
  { key: 'choice', emoji: '📋', name: '选择题', desc: '四选一词义匹配', page: '/pages/test/choice' },
  { key: 'listening', emoji: '🎧', name: '听写模式', desc: '听发音，写出单词', page: '/pages/test/listening' },
]

const sources = computed(() => [
  { key: 'all' as const, label: '全部', count: avail.value?.all, available: (avail.value?.all ?? 0) > 0 },
  { key: 'due' as const, label: '待复习', count: avail.value?.due, available: (avail.value?.due ?? 0) > 0 },
  { key: 'wrong_words' as const, label: '错词', count: avail.value?.wrong_words, available: (avail.value?.wrong_words ?? 0) > 0 },
])

onMounted(loadAvailability)

async function loadAvailability() {
  try {
    avail.value = await availability(selectedLevel.value)
    // 如果当前来源无数据，回退到"全部"
    if (source.value !== 'all' && avail.value) {
      const src = sources.value.find(s => s.key === source.value)
      if (!src?.available) source.value = 'all'
    }
  } catch {
    // 静默
  }
}

function onLevelChange(e: { detail: { value: number } }) {
  levelIdx.value = e.detail.value
  loadAvailability()
}

async function selectMode(mode: typeof modes[number]) {
  uni.showLoading({ title: '生成题目...' })
  try {
    const data = await generate(mode.key, selectedLevel.value, source.value, size.value)
    uni.navigateTo({
      url: `${mode.page}?test_id=${data.test_id}&level=${selectedLevel.value}`,
    })
  } finally {
    uni.hideLoading()
  }
}
</script>

<style lang="scss" scoped>
.test-home {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32rpx;
  padding-bottom: calc(120rpx + 32rpx);
}

.page-title { font-size: 36rpx; font-weight: 700; color: #1f2937; display: block; margin-bottom: 4rpx; }
.page-sub { font-size: 24rpx; color: #9ca3af; display: block; margin-bottom: 32rpx; }

.mode-list { display: flex; flex-direction: column; gap: 16rpx; margin-bottom: 32rpx; }
.mode-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.05);
}
.mode-emoji { font-size: 72rpx; flex-shrink: 0; }
.mode-info { flex: 1; }
.mode-name { font-size: 32rpx; font-weight: 600; color: #1f2937; display: block; }
.mode-desc { font-size: 24rpx; color: #9ca3af; margin-top: 4rpx; display: block; }
.mode-arrow { font-size: 36rpx; color: #d1d5db; }

.config-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  .config-title { font-size: 28rpx; font-weight: 600; color: #1f2937; display: block; margin-bottom: 24rpx; }
}
.config-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx; }
.config-label { font-size: 24rpx; color: #4b5563; }
.picker-val { font-size: 24rpx; color: #1890ff; }
.size-options { display: flex; gap: 12rpx; }
.size-opt {
  padding: 8rpx 20rpx;
  border-radius: 12rpx;
  font-size: 24rpx;
  color: #9ca3af;
  background: #f5f7fa;
  border: 2rpx solid transparent;
  &.active { color: #1890ff; border-color: #1890ff; background: #fff; }
  &.disabled { opacity: 0.4; }
}
</style>
