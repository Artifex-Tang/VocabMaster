<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-logo">
        <Icon icon="mdi:book-open-variant" width="40" color="#1890ff" />
        <h1>注册 VocabMaster</h1>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleRegister">
        <el-tabs v-model="regType" class="reg-tabs">
          <el-tab-pane label="邮箱注册" name="email" />
          <el-tab-pane label="手机号注册" name="phone" />
        </el-tabs>

        <el-form-item prop="identifier">
          <el-input
            v-model="form.identifier"
            :placeholder="regType === 'email' ? '邮箱地址' : '手机号'"
            size="large"
            clearable
          >
            <template #prefix>
              <Icon :icon="regType === 'email' ? 'mdi:email-outline' : 'mdi:cellphone'" />
            </template>
          </el-input>
        </el-form-item>

        <el-form-item v-if="regType === 'phone'" prop="code">
          <div style="display:flex;gap:8px;width:100%">
            <el-input
              v-model="form.code"
              placeholder="验证码"
              size="large"
              style="flex:1"
            />
            <el-button
              size="large"
              :disabled="countdown.remaining.value > 0"
              @click="sendCode"
            >
              {{ countdown.remaining.value > 0 ? `${countdown.remaining.value}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码（8位以上，含字母+数字）"
            size="large"
            show-password
          >
            <template #prefix><Icon icon="mdi:lock-outline" /></template>
          </el-input>
        </el-form-item>

        <el-form-item prop="nickname">
          <el-input
            v-model="form.nickname"
            placeholder="昵称（可选）"
            size="large"
            clearable
          >
            <template #prefix><Icon icon="mdi:account-outline" /></template>
          </el-input>
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          :loading="loading"
          style="width:100%"
          @click="handleRegister"
        >
          注册
        </el-button>
      </el-form>

      <p class="switch-link">
        已有账号？<RouterLink to="/login">立即登录</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useCountdown } from '@/composables/useCountdown'
import * as authApi from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const countdown = useCountdown(60)

const formRef = ref<FormInstance>()
const loading = ref(false)
const regType = ref<'email' | 'phone'>('email')

const form = reactive({
  identifier: '',
  password: '',
  code: '',
  nickname: '',
})

const rules: FormRules = {
  identifier: [{ required: true, message: '必填', trigger: 'blur' }],
  password: [
    { required: true, message: '必填', trigger: 'blur' },
    { min: 8, message: '至少 8 位', trigger: 'blur' },
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

async function sendCode() {
  if (!form.identifier) {
    ElMessage.warning('请先填写手机号')
    return
  }
  await authApi.sendCode({ type: 'phone', identifier: form.identifier, scene: 'register' })
  countdown.start(60)
  ElMessage.success('验证码已发送')
}

async function handleRegister() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const data = await authApi.register({
      type: regType.value,
      identifier: form.identifier,
      password: form.password,
      code: form.code || undefined,
      nickname: form.nickname || undefined,
    })
    userStore.setAuth(data)
    router.push('/dashboard')
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e0f2fe 0%, #e8e8ff 100%);
}

.auth-card {
  width: 420px;
  background: #fff;
  border-radius: $radius-dialog;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  padding: $space-8;
}

.auth-logo {
  text-align: center;
  margin-bottom: $space-6;
  h1 { margin: $space-2 0 0; font-size: 1.4rem; }
}

.reg-tabs { margin-bottom: $space-4; }

.switch-link {
  text-align: center;
  margin: $space-4 0 0;
  font-size: 0.875rem;
  color: #6b7280;
  a { color: #1890ff; text-decoration: none; }
}
</style>
