<template>
  <div class="ws-page">
    <div class="page-title">词库检索</div>

    <div class="toolbar">
      <el-input
        v-model="query"
        placeholder="输入单词或释义搜索..."
        clearable
        style="width: 280px"
        @keyup.enter="doSearch(1)"
      >
        <template #prefix>
          <Icon icon="mdi:magnify" width="18" />
        </template>
      </el-input>
      <el-select v-model="selectedLevel" placeholder="全部等级" clearable style="width: 160px" @change="doSearch(1)">
        <el-option v-for="lv in LEVELS" :key="lv.code" :label="lv.nameZh" :value="lv.code" />
      </el-select>
      <el-button type="primary" @click="doSearch(1)">搜索</el-button>
    </div>

    <el-table :data="list" v-loading="loading" @row-click="openCard" class="clickable-table">
      <el-table-column prop="word" label="单词" width="160">
        <template #default="{ row }">
          <span class="word-text">{{ row.word }}</span>
          <span v-if="row.emoji" class="row-emoji">{{ row.emoji }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="zh_definition" label="释义" show-overflow-tooltip />
      <el-table-column prop="ipa_uk" label="音标" width="150">
        <template #default="{ row }"><span class="ipa">{{ row.ipa_uk }}</span></template>
      </el-table-column>
      <el-table-column prop="level_code" label="等级" width="100">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.level_code }}</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && searched && !list.length" description="未找到匹配单词" />

    <el-pagination
      v-if="total > pageSize"
      v-model:current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="prev, pager, next"
      style="margin-top: 16px"
      @current-change="doSearch"
    />

    <!-- WordCard dialog -->
    <el-dialog v-model="cardVisible" :title="cardWord?.word" width="440px" destroy-on-close>
      <WordCard v-if="cardWord" :word="cardWord" style="height: 380px" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Icon } from '@iconify/vue'
import WordCard from '@/components/WordCard.vue'
import { LEVELS } from '@/utils/constants'
import * as wordApi from '@/api/word'
import type { WordBank } from '@/api/types'

const query = ref('')
const selectedLevel = ref('')
const list = ref<WordBank[]>([])
const loading = ref(false)
const searched = ref(false)
const page = ref(1)
const pageSize = 20
const total = ref(0)

const cardVisible = ref(false)
const cardWord = ref<WordBank | null>(null)

async function doSearch(p: number) {
  if (!query.value) return
  page.value = p
  loading.value = true
  searched.value = true
  try {
    const res = await wordApi.searchWords(query.value, selectedLevel.value || undefined, page.value, pageSize)
    list.value = res.items
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function openCard(row: WordBank) {
  cardWord.value = row
  cardVisible.value = true
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;
.ws-page { padding: $space-6; }
.page-title { font-size: 1.5rem; font-weight: 700; margin-bottom: $space-6; }
.toolbar { display: flex; gap: $space-3; margin-bottom: $space-4; }

.row-emoji { margin-left: 6px; }

.clickable-table {
  :deep(.el-table__row) { cursor: pointer; }
  :deep(.el-table__row:hover) { background: #f0f7ff; }
}
</style>
