<template>
  <div class="settings-page">
    <div class="page-title">个人设置</div>

    <el-form label-width="140px" size="default">
      <el-card style="margin-bottom: 16px">
        <template #header>学习偏好</template>
        <el-form-item label="每日新词目标">
          <el-input-number v-model="draft.daily_new_words_goal" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="每日复习上限">
          <el-input-number v-model="draft.daily_review_goal" :min="10" :max="500" />
        </el-form-item>
        <el-form-item label="新词排序">
          <el-radio-group v-model="draft.default_sort_mode">
            <el-radio value="alpha">字母序</el-radio>
            <el-radio value="topic">主题</el-radio>
            <el-radio value="random">随机</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="默认音标">
          <el-radio-group v-model="draft.preferred_accent">
            <el-radio value="uk">英式</el-radio>
            <el-radio value="us">美式</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="翻卡自动发音">
          <el-switch v-model="draft.auto_play_audio" />
        </el-form-item>
      </el-card>

      <el-card>
        <template #header>界面</template>
        <el-form-item label="主题">
          <el-radio-group v-model="draft.theme">
            <el-radio value="light">浅色</el-radio>
            <el-radio value="dark">深色</el-radio>
            <el-radio value="system">跟随系统</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-card>
    </el-form>

    <div style="margin-top: 24px">
      <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useSettingsStore } from '@/stores/settings'
import type { UserSettings } from '@/api/types'

const settingsStore = useSettingsStore()
const saving = ref(false)

const draft = reactive<UserSettings>({ ...settingsStore.settings })

onMounted(async () => {
  await settingsStore.fetch()
  Object.assign(draft, settingsStore.settings)
})

async function save() {
  saving.value = true
  try {
    await settingsStore.update({ ...draft })
    ElMessage.success('设置已保存')
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;
.settings-page { padding: $space-6; max-width: 600px; }
.page-title { font-size: 1.5rem; font-weight: 700; margin-bottom: $space-6; }
</style>
