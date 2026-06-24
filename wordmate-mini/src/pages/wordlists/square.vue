<template>
  <view class="square">
    <text class="page-title">教材词库</text>
    <text class="page-sub">选一本教材，按单元循序渐进</text>

    <view v-if="loading" class="loading-tip">加载中...</view>
    <view v-else-if="!lists.length" class="loading-tip">暂无词库</view>

    <view class="list-grid">
      <view v-for="l in lists" :key="l.id" class="wl-card" @click="onTap(l)">
        <view class="card-emoji">{{ l.cover_emoji || '📘' }}</view>
        <text class="card-name">{{ l.name }}</text>
        <text class="card-meta">{{ l.word_count }} 词</text>
        <text v-if="l.description" class="card-desc">{{ l.description }}</text>
        <view class="card-btn" :class="{ subscribed: l.subscribed }">
          {{ l.subscribed ? '已订阅 · 进入' : '订阅' }}
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { square, subscribe } from '@/api/wordList'
import type { WordListSummary } from '@/api/types'

const lists = ref<WordListSummary[]>([])
const loading = ref(true)

onShow(loadLists)

async function loadLists() {
  loading.value = true
  try {
    lists.value = await square()
  } catch {
    // request 已 toast
  } finally {
    loading.value = false
  }
}

async function onTap(l: WordListSummary) {
  if (l.subscribed) {
    uni.navigateTo({ url: `/pages/wordlists/detail?id=${l.id}` })
    return
  }
  uni.showLoading({ title: '订阅中...' })
  try {
    await subscribe(l.id)
    l.subscribed = true
    uni.hideLoading()
    uni.navigateTo({ url: `/pages/wordlists/detail?id=${l.id}` })
  } catch {
    uni.hideLoading()
  }
}
</script>

<style lang="scss" scoped>
.square {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32rpx;
}
.page-title { font-size: 36rpx; font-weight: 700; color: #1f2937; display: block; margin-bottom: 4rpx; }
.page-sub { font-size: 24rpx; color: #9ca3af; display: block; margin-bottom: 32rpx; }
.loading-tip { text-align: center; color: #9ca3af; font-size: 26rpx; padding: 80rpx 0; }

.list-grid { display: flex; flex-direction: column; gap: 24rpx; }
.wl-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}
.card-emoji { font-size: 96rpx; line-height: 1; margin-bottom: 8rpx; }
.card-name { font-size: 32rpx; font-weight: 600; color: #1f2937; }
.card-meta { font-size: 24rpx; color: #9ca3af; }
.card-desc { font-size: 24rpx; color: #6b7280; text-align: center; margin-top: 4rpx; }
.card-btn {
  margin-top: 16rpx;
  padding: 12rpx 40rpx;
  border-radius: 32rpx;
  font-size: 26rpx;
  background: #1890ff;
  color: #fff;
  &.subscribed { background: #f0f9eb; color: #52c41a; }
}
</style>
