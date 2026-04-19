<template>
  <el-table :data="words" size="small" max-height="280">
    <el-table-column prop="word" label="单词" width="140">
      <template #default="{ row }">
        <span class="word-text">{{ row.word }}</span>
      </template>
    </el-table-column>
    <el-table-column prop="zh_definition" label="释义" />
    <el-table-column prop="ipa_uk" label="音标" width="140">
      <template #default="{ row }"><span class="ipa">{{ row.ipa_uk }}</span></template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import type { WordBank } from '@/api/types'
defineProps<{ words: WordBank[] }>()
</script>
