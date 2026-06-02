<template>
  <view class="done">
    <text class="emoji-big">🎉</text>
    <text class="title">本次学习完成！</text>
    <text class="sub">共学 {{ total }} 词，答对 {{ correct }} 词</text>

    <!-- 进度环 -->
    <view class="ring-wrap" :style="{ borderColor: ringColor }">
      <text class="ring-pct" :style="{ color: ringColor }">{{ accuracy }}</text>
    </view>

    <view class="stat-row">
      <view class="stat-item">
        <text class="stat-num">{{ correct }}</text>
        <text class="stat-label">答对</text>
      </view>
      <view class="stat-item">
        <text class="stat-num danger">{{ total - correct }}</text>
        <text class="stat-label">答错</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ accuracy }}</text>
        <text class="stat-label">正确率</text>
      </view>
    </view>

    <button class="btn-primary" @click="goHome">返回首页</button>
    <button class="btn-ghost" @click="goStats">查看统计</button>
    <button class="btn-ghost" @click="goWrongBook">查看错词本</button>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const correct = ref(0)
const total = ref(0)

onLoad((opts) => {
  correct.value = Number(opts?.correct ?? 0)
  total.value = Number(opts?.total ?? 0)
})

const accuracy = computed(() => {
  if (!total.value) return '0%'
  return `${Math.round((correct.value / total.value) * 100)}%`
})

const ringColor = computed(() => {
  const pct = total.value ? correct.value / total.value : 0
  if (pct >= 0.8) return '#52c41a'
  if (pct >= 0.6) return '#faad14'
  return '#f56c6c'
})

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

function goStats() {
  uni.switchTab({ url: '/pages/stats/index' })
}

function goWrongBook() {
  uni.navigateTo({ url: '/pages/wrong-book/list' })
}
</script>

<style lang="scss" scoped>
.done {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48rpx;
  gap: 24rpx;
}

.emoji-big { font-size: 160rpx; line-height: 1; }
.title { font-size: 40rpx; font-weight: 800; color: #1f2937; }
.sub { font-size: 28rpx; color: #9ca3af; }

.ring-wrap {
  width: 200rpx;
  height: 200rpx;
  border-radius: 50%;
  border: 16rpx solid #52c41a;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 16rpx 0;
}
.ring-pct { font-size: 44rpx; font-weight: 700; }

.stat-row {
  display: flex;
  gap: 48rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx 48rpx;
  margin: 16rpx 0;
}
.stat-item { text-align: center; }
.stat-num { font-size: 36rpx; font-weight: 700; color: #1890ff; display: block; }
.stat-num.danger { color: #f56c6c; }
.stat-label { font-size: 24rpx; color: #9ca3af; }

.btn-primary {
  width: 100%;
  height: 96rpx;
  background: #1890ff;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 48rpx;
  border: none;
}
.btn-ghost {
  width: 100%;
  height: 96rpx;
  background: transparent;
  color: #1890ff;
  font-size: 28rpx;
  border-radius: 48rpx;
  border: 2rpx solid #1890ff;
}
</style>
