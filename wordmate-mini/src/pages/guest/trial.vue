<template>
  <view class="trial">
    <!-- 顶部进度 -->
    <view class="header">
      <text class="title">免费试学</text>
      <text v-if="!finished" class="counter">{{ idx + 1 }} / {{ words.length }}</text>
    </view>
    <view v-if="!finished" class="progress-bar">
      <view class="progress-inner" :style="{ width: ((idx + 1) / words.length) * 100 + '%' }" />
    </view>

    <!-- 单词卡 -->
    <template v-if="!finished">
      <view class="card-area">
        <word-card v-if="current" :key="current.id" :word="current" @flip="flipped = true" />
        <view v-else-if="loading" class="loading-tip">加载中...</view>
        <view v-else class="loading-tip">词库加载失败，请稍后再试</view>
      </view>
      <view class="flip-hint"><text>点击卡片翻面、听发音</text></view>

      <button class="btn-next" :disabled="!flipped" @click="next">
        {{ idx + 1 < words.length ? '下一个 →' : '完成试学' }}
      </button>
    </template>

    <!-- 试学完成：登录引导 -->
    <view v-else class="done-card">
      <text class="done-emoji">🎉</text>
      <text class="done-title">试学完成！</text>
      <text class="done-desc">登录后保存学习进度，解锁艾宾浩斯复习计划、测试与统计</text>
      <button class="btn-login" @click="goLogin">登录，保存我的进度</button>
      <button class="btn-again" @click="restart">再试 5 个词</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { searchWords } from '@/api/word'
import type { Word } from '@/api/types'

/** 试学词数上限（小步体验，足够感受翻卡/发音） */
const TRIAL_SIZE = 5

const words = ref<Word[]>([])
const idx = ref(0)
const flipped = ref(false)
const loading = ref(true)
const finished = ref(false)

const current = computed(() => words.value[idx.value] ?? null)

onLoad(load)

async function load() {
  loading.value = true
  try {
    // 游客可调的只读词库接口；不挑词，按等级取前 N 个
    const res = await searchWords('', 'CET4', 1, 20)
    words.value = res.items.slice(0, TRIAL_SIZE)
  } catch {
    words.value = []
  } finally {
    loading.value = false
  }
}

function next() {
  if (!flipped.value) return
  if (idx.value + 1 < words.value.length) {
    idx.value++
    flipped.value = false
  } else {
    finished.value = true
  }
}

function restart() {
  idx.value = 0
  flipped.value = false
  finished.value = false
  load()
}

function goLogin() {
  uni.reLaunch({ url: '/pages/auth/login' })
}
</script>

<style lang="scss" scoped>
.trial {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32rpx;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;

  .title { font-size: 36rpx; font-weight: 700; color: #1f2937; }
  .counter { font-size: 28rpx; color: #6b7280; }
}

.progress-bar {
  height: 12rpx;
  background: #e5e7eb;
  border-radius: 6rpx;
  overflow: hidden;
  margin-bottom: 24rpx;

  .progress-inner { height: 100%; background: #1890ff; border-radius: 6rpx; transition: width 0.3s; }
}

.card-area { min-height: 640rpx; }
.loading-tip { text-align: center; color: #9ca3af; padding: 80rpx 0; font-size: 28rpx; }

.flip-hint { text-align: center; margin-top: 16rpx; text { font-size: 24rpx; color: #9ca3af; } }

.btn-next {
  margin-top: 32rpx;
  height: 92rpx;
  background: #1890ff;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 46rpx;
  border: none;
  &[disabled] { opacity: 0.4; }
}

.done-card {
  margin-top: 120rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;

  .done-emoji { font-size: 96rpx; }
  .done-title { font-size: 40rpx; font-weight: 700; color: #1f2937; }
  .done-desc { font-size: 26rpx; color: #6b7280; text-align: center; margin-bottom: 16rpx; }

  .btn-login {
    width: 100%; height: 92rpx; background: #1890ff; color: #fff;
    font-size: 32rpx; font-weight: 600; border-radius: 46rpx; border: none;
  }
  .btn-again {
    width: 100%; height: 88rpx; background: transparent; color: #1890ff;
    font-size: 28rpx; border-radius: 44rpx; border: 2rpx solid #1890ff;
  }
}
</style>
