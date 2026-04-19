<template>
  <view class="done">
    <text class="emoji-big">🎉</text>
    <text class="title">今日完成！</text>
    <text class="sub">共学 {{ total }} 词，答对 {{ correct }} 词</text>

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
    <button class="btn-ghost" @click="goWrongBook">查看错词本</button>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'

const correct = ref(0)
const total = ref(0)

onLoad((opts) => {
  correct.value = Number(opts?.correct ?? 0)
  total.value = Number(opts?.total ?? 0)
})

const accuracy = computed(() => {
  if (!total.value) return '-'
  return `${Math.round((correct.value / total.value) * 100)}%`
})

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

function goWrongBook() {
  uni.navigateTo({ url: '/pages/wrong-book/list' })
}
</script>

<style lang="scss" scoped>
.done {
  min-height: 100vh;
  background: $color-bg-page;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $space-2xl;
  gap: $space-lg;
}

.emoji-big { font-size: 160rpx; line-height: 1; }
.title { font-size: $font-2xl; font-weight: 800; color: $color-text-primary; }
.sub { font-size: $font-md; color: $color-text-secondary; }

.stat-row {
  display: flex;
  gap: $space-2xl;
  background: #fff;
  border-radius: $radius-xl;
  padding: $space-lg $space-2xl;
  margin: $space-md 0;
}
.stat-item { text-align: center; }
.stat-num { font-size: $font-2xl; font-weight: 700; color: $color-primary; display: block; }
.stat-num.danger { color: $color-danger; }
.stat-label { font-size: $font-sm; color: $color-text-secondary; }

.btn-primary {
  width: 100%;
  height: 96rpx;
  background: $color-primary;
  color: #fff;
  font-size: $font-lg;
  font-weight: 600;
  border-radius: $radius-xl;
  border: none;
}
.btn-ghost {
  width: 100%;
  height: 96rpx;
  background: transparent;
  color: $color-primary;
  font-size: $font-md;
  border-radius: $radius-xl;
  border: 2rpx solid $color-primary;
}
</style>
