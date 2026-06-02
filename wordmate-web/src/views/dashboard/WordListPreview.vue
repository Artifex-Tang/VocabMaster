<template>
  <el-table :data="words" max-height="360">
    <el-table-column prop="word" label="单词" width="150">
      <template #default="{ row }">
        <span class="word-text">{{ row.word }}</span>
      </template>
    </el-table-column>
    <el-table-column prop="zh_definition" label="释义">
      <template #default="{ row }">
        <span class="def-text">{{ row.zh_definition }}</span>
      </template>
    </el-table-column>
    <el-table-column prop="ipa_uk" label="音标" width="150">
      <template #default="{ row }"><span class="ipa">{{ row.ipa_uk }}</span></template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import type { WordBank } from '@/api/types'
defineProps<{ words: WordBank[] }>()
</script>

<style scoped>
.word-text { font-size: 15px; font-weight: 600; }
.def-text { font-size: 14px; line-height: 1.6; }
.ipa { font-size: 13px; color: #6b7280; font-family: 'Lucida Console', monospace; }
</style>
