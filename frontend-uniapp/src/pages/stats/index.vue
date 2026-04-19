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

    <!-- 核心数字 -->
    <view class="summary-card" v-if="summary">
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
          <text class="num">{{ accuracy }}%</text>
          <text class="num-label">正确率</text>
        </view>
        <view class="num-item">
          <text class="num">{{ summary.days_active }}</text>
          <text class="num-label">学习天</text>
        </view>
      </view>
    </view>

    <!-- 每日明细柱状图（简单文字版，待接入 uCharts） -->
    <view class="daily-card">
      <text class="card-title">每日学习</text>
      <view class="daily-list" v-if="summary">
        <view v-for="day in summary.daily_breakdown.slice(-7)" :key="day.date" class="daily-row">
          <text class="daily-date">{{ day.date.slice(5) }}</text>
          <view class="daily-bar-wrap">
            <view class="daily-bar" :style="{ width: barWidth(day.learned) + 'rpx' }" />
          </view>
          <text class="daily-num">{{ day.learned }}</text>
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
              :style="{ width: Math.round((l.mastered / (l.mastered + l.learning || 1)) * 200) + 'rpx' }"
            />
          </view>
          <text class="level-num">{{ l.mastered }} 已掌握</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getSummary } from '@/api/stats'
import { formatDate } from '@/utils/date'
import type { SummaryStats } from '@/api/types'

const period = ref<'week' | 'month'>('week')
const summary = ref<SummaryStats | null>(null)

const periods = [
  { key: 'week' as const, label: '本周' },
  { key: 'month' as const, label: '本月' },
]

const accuracy = computed(() =>
  summary.value ? Math.round(summary.value.avg_accuracy * 100) : 0,
)

const maxLearned = computed(() => {
  if (!summary.value) return 1
  return Math.max(...summary.value.daily_breakdown.map(d => d.learned), 1)
})

function barWidth(learned: number): number {
  return Math.round((learned / maxLearned.value) * 360)
}

onMounted(loadData)
onShow(loadData)

async function loadData() {
  try {
    summary.value = await getSummary(period.value, formatDate(new Date()))
  } catch {
    // 静默
  }
}

async function setPeriod(p: 'week' | 'month') {
  period.value = p
  await loadData()
}
</script>

<style lang="scss" scoped>
.stats {
  min-height: 100vh; background: $color-bg-page;
  padding: $space-lg;
  padding-bottom: calc(#{$tabbar-height} + $space-lg);
}

.period-tabs {
  display: flex; background: #fff; border-radius: $radius-lg;
  padding: 6rpx; gap: 6rpx; margin-bottom: $space-lg;
}
.period-tab {
  flex: 1; text-align: center; padding: $space-sm 0;
  border-radius: $radius-md; font-size: $font-sm; color: $color-text-secondary;
  &.active { background: $color-primary; color: #fff; font-weight: 600; }
}

.summary-card {
  background: #fff; border-radius: $radius-xl; padding: $space-lg;
  margin-bottom: $space-md;
}
.num-row { display: flex; justify-content: space-around; }
.num-item { text-align: center; }
.num { font-size: $font-2xl; font-weight: 700; color: $color-primary; display: block; }
.num-label { font-size: $font-xs; color: $color-text-secondary; }

.daily-card, .level-card {
  background: #fff; border-radius: $radius-xl; padding: $space-lg; margin-bottom: $space-md;
  .card-title { font-size: $font-md; font-weight: 600; color: $color-text-primary; display: block; margin-bottom: $space-md; }
}

.daily-row {
  display: flex; align-items: center; gap: $space-sm; margin-bottom: $space-sm;
  .daily-date { font-size: $font-xs; color: $color-text-secondary; width: 72rpx; }
  .daily-bar-wrap { flex: 1; height: 20rpx; background: $color-bg-page; border-radius: 10rpx; overflow: hidden; }
  .daily-bar { height: 100%; background: $color-primary; border-radius: 10rpx; }
  .daily-num { font-size: $font-xs; color: $color-text-secondary; width: 48rpx; text-align: right; }
}

.level-row {
  display: flex; align-items: center; gap: $space-sm; margin-bottom: $space-sm;
  .level-code { font-size: $font-xs; font-weight: 600; color: $color-text-primary; width: 80rpx; }
  .level-bar-wrap { flex: 1; height: 20rpx; background: $color-bg-page; border-radius: 10rpx; overflow: hidden; }
  .level-bar { height: 100%; background: $color-success; border-radius: 10rpx; }
  .level-num { font-size: $font-xs; color: $color-text-secondary; width: 128rpx; text-align: right; }
}
</style>
