<template>
  <div class="wordlist-detail" v-loading="loading">
    <el-button text @click="$router.push('/wordlists')">
      <Icon icon="mdi:arrow-left" width="20" /> 返回广场
    </el-button>

    <div v-if="data" class="header">
      <div class="emoji">{{ '📘' }}</div>
      <div class="title-block">
        <h2 class="name">{{ data.name }}</h2>
        <div class="meta">{{ data.word_count }} 词 · {{ data.unit_count }} 单元</div>
        <div class="desc">{{ data.description || '教材词库' }}</div>
      </div>
      <div class="header-actions">
        <template v-if="data.subscribed">
          <el-button type="primary" :disabled="!data.origin_level_code" @click="goReview">
            <Icon icon="mdi:refresh" width="18" /> 复习到期词
          </el-button>
        </template>
        <el-button v-else type="primary" @click="onSubscribe">
          <Icon icon="mdi:plus" width="18" /> 订阅
        </el-button>
      </div>
    </div>

    <div v-if="data?.subscribed" class="units-section">
      <div class="section-title">
        单元进度
        <span class="current-hint">当前：第 {{ data.current_unit_no }} 单元 · 点任意单元学新词</span>
      </div>
      <div class="unit-grid">
        <div
          v-for="u in data.units"
          :key="u.unit_no"
          class="unit-card"
          :class="{ current: u.is_current, done: u.completed }"
          @click="goLearn(u.unit_no)"
        >
          <div class="unit-head">
            <span class="unit-no">Unit {{ u.unit_no }}</span>
            <Icon v-if="u.completed" icon="mdi:check-circle" width="18" color="#10b981" />
            <span v-else-if="u.is_current" class="current-badge">当前</span>
          </div>
          <el-progress
            :percentage="pct(u)"
            :status="u.completed ? 'success' : undefined"
            :stroke-width="8"
          />
          <div class="unit-stat">{{ u.learned_count }}/{{ u.total_count }} 已学 · {{ u.mastered_count }} 掌握</div>
        </div>
      </div>
    </div>

    <el-alert
      v-else-if="data"
      type="info"
      :closable="false"
      class="hint"
      title="订阅后按单元学新词"
      description="艾宾浩斯自动安排跨单元复习。点右上「订阅」开始。"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Icon } from '@iconify/vue'
import { detail, subscribe } from '@/api/wordList'
import type { WordListDetail, UnitSummary } from '@/api/types'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)
const data = ref<WordListDetail | null>(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    data.value = await detail(id)
  } finally {
    loading.value = false
  }
}

function pct(u: UnitSummary) {
  return u.total_count ? Math.round((u.learned_count / u.total_count) * 100) : 0
}

async function onSubscribe() {
  await subscribe(id)
  ElMessage.success('订阅成功，开始学习吧')
  await load()
}

function goLearn(unit: number) {
  router.push({ path: `/wordlists/${id}/learn`, query: { unit } })
}

function goReview() {
  if (!data.value?.origin_level_code) return
  router.push({ path: '/test', query: { level: data.value.origin_level_code, source: 'due' } })
}

onMounted(load)
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.wordlist-detail {
  max-width: 900px;
  margin: 0 auto;
  padding: $space-6;
}
.header {
  display: flex;
  align-items: center;
  gap: $space-4;
  margin: $space-4 0 $space-6;
  padding: $space-4;
  background: #f0f7ff;
  border-radius: $radius-card;
}
.emoji {
  font-size: 2.5rem;
}
.title-block {
  flex: 1;
}
.name {
  font-size: 1.4rem;
  font-weight: 700;
  margin-bottom: $space-1;
}
.meta {
  font-size: 0.85rem;
  color: $color-primary;
  margin-bottom: 2px;
}
.desc {
  font-size: 0.8rem;
  color: #6b7280;
}
.section-title {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: $space-4;
  display: flex;
  align-items: baseline;
  gap: $space-3;
}
.current-hint {
  font-size: 0.8rem;
  font-weight: 400;
  color: #9ca3af;
}
.unit-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: $space-3;
}
.unit-card {
  border: 1px solid #e5e7eb;
  border-radius: $radius-card;
  padding: $space-3 $space-4;
  cursor: pointer;
  transition: all 0.15s;
  &:hover {
    border-color: $color-primary;
    background: #f0f7ff;
  }
  &.current {
    border-color: $color-primary;
    border-width: 2px;
    background: #e6f4ff;
  }
  &.done {
    background: #f0fdf4;
  }
}
.unit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-2;
}
.unit-no {
  font-weight: 600;
  font-size: 0.95rem;
}
.current-badge {
  font-size: 0.7rem;
  color: $color-primary;
  background: #fff;
  padding: 1px 6px;
  border-radius: 8px;
}
.unit-stat {
  font-size: 0.75rem;
  color: #6b7280;
  margin-top: $space-2;
}
.hint {
  margin-top: $space-4;
}
</style>
