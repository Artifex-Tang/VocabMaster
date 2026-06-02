<template>
  <view class="stats">
    <!-- 周期切换 -->
    <view class="period-tabs">
      <text
        v-for="p in periods"
        :key="p.key"
        class="period-tab"
        :class="{ active: period === p.key }"
        @click="setPeriod(p.key)"
      >{{ p.label }}</text>
    </view>

    <!-- 今日统计 -->
    <view class="today-card" v-if="todayStats">
      <text class="card-title">今日数据</text>
      <view class="num-row">
        <view class="num-item">
          <text class="num">{{ todayStats.words_learned }}</text>
          <text class="num-label">新学</text>
        </view>
        <view class="num-item">
          <text class="num">{{ todayStats.words_reviewed }}</text>
          <text class="num-label">复习</text>
        </view>
        <view class="num-item">
          <text class="num">{{ Math.round(todayStats.accuracy * 100) }}%</text>
          <text class="num-label">正确率</text>
        </view>
        <view class="num-item">
          <text class="num">{{ Math.round(todayStats.duration_seconds / 60) }}</text>
          <text class="num-label">分钟</text>
        </view>
      </view>
    </view>

    <!-- 核心数字 -->
    <view class="summary-card" v-if="summary">
      <text class="card-title">{{ period === 'week' ? '本周' : '本月' }}汇总</text>
      <view class="num-row">
        <view class="num-item">
          <text class="num">{{ summary.total_learned }}</text>
          <text class="num-label">新学</text>
        </view>
        <view class="num-item">
          <text class="num">{{ summary.total_reviewed }}</text>
          <text class="num-label">复习</text>
        </view>
        <view class="num-item">
          <text class="num">{{ Math.round(summary.avg_accuracy * 100) }}%</text>
          <text class="num-label">正确率</text>
        </view>
        <view class="num-item">
          <text class="num">{{ summary.days_active }}</text>
          <text class="num-label">学习天</text>
        </view>
      </view>
    </view>

    <!-- 每日明细柱状图 -->
    <view class="daily-card">
      <text class="card-title">每日学习</text>
      <view class="daily-list" v-if="summary">
        <view v-for="day in summary.daily_breakdown.slice(-14)" :key="day.date" class="daily-row">
          <text class="daily-date">{{ day.date.slice(5) }}</text>
          <view class="daily-bar-wrap">
            <view class="daily-bar" :style="{ width: barWidth(day.learned + day.reviewed) + '%' }" />
          </view>
          <text class="daily-num">{{ day.learned + day.reviewed }}</text>
        </view>
      </view>
    </view>

    <!-- 等级掌握概览 -->
    <view class="level-card">
      <text class="card-title">等级进度</text>
      <view v-if="summary" class="level-list">
        <view v-for="l in summary.level_breakdown" :key="l.level_code" class="level-row">
          <text class="level-code">{{ l.level_code }}</text>
          <view class="level-bar-wrap">
            <view
              class="level-bar"
              :style="{ width: Math.round((l.mastered / (l.mastered + l.learning || 1)) * 100) + '%' }"
            />
          </view>
          <text class="level-num">{{ l.mastered }}/{{ l.mastered + l.learning }}</text>
        </view>
      </view>
    </view>

    <!-- 遗忘曲线入口 -->
    <button class="btn-ghost" @click="goForgettingCurve">查看遗忘曲线</button>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getSummary, getTodayStats } from '@/api/stats'
import { formatDate } from '@/utils/date'
import type { SummaryStats, TodayStats } from '@/api/types'

const period = ref<'week' | 'month'>('week')
const summary = ref<SummaryStats | null>(null)
const todayStats = ref<TodayStats | null>(null)

const periods = [
  { key: 'week' as const, label: '本周' },
  { key: 'month' as const, label: '本月' },
]

const maxDaily = computed(() => {
  if (!summary.value) return 1
  return Math.max(...summary.value.daily_breakdown.map(d => d.learned + d.reviewed), 1)
})

function barWidth(total: number): number {
  return Math.round((total / maxDaily.value) * 100)
}

onShow(loadData)

async function loadData() {
  try {
    const [s, t] = await Promise.all([
      getSummary(period.value, formatDate(new Date())),
      getTodayStats(),
    ])
    summary.value = s
    todayStats.value = t
  } catch {
    // 静默
  }
}

async function setPeriod(p: 'week' | 'month') {
  period.value = p
  try {
    summary.value = await getSummary(period.value, formatDate(new Date()))
  } catch {
    // 静默
  }
}

function goForgettingCurve() {
  uni.navigateTo({ url: '/pages/stats/forgetting-curve' })
}
</script>

<style lang="scss" scoped>
.stats {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32rpx;
  padding-bottom: calc(120rpx + 32rpx);
}

.period-tabs {
  display: flex; background: #fff; border-radius: 16rpx;
  padding: 6rpx; gap: 6rpx; margin-bottom: 24rpx;
}
.period-tab {
  flex: 1; text-align: center; padding: 16rpx 0;
  border-radius: 12rpx; font-size: 24rpx; color: #9ca3af;
  &.active { background: #1890ff; color: #fff; font-weight: 600; }
}

.today-card, .summary-card {
  background: #fff; border-radius: 24rpx; padding: 32rpx;
  margin-bottom: 16rpx;
}
.card-title { font-size: 28rpx; font-weight: 600; color: #1f2937; display: block; margin-bottom: 24rpx; }
.num-row { display: flex; justify-content: space-around; }
.num-item { text-align: center; }
.num { font-size: 36rpx; font-weight: 700; color: #1890ff; display: block; }
.num-label { font-size: 20rpx; color: #9ca3af; }

.daily-card, .level-card {
  background: #fff; border-radius: 24rpx; padding: 32rpx; margin-bottom: 16rpx;
}

.daily-row {
  display: flex; align-items: center; gap: 12rpx; margin-bottom: 12rpx;
  .daily-date { font-size: 20rpx; color: #9ca3af; width: 72rpx; }
  .daily-bar-wrap { flex: 1; height: 20rpx; background: #f5f7fa; border-radius: 10rpx; overflow: hidden; }
  .daily-bar { height: 100%; background: #1890ff; border-radius: 10rpx; }
  .daily-num { font-size: 20rpx; color: #9ca3af; width: 64rpx; text-align: right; }
}

.level-row {
  display: flex; align-items: center; gap: 12rpx; margin-bottom: 12rpx;
  .level-code { font-size: 20rpx; font-weight: 600; color: #1f2937; width: 80rpx; }
  .level-bar-wrap { flex: 1; height: 20rpx; background: #f5f7fa; border-radius: 10rpx; overflow: hidden; }
  .level-bar { height: 100%; background: #52c41a; border-radius: 10rpx; }
  .level-num { font-size: 20rpx; color: #9ca3af; width: 100rpx; text-align: right; }
}

.btn-ghost {
  width: 100%; height: 88rpx; background: transparent; color: #1890ff;
  font-size: 28rpx; border-radius: 48rpx; border: 2rpx solid #1890ff;
  margin-top: 16rpx;
}
</style>
