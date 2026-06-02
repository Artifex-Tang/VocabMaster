<template>
  <div class="fc-page">
    <div class="page-title">遗忘曲线</div>

    <div class="toolbar">
      <el-autocomplete
        v-model="searchText"
        :fetch-suggestions="searchWords"
        placeholder="输入单词搜索..."
        clearable
        style="width: 300px"
        @select="onSelect"
        @keyup.enter="onEnter"
      >
        <template #default="{ item }">
          <div class="search-item">
            <span class="word-label">{{ item.word }}</span>
            <el-tag size="small" type="info" class="level-tag">{{ item.level_code }}</el-tag>
            <span class="zh-label">{{ item.zh_definition }}</span>
          </div>
        </template>
      </el-autocomplete>
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
import * as wordApi from '@/api/word'
import type { ForgettingCurveData, WordBank } from '@/api/types'

const searchText = ref('')
const loading = ref(false)
const searched = ref(false)
const data = ref<ForgettingCurveData | null>(null)

interface SearchItem { value: string; word: string; wordId: number; zh_definition: string; level_code: string }

let lastResults: WordBank[] = []

function searchWords(query: string, cb: (results: SearchItem[]) => void) {
  if (!query || query.length < 1) { cb([]); return }
  wordApi.searchWords(query, undefined, 1, 10).then(res => {
    lastResults = res.items
    cb(res.items.map(w => ({
      value: `${w.word}  ${w.level_code}  ${w.zh_definition ?? ''}`,
      word: w.word,
      wordId: w.id,
      zh_definition: w.zh_definition ?? '',
      level_code: w.level_code,
    })))
  }).catch(() => cb([]))
}

function onSelect(item: SearchItem) {
  loadCurve(item.wordId)
}

function onEnter() {
  if (lastResults.length > 0) {
    loadCurve(lastResults[0].id)
  }
}

async function loadCurve(wid: number) {
  loading.value = true
  searched.value = true
  try {
    data.value = await statsApi.forgettingCurve(wid)
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

.search-item { display: flex; align-items: center; gap: 8px; }
.word-label { font-weight: 600; }
.level-tag { flex-shrink: 0; }
.zh-label { color: #6b7280; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 200px; }
</style>
