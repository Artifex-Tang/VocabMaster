<template>
  <view class="detail">
    <view v-if="data" class="header">
      <view class="head-emoji">📘</view>
      <view class="head-info">
        <text class="head-name">{{ data.name }}</text>
        <text class="head-meta">{{ data.word_count }} 词 · {{ data.unit_count }} 单元</text>
        <text v-if="data.description" class="head-desc">{{ data.description }}</text>
      </view>
    </view>

    <view v-if="data && !data.subscribed" class="action-bar">
      <button class="btn-sub" @click="doSubscribe">订阅词库</button>
    </view>

    <view v-if="data && data.origin_level_code && data.subscribed" class="action-bar">
      <button class="btn-review" @click="goReview">复习到期词</button>
    </view>

    <view v-if="data && data.subscribed" class="units">
      <text class="section-title">单元进度</text>
      <view class="unit-grid">
        <view
          v-for="u in data.units"
          :key="u.unit_no"
          class="unit-card"
          :class="{ current: u.is_current }"
          @click="goLearn(u.unit_no)"
        >
          <view class="unit-head">
            <text class="unit-no">Unit {{ u.unit_no }}</text>
            <text v-if="u.is_current" class="unit-tag">当前</text>
            <text v-else-if="u.completed" class="unit-tag done">✓</text>
          </view>
          <view class="bar"><view class="bar-inner" :style="{ width: pct(u) + '%' }" /></view>
          <text class="unit-meta">{{ u.learned_count }}/{{ u.total_count }} 已学</text>
        </view>
      </view>
    </view>

    <view v-else-if="loading" class="loading-tip">加载中...</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { detail as getDetail, subscribe } from '@/api/wordList'
import type { WordListDetail, UnitSummary } from '@/api/types'

const id = ref(0)
const data = ref<WordListDetail | null>(null)
const loading = ref(true)

onLoad((opts) => {
  id.value = Number(opts?.id ?? 0)
})
onShow(loadData)

async function loadData() {
  if (!id.value) return
  loading.value = true
  try {
    data.value = await getDetail(id.value)
  } catch {
    // request 已 toast
  } finally {
    loading.value = false
  }
}

function pct(u: UnitSummary) {
  if (!u.total_count) return 0
  return Math.round((u.learned_count / u.total_count) * 100)
}

function goLearn(unit: number) {
  uni.navigateTo({ url: `/pages/wordlists/learn?id=${id.value}&unit=${unit}` })
}

function goReview() {
  const level = data.value?.origin_level_code
  if (!level) return
  // test/index 是 tabBar 页，switchTab 不带 query → storage 中转
  uni.setStorageSync('test_entry_override', { level, source: 'due' })
  uni.switchTab({ url: '/pages/test/index' })
}

async function doSubscribe() {
  uni.showLoading({ title: '订阅中...' })
  try {
    await subscribe(id.value)
    uni.hideLoading()
    await loadData()
  } catch {
    uni.hideLoading()
  }
}
</script>

<style lang="scss" scoped>
.detail {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32rpx;
}
.header {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.05);
}
.head-emoji { font-size: 88rpx; flex-shrink: 0; }
.head-info { flex: 1; }
.head-name { font-size: 32rpx; font-weight: 700; color: #1f2937; display: block; }
.head-meta { font-size: 24rpx; color: #9ca3af; display: block; margin-top: 4rpx; }
.head-desc { font-size: 24rpx; color: #6b7280; display: block; margin-top: 8rpx; }

.action-bar { margin-bottom: 24rpx; }
.btn-sub, .btn-review {
  width: 100%;
  height: 88rpx;
  border-radius: 44rpx;
  border: none;
  font-size: 30rpx;
  font-weight: 600;
}
.btn-sub { background: #1890ff; color: #fff; }
.btn-review { background: #f0f9eb; color: #52c41a; border: 2rpx solid #52c41a; }

.section-title { font-size: 28rpx; font-weight: 600; color: #1f2937; display: block; margin-bottom: 16rpx; }
.unit-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
}
.unit-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.05);
  &.current { border: 2rpx solid #1890ff; }
}
.unit-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12rpx; }
.unit-no { font-size: 26rpx; font-weight: 600; color: #1f2937; }
.unit-tag {
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: 12rpx;
  background: #1890ff;
  color: #fff;
  &.done { background: #52c41a; }
}
.bar { height: 8rpx; background: #e5e7eb; border-radius: 4rpx; overflow: hidden; }
.bar-inner { height: 100%; background: #1890ff; border-radius: 4rpx; transition: width 0.3s; }
.unit-meta { font-size: 22rpx; color: #9ca3af; display: block; margin-top: 8rpx; }

.loading-tip { text-align: center; color: #9ca3af; font-size: 26rpx; padding: 80rpx 0; }
</style>
