<template>
  <div class="ww-page">
    <div class="page-title">错词本</div>

    <div class="toolbar">
      <el-select v-model="selectedLevel" placeholder="全部等级" clearable style="width: 160px">
        <el-option v-for="lv in LEVELS" :key="lv.code" :label="lv.nameZh" :value="lv.code" />
      </el-select>
      <el-button type="primary" @click="startReview" :disabled="!list.length">
        开始复习错词
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading">
      <el-table-column prop="word" label="单词" width="150">
        <template #default="{ row }"><span class="word-text">{{ row.word }}</span></template>
      </el-table-column>
      <el-table-column prop="zh_definition" label="释义" />
      <el-table-column prop="ipa_uk" label="音标" width="140">
        <template #default="{ row }"><span class="ipa">{{ row.ipa_uk }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link size="small" @click="remove(row.id)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > pageSize"
      v-model:current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="prev, pager, next"
      style="margin-top: 16px"
      @current-change="loadList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useStudyStore } from '@/stores/study'
import { LEVELS } from '@/utils/constants'
import * as wrongWordApi from '@/api/wrong-word'
import type { WordBank } from '@/api/types'

const router = useRouter()
const studyStore = useStudyStore()

const selectedLevel = ref('')
const list = ref<WordBank[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = 20
const total = ref(0)

async function loadList() {
  loading.value = true
  try {
    const res = await wrongWordApi.list(selectedLevel.value || undefined, 0, page.value, pageSize)
    list.value = res.items
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function remove(id: number) {
  await wrongWordApi.resolve(id)
  await loadList()
}

async function startReview() {
  const res = await wrongWordApi.startReview(selectedLevel.value || undefined)
  studyStore.startSession(res.review_words, selectedLevel.value || 'mixed')
  router.push(`/study/${selectedLevel.value || 'mixed'}`)
}

onMounted(loadList)
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;
.ww-page { padding: $space-6; }
.page-title { font-size: 1.5rem; font-weight: 700; margin-bottom: $space-6; }
.toolbar { display: flex; gap: $space-3; margin-bottom: $space-4; }
</style>
