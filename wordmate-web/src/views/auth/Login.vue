<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-logo">
        <Icon icon="mdi:book-open-variant" width="40" color="#1890ff" />
        <h1>VocabMaster</h1>
        <p>科学记忆，高效学词</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="identifier">
          <el-input
            v-model="form.identifier"
            placeholder="邮箱或手机号"
            size="large"
            clearable
          >
            <template #prefix><Icon icon="mdi:account-outline" /></template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          >
            <template #prefix><Icon icon="mdi:lock-outline" /></template>
          </el-input>
        </el-form-item>

        <div class="form-extra">
          <el-checkbox v-model="rememberMe">记住我 7 天</el-checkbox>
          <RouterLink to="/reset-password" class="link">忘记密码？</RouterLink>
        </div>

        <el-button
          type="primary"
          size="large"
          :loading="loading"
          style="width: 100%"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>

      <div class="divider"><span>或</span></div>

      <div class="third-party">
        <el-button size="large" style="width: 100%" @click="handleWechat">
          <Icon icon="mdi:wechat" color="#07C160" width="20" />
          微信扫码登录
        </el-button>
      </div>

      <p class="switch-link">
        没有账号？
        <RouterLink to="/register">立即注册</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Icon } from '@iconify/vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(false)

const form = reactive({ identifier: '', password: '' })

const rules: FormRules = {
  identifier: [{ required: true, message: '请输入邮箱或手机号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少 8 位', trigger: 'blur' },
  ],
}

function detectType(v: string): 'email' | 'phone' {
  return /^\d{11}$/.test(v) ? 'phone' : 'email'
}

async function handleLogin() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await userStore.login(detectType(form.identifier), form.identifier, form.password)
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch {
    ElMessage.error('登录失败，请检查账号或密码')
  } finally {
    loading.value = false
  }
}

function handleWechat() {
  ElMessage.info('微信登录需在小程序端使用')
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
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  padding: $space-8 $space-8 $space-6;
}

.auth-logo {
  text-align: center;
  margin-bottom: $space-6;
  h1 { margin: $space-2 0 0; font-size: 1.5rem; font-family: $font-en; }
  p { margin: $space-1 0 0; color: #6b7280; font-size: 0.9rem; }
}

.form-extra {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $space-4;
}

.link, .switch-link a {
  color: $color-primary;
  text-decoration: none;
  font-size: 0.875rem;
  &:hover { text-decoration: underline; }
}

.divider {
  text-align: center;
  margin: $space-4 0;
  position: relative;
  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 0; right: 0;
    border-top: 1px solid #e5e7eb;
  }
  span {
    background: #fff;
    padding: 0 $space-2;
    position: relative;
    color: #9ca3af;
    font-size: 0.85rem;
  }
}

.third-party { margin-bottom: $space-4; }

.switch-link {
  text-align: center;
  margin: $space-4 0 0;
  font-size: 0.875rem;
  color: #6b7280;
}
</style>
