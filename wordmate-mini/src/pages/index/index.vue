<template>
  <view class="dashboard">
    <view class="header">
      <view>
        <text class="greeting">{{ greeting }}，{{ userStore.userInfo?.nickname ?? '同学' }}</text>
        <text class="date">{{ todayStr }}</text>
      </view>
      <picker :range="levelNames" @change="onLevelChange">
        <view class="level-tag">{{ currentLevelName }} ▾</view>
      </picker>
    </view>

    <!-- 今日进度 -->
    <view class="progress-card">
      <view class="ring-wrap">
        <view class="ring-text">
          <text class="ring-num">{{ progressPct }}%</text>
          <text class="ring-label">完成度</text>
        </view>
      </view>
      <view class="progress-detail">
        <view class="detail-row">
          <text class="detail-label">新词</text>
          <text class="detail-val">{{ todayStats?.goal_progress?.new ?? '0' }}</text>
        </view>
        <view class="detail-row">
          <text class="detail-label">复习</text>
          <text class="detail-val">{{ todayStats?.goal_progress?.review ?? '0' }}</text>
        </view>
        <view class="detail-row">
          <text class="detail-label">正确率</text>
          <text class="detail-val accent">{{ accuracy }}</text>
        </view>
      </view>
    </view>

    <!-- 学习计划 -->
    <view v-if="plan" class="plan-card">
      <text class="plan-text">
        待复习 <text class="accent">{{ plan.review_count }}</text> 词，新词 <text class="accent">{{ plan.new_count }}</text> 词
      </text>
      <text class="plan-time">预计 {{ plan.estimated_minutes }} 分钟</text>
    </view>

    <!-- 已完成 -->
    <view v-if="plan && plan.review_count + plan.new_count === 0" class="done-card">
      <text class="done-text">🎉 今日任务已完成！</text>
    </view>

    <!-- 开始学习 -->
    <button
      v-if="plan && plan.review_count + plan.new_count > 0"
      class="btn-start"
      :disabled="loading"
      @click="startStudy"
    >
      {{ loading ? '加载中...' : '开始学习' }}
    </button>

    <!-- 教材词库入口 -->
    <view class="wordlist-entry" @click="goWordlists">
      <text class="wl-emoji">📚</text>
      <view class="wl-info">
        <text class="wl-title">教材词库</text>
        <text class="wl-desc">《Think》系列，按单元循序渐进</text>
      </view>
      <text class="wl-arrow">›</text>
    </view>

    <!-- 打卡日历 -->
    <view v-if="calendar" class="calendar-card">
      <text class="card-title">本月打卡</text>
      <text class="streak">连续打卡 <text class="accent">{{ calendar.current_streak }}</text> 天</text>
      <view class="calendar-grid">
        <view
          v-for="day in calendar.days"
          :key="day.date"
          class="cal-day"
          :class="{ checked: day.checked_in, today: day.date === todayStr }"
        >
          <text class="cal-num">{{ day.date.slice(-2) }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'
import { useStudyStore } from '@/stores/study'
import { getToday } from '@/api/study'
import { getTodayStats, getCalendar, checkinToday } from '@/api/stats'
import { formatDate } from '@/utils/date'
import { LEVELS } from '@/api/types'
import type { TodayPlan, TodayStats, CalendarData } from '@/api/types'

const userStore = useUserStore()
const settingsStore = useSettingsStore()
const studyStore = useStudyStore()
const settings = computed(() => settingsStore.settings)

const plan = ref<TodayPlan | null>(null)
const todayStats = ref<TodayStats | null>(null)
const calendar = ref<CalendarData | null>(null)
const loading = ref(false)
const checkedIn = ref(false)

const todayStr = formatDate(new Date())

const activeLevels = computed(() => settings.value.active_levels)
const currentLevel = computed(() => activeLevels.value?.[0] || 'CET4')
const currentLevelName = computed(() => LEVELS.find(l => l.code === currentLevel.value)?.name_zh ?? currentLevel.value)
const levelNames = computed(() => LEVELS.map(l => `${l.name_en} (${l.name_zh})`))

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

const progressPct = computed(() => {
  if (!todayStats.value) return 0
  const learned = todayStats.value.words_learned + todayStats.value.words_reviewed
  const goal = settings.value.daily_new_words_goal + settings.value.daily_review_goal
  if (goal === 0) return 0
  return Math.min(100, Math.round((learned / goal) * 100))
})

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

    // 打卡（只打一次）
    if (!checkedIn.value) {
      try {
        await checkinToday()
        checkedIn.value = true
      } catch {
        // 已打卡过或失败，静默
      }
    }
  } catch {
    // 网络错误时静默
  }
}

function onLevelChange(e: { detail: { value: number } }) {
  const idx = e.detail.value
  const level = LEVELS[idx]
  if (level) {
    settingsStore.update({ active_levels: [level.code, ...activeLevels.value.filter(l => l !== level.code)] })
  }
}

function startStudy() {
  if (!plan.value || loading.value) return
  loading.value = true
  studyStore.initSession(plan.value, currentLevel.value)
  uni.navigateTo({ url: `/pages/study/session?level=${currentLevel.value}` })
  loading.value = false
}

function goWordlists() {
  uni.navigateTo({ url: '/pages/wordlists/square' })
}
</script>

<style lang="scss" scoped>
.dashboard {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32rpx;
  padding-bottom: calc(120rpx + 32rpx);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32rpx;
  .greeting { font-size: 36rpx; font-weight: 700; color: #1f2937; display: block; }
  .date { font-size: 24rpx; color: #9ca3af; margin-top: 4rpx; display: block; }
  .level-tag {
    background: #1890ff;
    color: #fff;
    padding: 8rpx 20rpx;
    border-radius: 16rpx;
    font-size: 24rpx;
  }
}

.progress-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  display: flex;
  align-items: center;
  gap: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.05);
  .ring-wrap {
    width: 160rpx;
    height: 160rpx;
    border-radius: 50%;
    border: 12rpx solid #1890ff;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }
  .ring-text { text-align: center; }
  .ring-num { font-size: 40rpx; font-weight: 700; color: #1890ff; display: block; }
  .ring-label { font-size: 20rpx; color: #9ca3af; }
  .progress-detail { flex: 1; }
  .detail-row { display: flex; justify-content: space-between; margin-bottom: 12rpx; }
  .detail-label { font-size: 24rpx; color: #9ca3af; }
  .detail-val { font-size: 24rpx; color: #1f2937; font-weight: 500; }
  .detail-val.accent { color: #52c41a; }
}

.plan-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  .plan-text { font-size: 28rpx; color: #4b5563; display: block; }
  .plan-time { font-size: 24rpx; color: #9ca3af; margin-top: 8rpx; display: block; }
}

.done-card {
  background: #f6ffed;
  border-radius: 16rpx;
  padding: 32rpx;
  text-align: center;
  margin-bottom: 24rpx;
  .done-text { font-size: 32rpx; color: #52c41a; }
}

.accent { color: #1890ff; font-weight: 600; }

.btn-start {
  width: 100%;
  height: 96rpx;
  background: #1890ff;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 48rpx;
  border: none;
  margin-bottom: 32rpx;
  &[disabled] { opacity: 0.6; }
}

.wordlist-entry {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.05);
  .wl-emoji { font-size: 56rpx; flex-shrink: 0; }
  .wl-info { flex: 1; }
  .wl-title { font-size: 30rpx; font-weight: 600; color: #1f2937; display: block; }
  .wl-desc { font-size: 24rpx; color: #9ca3af; margin-top: 4rpx; display: block; }
  .wl-arrow { font-size: 36rpx; color: #d1d5db; }
}

.calendar-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  .card-title { font-size: 28rpx; font-weight: 600; color: #1f2937; display: block; margin-bottom: 8rpx; }
  .streak { font-size: 24rpx; color: #9ca3af; display: block; margin-bottom: 24rpx; }
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
    border-radius: 8rpx;
    background: #f5f7fa;
    &.checked { background: #1890ff; }
    &.today { border: 2rpx solid #1890ff; }
  }
  .cal-num { font-size: 20rpx; color: #9ca3af; }
  .cal-day.checked .cal-num { color: #fff; }
}
</style>
