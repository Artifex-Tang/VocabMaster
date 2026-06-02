<template>
  <view class="mine">
    <!-- 用户信息 -->
    <view class="user-card">
      <image
        class="avatar"
        :src="userInfo?.avatar_url || '/static/default-avatar.png'"
        mode="aspectFill"
      />
      <view class="user-info">
        <text class="nickname">{{ userInfo?.nickname ?? '用户' }}</text>
        <text class="email">{{ userInfo?.email ?? userInfo?.phone_masked ?? '' }}</text>
      </view>
    </view>

    <!-- 连续打卡 -->
    <view class="streak-card" v-if="checkin">
      <text class="streak-num">{{ checkin.current_streak }}</text>
      <text class="streak-label">天连续打卡</text>
      <text class="streak-total">累计 {{ checkin.total_days }} 天</text>
    </view>

    <!-- 菜单 -->
    <view class="menu-list">
      <view v-for="item in menuItems" :key="item.key" class="menu-item" @click="item.action()">
        <text class="menu-icon">{{ item.icon }}</text>
        <text class="menu-label">{{ item.label }}</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <button class="btn-logout" @click="handleLogout">退出登录</button>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { checkinToday, getAchievements } from '@/api/stats'
import { logout as apiLogout } from '@/api/auth'
import type { CheckinResult } from '@/api/types'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const checkin = ref<CheckinResult | null>(null)

const menuItems = [
  { key: 'wrong-book', icon: '📖', label: '错词本', action: () => uni.navigateTo({ url: '/pages/wrong-book/list' }) },
  { key: 'word-search', icon: '🔍', label: '词库搜索', action: () => uni.navigateTo({ url: '/pages/word/search' }) },
  { key: 'settings', icon: '⚙️', label: '学习设置', action: () => uni.navigateTo({ url: '/pages/mine/settings' }) },
  { key: 'achievements', icon: '🏆', label: '我的成就', action: () => uni.navigateTo({ url: '/pages/mine/achievements' }) },
]

onShow(loadData)

async function loadData() {
  try {
    checkin.value = await checkinToday()
  } catch {
    // 可能已签到过，静默
  }
}

async function handleLogout() {
  uni.showModal({
    title: '确认退出',
    content: '退出后需要重新登录',
    success: async (res) => {
      if (!res.confirm) return
      try { await apiLogout() } catch { /* 静默 */ }
      userStore.logout()
      uni.reLaunch({ url: '/pages/auth/login' })
    },
  })
}
</script>

<style lang="scss" scoped>
.mine {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32rpx;
  padding-bottom: calc(120rpx + 32rpx);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.user-card {
  width: 100%; background: #fff; border-radius: 24rpx;
  padding: 32rpx; display: flex; align-items: center; gap: 24rpx;
  margin-bottom: 16rpx;
}
.avatar { width: 120rpx; height: 120rpx; border-radius: 50%; flex-shrink: 0; }
.user-info { flex: 1; }
.nickname { font-size: 32rpx; font-weight: 700; color: #1f2937; display: block; }
.email { font-size: 24rpx; color: #9ca3af; margin-top: 4rpx; display: block; }

.streak-card {
  width: 100%; background: #1890ff; border-radius: 24rpx;
  padding: 32rpx; margin-bottom: 16rpx;
  display: flex; align-items: baseline; gap: 8rpx;
}
.streak-num { font-size: 48rpx; font-weight: 800; color: #fff; }
.streak-label { font-size: 28rpx; color: rgba(255,255,255,0.85); }
.streak-total { margin-left: auto; font-size: 24rpx; color: rgba(255,255,255,0.7); }

.menu-list { width: 100%; background: #fff; border-radius: 24rpx; margin-bottom: 32rpx; overflow: hidden; }
.menu-item {
  display: flex; align-items: center; padding: 28rpx 32rpx;
  gap: 20rpx; border-bottom: 1rpx solid #f5f7fa;
  &:last-child { border-bottom: none; }
}
.menu-icon { font-size: 40rpx; }
.menu-label { flex: 1; font-size: 28rpx; color: #1f2937; }
.menu-arrow { font-size: 36rpx; color: #d1d5db; }

.btn-logout {
  width: 100%; height: 88rpx; border-radius: 48rpx;
  background: #fef0f0; color: #f56c6c;
  font-size: 28rpx; font-weight: 600; border: none;
}
</style>
