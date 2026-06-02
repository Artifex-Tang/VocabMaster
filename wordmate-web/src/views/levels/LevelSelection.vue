<template>
  <div class="level-page">
    <div class="page-title">选择等级</div>
    <div class="level-grid">
      <el-card
        v-for="lv in LEVELS"
        :key="lv.code"
        class="level-card"
        :class="{ active: activeSet.has(lv.code) }"
        shadow="hover"
        @click="toggleLevel(lv.code)"
      >
        <div class="lv-code">{{ lv.nameEn }}</div>
        <div class="lv-zh">{{ lv.nameZh }}</div>
        <div class="lv-count">{{ lv.targetCount.toLocaleString() }} 词</div>
        <Icon
          v-if="activeSet.has(lv.code)"
          icon="mdi:check-circle"
          color="#10b981"
          width="20"
          class="check-icon"
        />
      </el-card>
    </div>
    <el-button type="primary" size="large" :disabled="!changed" @click="saveActiveLevels">
      保存选择
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { ElMessage } from 'element-plus'
import { LEVELS } from '@/utils/constants'
import { useSettingsStore } from '@/stores/settings'

const settingsStore = useSettingsStore()
const activeSet = ref<Set<string>>(new Set())
const original = ref<string[]>([])

const changed = computed(
  () => JSON.stringify([...activeSet.value].sort()) !== JSON.stringify([...original.value].sort()),
)

function toggleLevel(code: string) {
  if (activeSet.value.has(code)) activeSet.value.delete(code)
  else activeSet.value.add(code)
}

async function saveActiveLevels() {
  await settingsStore.update({ active_levels: [...activeSet.value] })
  original.value = [...activeSet.value]
  ElMessage.success('已保存')
}

onMounted(() => {
  const al = settingsStore.settings.active_levels
  activeSet.value = new Set(al)
  original.value = [...al]
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.level-page { padding: $space-6; }
.page-title { font-size: 1.5rem; font-weight: 700; margin-bottom: $space-6; }

.level-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: $space-4;
  margin-bottom: $space-6;
}

.level-card {
  cursor: pointer;
  position: relative;
  transition: border-color 0.2s;
  &.active { border: 2px solid $color-primary; }
}

.lv-code { font-family: $font-en; font-weight: 700; font-size: 1.1rem; }
.lv-zh { font-size: 0.85rem; color: #6b7280; margin: 4px 0; }
.lv-count { font-size: 0.75rem; color: #9ca3af; }

.check-icon { position: absolute; top: 8px; right: 8px; }
</style>
