<template>
  <view class="achievements">
    <view class="section" v-if="unlocked.length">
      <text class="section-title">已解锁</text>
      <view v-for="a in unlocked" :key="a.code" class="achievement-card unlocked">
        <text class="achievement-icon">{{ a.icon || '🏆' }}</text>
        <view class="achievement-info">
          <text class="achievement-name">{{ a.name_zh }}</text>
          <text class="achievement-date">{{ a.achieved_at?.slice(0, 10) }}</text>
        </view>
      </view>
    </view>

    <view class="section" v-if="locked.length">
      <text class="section-title">未解锁</text>
      <view v-for="a in locked" :key="a.code" class="achievement-card locked">
        <text class="achievement-icon">🔒</text>
        <view class="achievement-info">
          <text class="achievement-name">{{ a.name_zh }}</text>
          <text class="achievement-progress">{{ a.progress }}</text>
        </view>
      </view>
    </view>

    <view v-if="!unlocked.length && !locked.length && !loading" class="empty">
      <text class="empty-text">暂无成就数据</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface AchievementUnlocked {
  code: string
  name_zh: string
  icon: string
  achieved_at: string
}

interface AchievementLocked {
  code: string
  name_zh: string
  progress: string
}

const unlocked = ref<AchievementUnlocked[]>([])
const locked = ref<AchievementLocked[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const { getAchievements } = await import('@/api/stats')
    const data = await getAchievements()
    unlocked.value = data.unlocked
    locked.value = data.locked
  } catch {
    // 静默
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.achievements {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32rpx;
}

.section { margin-bottom: 32rpx; }
.section-title {
  font-size: 28rpx; font-weight: 600; color: #1f2937;
  display: block; margin-bottom: 16rpx;
}

.achievement-card {
  background: #fff; border-radius: 16rpx; padding: 24rpx 32rpx;
  display: flex; align-items: center; gap: 20rpx;
  margin-bottom: 12rpx;
}
.achievement-card.unlocked { border-left: 8rpx solid #52c41a; }
.achievement-card.locked { border-left: 8rpx solid #d1d5db; opacity: 0.7; }

.achievement-icon { font-size: 48rpx; }
.achievement-info { flex: 1; }
.achievement-name { font-size: 28rpx; font-weight: 600; color: #1f2937; display: block; }
.achievement-date, .achievement-progress { font-size: 24rpx; color: #9ca3af; display: block; margin-top: 4rpx; }

.empty { text-align: center; padding-top: 200rpx; }
.empty-text { font-size: 28rpx; color: #9ca3af; }
</style>
