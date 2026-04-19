<template>
  <view class="dashboard">
    <view class="header">
      <view>
        <text class="greeting">{{ greeting }}，{{ userStore.userInfo?.nickname ?? '同学' }}</text>
        <text class="date">{{ todayStr }}</text>
      </view>
      <view class="level-tag" @click="showLevelPicker = true">
        {{ currentLevel }} ▾
      </view>
    </view>

    <!-- 今日进度环 -->
    <view class="progress-card">
      <view class="ring-wrap">
        <view class="ring-text">
          <text class="ring-num">{{ todayStats?.words_learned ?? 0 }}</text>
          <text class="ring-label">已学</text>
        </view>
      </view>
      <view class="progress-detail">
        <view class="detail-row">
          <text class="detail-label">新词</text>
          <text class="detail-val">{{ todayStats?.goal_progress?.new ?? `0/${settings.daily_new_words_goal}` }}</text>
        </view>
        <view class="detail-row">
          <text class="detail-label">复习</text>
          <text class="detail-val">{{ todayStats?.goal_progress?.review ?? `0/${settings.daily_review_goal}` }}</text>
        </view>
        <view class="detail-row">
          <text class="detail-label">正确率</text>
          <text class="detail-val accent">{{ accuracy }}</text>
        </view>
      </view>
    </view>

    <!-- 学习计划提示 -->
    <view v-if="plan" class="plan-card">
      <text class="plan-text">今日待复习 <text class="accent">{{ plan.review_count }}</text> 词，新词 <text class="accent">{{ plan.new_count }}</text> 词</text>
      <text class="plan-time">预计 {{ plan.estimated_minutes }} 分钟</text>
    </view>

    <!-- 开始学习按钮 -->
    <button class="btn-start" :disabled="loading" @click="startStudy">
      {{ loading ? '加载中...' : '开始学习' }}
    </button>

    <!-- 打卡日历 -->
    <view v-if="calendar" class="calendar-card">
      <text class="card-title">本月打卡</text>
      <text class="streak">连续打卡 <text class="accent">{{ calendar.current_streak }}</text> 天</text>
      <view class="calendar-grid">
        <view
          v-for="day in calendar.days"
          :key="day.date"
          class="cal-day"
          :class="{ 'checked': day.checked_in, 'today': day.date === todayStr }"
        >
          <text class="cal-num">{{ day.date.slice(-2) }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'
import { getToday } from '@/api/study'
import { getTodayStats, getCalendar, checkinToday } from '@/api/stats'
import { formatDate } from '@/utils/date'
import type { TodayPlan, TodayStats, CalendarData } from '@/api/types'

const userStore = useUserStore()
const settingsStore = useSettingsStore()
const settings = computed(() => settingsStore.settings)

const plan = ref<TodayPlan | null>(null)
const todayStats = ref<TodayStats | null>(null)
const calendar = ref<CalendarData | null>(null)
const loading = ref(false)
const showLevelPicker = ref(false)

const todayStr = formatDate(new Date())
const currentLevel = computed(() => settings.value.active_levels[0] ?? 'CET4')

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const accuracy = computed(() => {
  if (!todayStats.value) return '-'
  return `${Math.round(todayStats.value.accuracy * 100)}%`
})

onMounted(loadData)
onShow(loadData)

async function loadData() {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  try {
    const [p, s, c] = await Promise.all([
      getToday(currentLevel.value),
      getTodayStats(),
      getCalendar(todayStr.slice(0, 7)),
    ])
    plan.value = p
    todayStats.value = s
    calendar.value = c
    await checkinToday()
  } catch {
    // 网络错误时静默失败，使用缓存数据
  }
}

async function startStudy() {
  if (!plan.value || loading.value) return
  loading.value = true
  try {
    uni.navigateTo({ url: `/pages/study/session?level=${currentLevel.value}` })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.dashboard {
  min-height: 100vh;
  background: $color-bg-page;
  padding: $space-lg;
  padding-bottom: calc(#{$tabbar-height} + $space-lg);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: $space-lg;

  .greeting { font-size: $font-xl; font-weight: 700; color: $color-text-primary; display: block; }
  .date { font-size: $font-sm; color: $color-text-secondary; margin-top: 4rpx; display: block; }
  .level-tag {
    background: $color-primary;
    color: #fff;
    padding: 8rpx 20rpx;
    border-radius: $radius-lg;
    font-size: $font-sm;
  }
}

.progress-card {
  background: #fff;
  border-radius: $radius-xl;
  padding: $space-lg;
  display: flex;
  align-items: center;
  gap: $space-lg;
  margin-bottom: $space-md;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.05);

  .ring-wrap {
    width: 160rpx;
    height: 160rpx;
    border-radius: 50%;
    border: 12rpx solid $color-primary;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }
  .ring-text { text-align: center; }
  .ring-num { font-size: $font-2xl; font-weight: 700; color: $color-primary; display: block; }
  .ring-label { font-size: $font-xs; color: $color-text-secondary; }

  .progress-detail { flex: 1; }
  .detail-row { display: flex; justify-content: space-between; margin-bottom: 12rpx; }
  .detail-label { font-size: $font-sm; color: $color-text-secondary; }
  .detail-val { font-size: $font-sm; color: $color-text-primary; font-weight: 500; }
  .detail-val.accent { color: $color-success; }
}

.plan-card {
  background: #fff;
  border-radius: $radius-lg;
  padding: $space-md;
  margin-bottom: $space-md;

  .plan-text { font-size: $font-md; color: $color-text-regular; display: block; }
  .plan-time { font-size: $font-sm; color: $color-text-secondary; margin-top: 8rpx; display: block; }
}

.accent { color: $color-primary; font-weight: 600; }

.btn-start {
  width: 100%;
  height: 96rpx;
  background: $color-primary;
  color: #fff;
  font-size: $font-lg;
  font-weight: 600;
  border-radius: $radius-xl;
  border: none;
  margin-bottom: $space-lg;

  &[disabled] { opacity: 0.6; }
}

.calendar-card {
  background: #fff;
  border-radius: $radius-xl;
  padding: $space-lg;

  .card-title { font-size: $font-md; font-weight: 600; color: $color-text-primary; display: block; margin-bottom: $space-xs; }
  .streak { font-size: $font-sm; color: $color-text-secondary; display: block; margin-bottom: $space-md; }
  .calendar-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 8rpx;
  }
  .cal-day {
    aspect-ratio: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: $radius-sm;
    background: $color-bg-page;
    &.checked { background: $color-primary; }
    &.today { border: 2rpx solid $color-primary; }
  }
  .cal-num { font-size: $font-xs; color: $color-text-secondary; }
  .cal-day.checked .cal-num { color: #fff; }
}
</style>
