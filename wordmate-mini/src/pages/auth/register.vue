<template>
  <view class="register">
    <view class="form">
      <!-- 注册方式切换 -->
      <view class="tab-row">
        <text :class="['tab', regType === 'email' && 'tab-active']" @click="regType = 'email'">邮箱注册</text>
        <text :class="['tab', regType === 'phone' && 'tab-active']" @click="regType = 'phone'">手机号注册</text>
      </view>

      <view class="input-wrap">
        <input
          v-model="identifier"
          class="input"
          :placeholder="regType === 'email' ? '请输入邮箱' : '请输入手机号'"
          :type="regType === 'email' ? 'text' : 'number'"
        />
      </view>

      <!-- 手机号注册才需要验证码 -->
      <view v-if="regType === 'phone'" class="input-wrap code-row">
        <input v-model="code" class="input" placeholder="验证码" type="number" maxlength="6" />
        <text class="send-btn" :class="{ disabled: countdown > 0 }" @click="sendCode">
          {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
        </text>
      </view>

      <view class="input-wrap">
        <input v-model="password" class="input" placeholder="密码（至少8位）" password />
      </view>
      <view class="input-wrap">
        <input v-model="nickname" class="input" placeholder="昵称（可选）" />
      </view>

      <button class="btn-primary" :disabled="loading" @click="handleRegister">
        {{ loading ? '注册中...' : '注册' }}
      </button>

      <text class="login-link" @click="goBack">已有账号？返回登录</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'
import { register, sendCode as apiSendCode } from '@/api/auth'

const userStore = useUserStore()
const settingsStore = useSettingsStore()

const regType = ref<'email' | 'phone'>('email')
const identifier = ref('')
const password = ref('')
const code = ref('')
const nickname = ref('')
const loading = ref(false)
const countdown = ref(0)

async function sendCode() {
  if (countdown.value > 0 || !identifier.value) return
  try {
    await apiSendCode(regType.value, identifier.value, 'register')
    uni.showToast({ title: '验证码已发送', icon: 'success' })
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch {
    // request.ts 已 toast 错误
  }
}

function goBack() { uni.navigateBack() }

async function handleRegister() {
  if (!identifier.value || !password.value) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' })
    return
  }
  if (password.value.length < 8) {
    uni.showToast({ title: '密码至少 8 位', icon: 'none' })
    return
  }
  if (regType.value === 'phone' && !code.value) {
    uni.showToast({ title: '请输入验证码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const data = await register(
      regType.value,
      identifier.value,
      password.value,
      regType.value === 'phone' ? code.value : '',
      nickname.value || undefined,
    )
    userStore.setAuth(data)
    await settingsStore.fetch()
    settingsStore.sync()
    uni.switchTab({ url: '/pages/index/index' })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.register {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 48rpx 48rpx;
}
.form { display: flex; flex-direction: column; gap: 24rpx; }

.tab-row {
  display: flex;
  gap: 32rpx;
  justify-content: center;
  margin-bottom: 8rpx;
}
.tab { font-size: 28rpx; color: #9ca3af; padding: 8rpx 16rpx; }
.tab-active { color: #1890ff; font-weight: 600; border-bottom: 4rpx solid #1890ff; }

.input-wrap {
  background: #fff;
  border-radius: 16rpx;
  padding: 0 24rpx;
  height: 96rpx;
  display: flex;
  align-items: center;
}
.input { flex: 1; font-size: 28rpx; }
.code-row { gap: 16rpx; }
.send-btn {
  font-size: 24rpx;
  color: #1890ff;
  flex-shrink: 0;
  white-space: nowrap;
  &.disabled { color: #ccc; }
}
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
.login-link { text-align: center; font-size: 24rpx; color: #1890ff; display: block; }
</style>
