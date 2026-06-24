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
        <el-option v-for="lv in levelOptions" :key="lv.code" :label="lv.nameZh" :value="lv.code" />
      </el-select>
      <el-select v-model="source" style="width: 160px">
        <el-option
          v-for="opt in sourceOptions"
          :key="opt.value"
          :label="sourceLabel(opt.value)"
          :value="opt.value"
          :disabled="isSourceDisabled(opt.value) && source !== opt.value"
        />
      </el-select>
      <el-input-number v-model="size" :min="5" :max="50" label="题数" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { LEVELS } from '@/utils/constants'
import { availability } from '@/api/test'
import type { TestMode, TestSource } from '@/api/types'

const route = useRoute()
const router = useRouter()

const THINK_NAMES: Record<string, string> = {
  THINK_STARTER: 'Think 入门', THINK_L2: 'Think 2', THINK_L3: 'Think 3', THINK_L4: 'Think 4', THINK_L5: 'Think 5',
}
const queryLevel = (route.query.level as string) || ''
const selectedLevel = ref(queryLevel || LEVELS[7].code)
const source = ref((route.query.source as TestSource) || 'all')
const size = ref(20)
// 从词库「复习到期词」带 THINK_* 跳来时，下拉补显该等级（不污染全局 LEVELS / 等级选择页，守 org-model A）
const levelOptions = computed(() => {
  const opts = [...LEVELS]
  if (THINK_NAMES[queryLevel] && !opts.find(l => l.code === queryLevel)) {
    opts.push({ code: queryLevel, nameZh: THINK_NAMES[queryLevel], nameEn: queryLevel, targetCount: 0 })
  }
  return opts
})

const sourceCounts = ref<Record<string, number>>({ all: 0, due: 0, wrong_words: 0 })

const modes = [
  { value: 'spelling' as TestMode, label: '拼写测试', icon: 'mdi:keyboard-outline', color: '#1890ff', desc: '看中文释义，拼出单词' },
  { value: 'choice' as TestMode, label: '选择题', icon: 'mdi:format-list-checks', color: '#4f46e5', desc: '四选一词义测试' },
  { value: 'listening' as TestMode, label: '听写', icon: 'mdi:headphones', color: '#10b981', desc: '听发音，拼写单词' },
]

const sourceOptions: { label: string; value: TestSource }[] = [
  { label: '全部', value: 'all' },
  { label: '待复习', value: 'due' },
  { label: '错词本', value: 'wrong_words' },
]

function isSourceDisabled(s: TestSource) {
  return (sourceCounts.value[s] ?? 0) === 0
}

function sourceLabel(s: TestSource) {
  const base = sourceOptions.find(o => o.value === s)?.label ?? s
  const count = sourceCounts.value[s]
  if (count === undefined) return base
  return count > 0 ? `${base} (${count})` : `${base} (无数据)`
}

watch(selectedLevel, async (level) => {
  try {
    sourceCounts.value = await availability(level)
    // 如果当前选的来源没数据，自动切回"全部"
    if (isSourceDisabled(source.value)) source.value = 'all'
  } catch { /* ignore */ }
}, { immediate: true })

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
