<template>
  <view class="login">
    <view class="logo-area">
      <text class="app-name">VocabMaster</text>
      <text class="tagline">科学记忆，高效背词</text>
    </view>

    <view class="form">
      <!-- 邮箱/手机号 -->
      <view class="input-wrap">
        <input
          v-model="identifier"
          class="input"
          :placeholder="tab === 'email' ? '邮箱' : '手机号'"
          :type="tab === 'email' ? 'text' : 'number'"
        />
      </view>
      <view class="input-wrap">
        <input
          v-model="password"
          class="input"
          placeholder="密码"
          password
        />
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
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'
import { loginByPassword, wechatLoginMiniProgram } from '@/api/auth'

const userStore = useUserStore()
const settingsStore = useSettingsStore()

const tab = ref<'email' | 'phone'>('email')
const identifier = ref('')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  if (!identifier.value || !password.value) {
    uni.showToast({ title: '请填写账号和密码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const data = await loginByPassword(tab.value, identifier.value, password.value)
    userStore.setAuth(data)
    await settingsStore.fetch()
    settingsStore.sync()
    uni.switchTab({ url: '/pages/index/index' })
  } finally {
    loading.value = false
  }
}

async function handleWechatLogin(e: Record<string, unknown>) {
  loading.value = true
  try {
    const userInfoRaw = (e as { detail?: { userInfo?: { nickName?: string; avatarUrl?: string } } }).detail?.userInfo
    const userInfo = userInfoRaw
      ? { nickname: userInfoRaw.nickName ?? '', avatar_url: userInfoRaw.avatarUrl ?? '' }
      : undefined
    const data = await wechatLoginMiniProgram()
    userStore.setAuth(data)
    await settingsStore.fetch()
    settingsStore.sync()
    uni.switchTab({ url: '/pages/index/index' })
  } finally {
    loading.value = false
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
  background: $color-bg-page;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx $space-2xl $space-2xl;
}

.logo-area {
  margin-bottom: $space-2xl;
  text-align: center;
  .app-name { font-size: 80rpx; font-weight: 800; color: $color-primary; display: block; }
  .tagline { font-size: $font-md; color: $color-text-secondary; margin-top: $space-sm; display: block; }
}

.form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: $space-md;
}

.input-wrap {
  background: #fff;
  border-radius: $radius-lg;
  padding: 0 $space-md;
  height: 96rpx;
  display: flex;
  align-items: center;
}
.input { width: 100%; font-size: $font-md; }

.btn-primary {
  height: 96rpx;
  background: $color-primary;
  color: #fff;
  font-size: $font-lg;
  font-weight: 600;
  border-radius: $radius-xl;
  border: none;
  &[disabled] { opacity: 0.6; }
}

.btn-wechat {
  height: 96rpx;
  background: #07C160;
  color: #fff;
  font-size: $font-lg;
  font-weight: 600;
  border-radius: $radius-xl;
  border: none;
}

.footer-row {
  display: flex;
  justify-content: space-between;
  font-size: $font-sm;
  color: $color-primary;
  padding: 0 $space-xs;
}
</style>
