<template>
  <div class="wordlist-square">
    <h2 class="page-title">词库广场</h2>
    <p class="page-desc">选一本教材词库，按单元系统背单词，艾宾浩斯自动安排跨单元复习。</p>

    <div v-loading="loading" class="list-grid">
      <el-card
        v-for="l in lists"
        :key="l.id"
        class="list-card"
        shadow="hover"
        @click="goDetail(l.id)"
      >
        <div class="card-emoji">{{ l.cover_emoji || '📘' }}</div>
        <div class="card-name">{{ l.name }}</div>
        <div class="card-meta">{{ l.word_count }} 词</div>
        <div class="card-desc">{{ l.description || '教材词库' }}</div>
        <el-button
          :type="l.subscribed ? 'success' : 'primary'"
          :plain="l.subscribed"
          size="small"
          :loading="subscribing === l.id"
          @click.stop="onSubscribe(l)"
        >
          <Icon :icon="l.subscribed ? 'mdi:check' : 'mdi:plus'" width="14" />
          {{ l.subscribed ? '已订阅' : '订阅' }}
        </el-button>
      </el-card>
      <el-empty v-if="!loading && !lists.length" description="暂无词库" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Icon } from '@iconify/vue'
import { square, subscribe } from '@/api/wordList'
import type { WordListSummary } from '@/api/types'

const router = useRouter()
const lists = ref<WordListSummary[]>([])
const loading = ref(false)
const subscribing = ref<number | null>(null)

async function load() {
  loading.value = true
  try {
    lists.value = await square()
  } finally {
    loading.value = false
  }
}

function goDetail(id: number) {
  router.push(`/wordlists/${id}`)
}

async function onSubscribe(l: WordListSummary) {
  subscribing.value = l.id
  try {
    await subscribe(l.id)
    l.subscribed = true
    ElMessage.success(`已订阅「${l.name}」，进入词库开始学习`)
    goDetail(l.id)
  } finally {
    subscribing.value = null
  }
}

onMounted(load)
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.wordlist-square {
  max-width: 900px;
  margin: 0 auto;
  padding: $space-6;
}
.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: $space-2;
}
.page-desc {
  color: #6b7280;
  margin-bottom: $space-6;
  font-size: 0.9rem;
}
.list-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: $space-4;
}
.list-card {
  cursor: pointer;
  text-align: center;
  transition: transform 0.15s;
  &:hover {
    transform: translateY(-2px);
  }
  :deep(.el-card__body) {
    padding: $space-4;
  }
}
.card-emoji {
  font-size: 2.5rem;
  margin-bottom: $space-2;
}
.card-name {
  font-size: 1.15rem;
  font-weight: 600;
  margin-bottom: $space-1;
}
.card-meta {
  font-size: 0.8rem;
  color: $color-primary;
  margin-bottom: $space-1;
}
.card-desc {
  font-size: 0.75rem;
  color: #9ca3af;
  margin-bottom: $space-3;
  min-height: 1.2em;
}
</style>
