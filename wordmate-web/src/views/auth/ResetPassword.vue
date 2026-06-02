<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>重置密码</h2>

      <el-form ref="formRef" :model="form" :rules="rules">
        <el-form-item prop="identifier">
          <el-input v-model="form.identifier" placeholder="邮箱或手机号" size="large" />
        </el-form-item>
        <el-form-item prop="code">
          <div style="display:flex;gap:8px;width:100%">
            <el-input v-model="form.code" placeholder="验证码" size="large" style="flex:1" />
            <el-button size="large" :disabled="countdown.remaining.value > 0" @click="sendCode">
              {{ countdown.remaining.value > 0 ? `${countdown.remaining.value}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="新密码（8位以上）"
            size="large"
            show-password
          />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" style="width:100%" @click="submit">
          重置密码
        </el-button>
      </el-form>

      <p class="back-link"><RouterLink to="/login">返回登录</RouterLink></p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useCountdown } from '@/composables/useCountdown'
import * as authApi from '@/api/auth'

const router = useRouter()
const countdown = useCountdown(60)
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ identifier: '', code: '', newPassword: '' })

const rules: FormRules = {
  identifier: [{ required: true, message: '必填', trigger: 'blur' }],
  code: [{ required: true, message: '必填', trigger: 'blur' }],
  newPassword: [{ required: true, min: 8, message: '至少 8 位', trigger: 'blur' }],
}

function detectType(v: string): 'email' | 'phone' {
  return /^\d{11}$/.test(v) ? 'phone' : 'email'
}

async function sendCode() {
  if (!form.identifier) { ElMessage.warning('请先填写账号'); return }
  await authApi.sendCode({
    type: detectType(form.identifier),
    identifier: form.identifier,
    scene: 'reset_password',
  })
  countdown.start()
  ElMessage.success('验证码已发送')
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await authApi.resetPassword({
      type: detectType(form.identifier),
      identifier: form.identifier,
      code: form.code,
      new_password: form.newPassword,
    })
    ElMessage.success('密码重置成功，请重新登录')
    router.push('/login')
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
  width: 400px;
  background: #fff;
  border-radius: $radius-dialog;
  box-shadow: 0 8px 32px rgba(0,0,0,.12);
  padding: $space-8;
  h2 { margin: 0 0 $space-6; text-align: center; }
}
.back-link { text-align: center; margin-top: $space-4; font-size: .875rem; a { color: $color-primary; text-decoration: none; } }
</style>
