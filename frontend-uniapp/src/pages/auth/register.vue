<template>
  <view class="register">
    <view class="form">
      <view class="input-wrap">
        <input v-model="identifier" class="input" placeholder="邮箱" type="text" />
      </view>
      <view class="input-wrap">
        <input v-model="password" class="input" placeholder="密码（至少8位，含字母和数字）" password />
      </view>
      <view class="input-wrap code-row">
        <input v-model="code" class="input" placeholder="验证码" type="number" maxlength="6" />
        <text class="send-btn" :class="{ disabled: countdown > 0 }" @click="sendCode">
          {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
        </text>
      </view>
      <view class="input-wrap">
        <input v-model="nickname" class="input" placeholder="昵称（可选）" />
      </view>

      <button class="btn-primary" :disabled="loading" @click="handleRegister">
        {{ loading ? '注册中...' : '注册' }}
      </button>

      <text class="login-link" @click="uni.navigateBack()">已有账号？返回登录</text>
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

const identifier = ref('')
const password = ref('')
const code = ref('')
const nickname = ref('')
const loading = ref(false)
const countdown = ref(0)

async function sendCode() {
  if (countdown.value > 0 || !identifier.value) return
  await apiSendCode('email', identifier.value, 'register')
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}

async function handleRegister() {
  if (!identifier.value || !password.value || !code.value) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const data = await register('email', identifier.value, password.value, code.value, nickname.value || undefined)
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
  background: $color-bg-page;
  padding: $space-2xl $space-2xl;
}
.form { display: flex; flex-direction: column; gap: $space-md; }
.input-wrap {
  background: #fff;
  border-radius: $radius-lg;
  padding: 0 $space-md;
  height: 96rpx;
  display: flex;
  align-items: center;
}
.input { flex: 1; font-size: $font-md; }
.code-row { gap: $space-sm; }
.send-btn {
  font-size: $font-sm;
  color: $color-primary;
  flex-shrink: 0;
  white-space: nowrap;
  &.disabled { color: $color-text-placeholder; }
}
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
.login-link {
  text-align: center;
  font-size: $font-sm;
  color: $color-primary;
  display: block;
}
</style>
