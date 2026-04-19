<template>
  <div class="fc-page">
    <div class="page-title">遗忘曲线</div>

    <div class="toolbar">
      <el-input
        v-model="wordId"
        placeholder="输入单词 ID"
        style="width: 200px"
        type="number"
        @keyup.enter="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />

    <template v-else-if="data">
      <div class="word-info">
        <span class="word-text" style="font-size: 1.4rem">{{ data.word }}</span>
        <span class="ipa">{{ data.reviews.length }} 次复习记录</span>
      </div>
      <EbbinghausChart :data="data" />
    </template>

    <el-empty v-else-if="searched" description="暂无数据" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import EbbinghausChart from '@/components/EbbinghausChart.vue'
import * as statsApi from '@/api/stats'
import type { ForgettingCurveData } from '@/api/types'

const wordId = ref('')
const loading = ref(false)
const searched = ref(false)
const data = ref<ForgettingCurveData | null>(null)

async function load() {
  if (!wordId.value) return
  loading.value = true
  searched.value = true
  try {
    data.value = await statsApi.forgettingCurve(Number(wordId.value))
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;
.fc-page { padding: $space-6; }
.page-title { font-size: 1.5rem; font-weight: 700; margin-bottom: $space-6; }
.toolbar { display: flex; gap: $space-3; margin-bottom: $space-4; }
.word-info { display: flex; align-items: baseline; gap: $space-3; margin-bottom: $space-3; }
</style>
