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
    <text class="version">v1.0.0</text>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getTodayStats, checkinToday } from '@/api/stats'
import { logout as apiLogout } from '@/api/auth'
import type { CheckinResult } from '@/api/types'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const checkin = ref<CheckinResult | null>(null)

const menuItems = [
  { key: 'wrong-book', icon: '📖', label: '错词本', action: () => uni.navigateTo({ url: '/pages/wrong-book/list' }) },
  { key: 'settings',   icon: '⚙️', label: '设置',   action: () => uni.showToast({ title: '设置页待实现', icon: 'none' }) },
  { key: 'achievements', icon: '🏆', label: '我的成就', action: () => uni.showToast({ title: '成就页待实现', icon: 'none' }) },
  { key: 'export',     icon: '📤', label: '导出数据', action: handleExport },
]

onMounted(loadData)
onShow(loadData)

async function loadData() {
  try {
    checkin.value = await checkinToday()
  } catch {
    // 静默
  }
}

function handleExport() {
  uni.showToast({ title: '导出功能开发中', icon: 'none' })
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
  min-height: 100vh; background: $color-bg-page;
  padding: $space-lg;
  padding-bottom: calc(#{$tabbar-height} + $space-lg);
  display: flex; flex-direction: column; align-items: center;
}

.user-card {
  width: 100%; background: #fff; border-radius: $radius-xl;
  padding: $space-lg; display: flex; align-items: center; gap: $space-lg;
  margin-bottom: $space-md;
}
.avatar { width: 120rpx; height: 120rpx; border-radius: 50%; flex-shrink: 0; }
.user-info { flex: 1; }
.nickname { font-size: $font-lg; font-weight: 700; color: $color-text-primary; display: block; }
.email { font-size: $font-sm; color: $color-text-secondary; margin-top: 4rpx; display: block; }

.streak-card {
  width: 100%; background: $color-primary; border-radius: $radius-xl;
  padding: $space-lg; margin-bottom: $space-md;
  display: flex; align-items: baseline; gap: $space-sm;
}
.streak-num { font-size: $font-3xl; font-weight: 800; color: #fff; }
.streak-label { font-size: $font-md; color: rgba(255,255,255,0.85); }
.streak-total { margin-left: auto; font-size: $font-sm; color: rgba(255,255,255,0.7); }

.menu-list { width: 100%; background: #fff; border-radius: $radius-xl; margin-bottom: $space-lg; overflow: hidden; }
.menu-item {
  display: flex; align-items: center; padding: $space-md $space-lg;
  gap: $space-md; border-bottom: 1rpx solid $color-bg-page;
  &:last-child { border-bottom: none; }
}
.menu-icon { font-size: 40rpx; }
.menu-label { flex: 1; font-size: $font-md; color: $color-text-primary; }
.menu-arrow { font-size: $font-xl; color: $color-text-placeholder; }

.btn-logout {
  width: 100%; height: 88rpx; border-radius: $radius-xl;
  background: #FEF0F0; color: $color-danger;
  font-size: $font-md; font-weight: 600; border: none;
  margin-bottom: $space-md;
}
.version { font-size: $font-xs; color: $color-text-placeholder; }
</style>
