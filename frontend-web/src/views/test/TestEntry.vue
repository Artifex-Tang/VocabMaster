<template>
  <div class="test-page">
    <div class="page-title">测试练习</div>

    <el-row :gutter="16">
      <el-col :span="8" v-for="mode in modes" :key="mode.value">
        <el-card class="mode-card" shadow="hover" @click="start(mode.value)">
          <Icon :icon="mode.icon" width="36" :color="mode.color" />
          <h3>{{ mode.label }}</h3>
          <p>{{ mode.desc }}</p>
        </el-card>
      </el-col>
    </el-row>

    <el-divider />
    <div class="config-row">
      <el-select v-model="selectedLevel" placeholder="选择等级" style="width: 160px">
        <el-option v-for="lv in LEVELS" :key="lv.code" :label="lv.nameZh" :value="lv.code" />
      </el-select>
      <el-select v-model="source" style="width: 130px">
        <el-option label="待复习" value="due" />
        <el-option label="全部" value="all" />
        <el-option label="错词本" value="wrong_words" />
      </el-select>
      <el-input-number v-model="size" :min="5" :max="50" label="题数" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { LEVELS } from '@/utils/constants'
import type { TestMode, TestSource } from '@/api/types'

const router = useRouter()
const selectedLevel = ref(LEVELS[7].code)
const source = ref<TestSource>('due')
const size = ref(20)

const modes = [
  { value: 'spelling' as TestMode, label: '拼写测试', icon: 'mdi:keyboard-outline', color: '#1890ff', desc: '看中文释义，拼出单词' },
  { value: 'choice' as TestMode, label: '选择题', icon: 'mdi:format-list-checks', color: '#4f46e5', desc: '四选一词义测试' },
  { value: 'listening' as TestMode, label: '听写', icon: 'mdi:headphones', color: '#10b981', desc: '听发音，拼写单词' },
]

function start(mode: TestMode) {
  router.push({ path: `/test/${mode}`, query: { level: selectedLevel.value, source: source.value, size: String(size.value) } })
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;
.test-page { padding: $space-6; }
.page-title { font-size: 1.5rem; font-weight: 700; margin-bottom: $space-6; }
.mode-card { cursor: pointer; text-align: center; padding: $space-4; h3 { margin: $space-2 0 $space-1; } p { color: #6b7280; font-size: .85rem; margin: 0; } }
.config-row { display: flex; gap: $space-3; align-items: center; }
</style>
