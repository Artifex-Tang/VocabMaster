<template>
  <view class="login">
    <view class="logo-area">
      <text class="app-name">VocabMaster</text>
      <text class="tagline">科学记忆，高效背词</text>
    </view>

    <view class="form">
      <!-- 登录方式切换 -->
      <view class="tab-row">
        <text :class="['tab', tab === 'email' && 'tab-active']" @click="tab = 'email'">邮箱登录</text>
        <text :class="['tab', tab === 'phone' && 'tab-active']" @click="tab = 'phone'">手机号登录</text>
      </view>

      <view class="input-wrap">
        <input
          v-model="identifier"
          class="input"
          :placeholder="tab === 'email' ? '请输入邮箱' : '请输入手机号'"
          :type="tab === 'email' ? 'text' : 'number'"
        />
      </view>
      <view class="input-wrap">
        <input v-model="password" class="input" placeholder="请输入密码" password />
      </view>

      <button class="btn-primary" :disabled="loading" @click="handleLogin">
        {{ loading ? '登录中...' : '登录' }}
      </button>

      <!-- 微信登录（仅小程序端） -->
      <!-- #ifdef MP-WEIXIN -->
      <button class="btn-wechat" open-type="getUserInfo" @getuserinfo="handleWechatLogin">
        微信一键登录
      </button>
      <!-- #endif -->

      <view class="footer-row">
        <text @click="goRegister">注册账号</text>
        <text @click="goForgot">忘记密码</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'
import { loginByPassword, loginWechat } from '@/api/auth'

const userStore = useUserStore()
const settingsStore = useSettingsStore()

const tab = ref<'email' | 'phone'>('email')
const identifier = ref('')
const password = ref('')
const loading = ref(false)
let redirectUrl = ''

onLoad((query) => {
  if (query?.redirect) redirectUrl = query.redirect
})

function detectType(val: string): 'email' | 'phone' {
  return /^\d{11}$/.test(val) ? 'phone' : 'email'
}

async function handleLogin() {
  if (!identifier.value || !password.value) {
    uni.showToast({ title: '请填写账号和密码', icon: 'none' })
    return
  }
  if (password.value.length < 8) {
    uni.showToast({ title: '密码至少 8 位', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const type = detectType(identifier.value)
    const data = await loginByPassword(type, identifier.value, password.value)
    userStore.setAuth(data)
    await settingsStore.fetch()
    settingsStore.sync()
    navigateAfterLogin()
  } finally {
    loading.value = false
  }
}

async function handleWechatLogin(e: Record<string, unknown>) {
  loading.value = true
  try {
    const detail = (e as { detail?: { userInfo?: { nickName?: string; avatarUrl?: string }; errMsg?: string } }).detail
    if (!detail || detail.errMsg?.includes('fail')) return

    // 获取微信 code
    const loginRes = await new Promise<{ code: string }>((resolve, reject) => {
      uni.login({ provider: 'weixin', success: resolve, fail: reject })
    })

    const userInfo = detail.userInfo
      ? { nickname: detail.userInfo.nickName ?? '', avatar_url: detail.userInfo.avatarUrl ?? '' }
      : undefined

    const data = await loginWechat(loginRes.code, userInfo)
    userStore.setAuth(data)
    await settingsStore.fetch()
    settingsStore.sync()
    navigateAfterLogin()
  } finally {
    loading.value = false
  }
}

function navigateAfterLogin() {
  if (redirectUrl) {
    uni.redirectTo({ url: redirectUrl })
  } else {
    uni.switchTab({ url: '/pages/index/index' })
  }
}

function goRegister() {
  uni.navigateTo({ url: '/pages/auth/register' })
}

function goForgot() {
  uni.showToast({ title: '请通过邮件重置密码', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.login {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 48rpx 48rpx;
}

.logo-area {
  margin-bottom: 48rpx;
  text-align: center;
  .app-name { font-size: 80rpx; font-weight: 800; color: #1890ff; display: block; }
  .tagline { font-size: 28rpx; color: #9ca3af; margin-top: 12rpx; display: block; }
}

.form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.tab-row {
  display: flex;
  gap: 32rpx;
  justify-content: center;
  margin-bottom: 8rpx;
}
.tab {
  font-size: 28rpx;
  color: #9ca3af;
  padding: 8rpx 16rpx;
}
.tab-active {
  color: #1890ff;
  font-weight: 600;
  border-bottom: 4rpx solid #1890ff;
}

.input-wrap {
  background: #fff;
  border-radius: 16rpx;
  padding: 0 24rpx;
  height: 96rpx;
  display: flex;
  align-items: center;
}
.input { width: 100%; font-size: 28rpx; }

.btn-primary {
  height: 96rpx;
  background: #1890ff;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 48rpx;
  border: none;
  &[disabled] { opacity: 0.6; }
}

.btn-wechat {
  height: 96rpx;
  background: #07c160;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 48rpx;
  border: none;
}

.footer-row {
  display: flex;
  justify-content: space-between;
  font-size: 24rpx;
  color: #1890ff;
  padding: 0 8rpx;
}
</style>
