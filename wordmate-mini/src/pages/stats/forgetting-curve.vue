<template>
  <view class="forgetting-curve">
    <!-- 搜索 -->
    <view class="search-bar">
      <input
        v-model="query"
        class="search-input"
        placeholder="搜索单词查看遗忘曲线"
        @confirm="doSearch"
        @input="onInput"
      />
    </view>

    <!-- 搜索建议 -->
    <view v-if="suggestions.length" class="suggestions">
      <view v-for="s in suggestions" :key="s.id" class="suggestion-item" @click="selectWord(s)">
        <text class="sug-word">{{ s.word }}</text>
        <text class="sug-level">{{ s.level_code }}</text>
      </view>
    </view>

    <!-- 选中单词信息 -->
    <view v-if="selectedWord" class="word-info">
      <text class="word-title">{{ selectedWord.word }}</text>
      <text class="word-ipa">{{ selectedWord.ipa_us }}</text>
      <text class="word-def">{{ selectedWord.zh_definition }}</text>
    </view>

    <!-- 遗忘曲线图 -->
    <view v-if="curveData" class="chart-card">
      <text class="chart-title">遗忘曲线</text>
      <text class="chart-sub">复习次数：{{ curveData.reviews.length }}</text>

      <!-- 简化版曲线：用进度条表示各阶段 -->
      <view class="curve-stages">
        <view
          v-for="(stage, i) in curveData.theoretical_curve.stages"
          :key="i"
          class="stage-row"
        >
          <text class="stage-label">阶段{{ i + 1 }}</text>
          <view class="stage-bar-wrap">
            <view class="stage-bar" :style="{ width: stage + '%' }" />
          </view>
          <text class="stage-pct">{{ stage }}%</text>
        </view>
      </view>

      <!-- 实际复习记录 -->
      <view v-if="curveData.reviews.length" class="reviews">
        <text class="reviews-title">复习记录</text>
        <view v-for="r in curveData.reviews" :key="r.ts" class="review-item">
          <text class="review-time">{{ r.ts.slice(0, 16) }}</text>
          <text :class="['review-result', r.result === 'correct' ? 'correct' : 'wrong']">
            {{ r.result === 'correct' ? '✓' : '✗' }}
          </text>
          <text class="review-stage">阶段{{ r.stage_after }}</text>
        </view>
      </view>
    </view>

    <view v-if="!curveData && !loading" class="empty">
      <text class="empty-text">搜索一个单词查看它的遗忘曲线</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { searchWords } from '@/api/word'
import { getForgettingCurve } from '@/api/stats'
import type { Word } from '@/api/types'

const query = ref('')
const suggestions = ref<Word[]>([])
const selectedWord = ref<Word | null>(null)
const curveData = ref<{
  word_id: number
  word: string
  reviews: Array<{ ts: string; result: string; stage_after: number }>
  theoretical_curve: { type: string; stages: number[] }
} | null>(null)
const loading = ref(false)

let debounceTimer: ReturnType<typeof setTimeout> | null = null

function onInput() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(async () => {
    if (!query.value.trim()) {
      suggestions.value = []
      return
    }
    try {
      const result = await searchWords(query.value.trim(), undefined, 1, 10)
      suggestions.value = result.items
    } catch {
      suggestions.value = []
    }
  }, 300)
}

async function doSearch() {
  if (!query.value.trim()) return
  try {
    const result = await searchWords(query.value.trim(), undefined, 1, 1)
    if (result.items.length) {
      selectWord(result.items[0])
    }
  } catch {
    // 静默
  }
}

async function selectWord(word: Word) {
  selectedWord.value = word
  suggestions.value = []
  query.value = word.word
  loading.value = true
  try {
    curveData.value = await getForgettingCurve(word.id)
  } catch {
    curveData.value = null
    uni.showToast({ title: '暂无数据', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.forgetting-curve {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32rpx;
}

.search-bar {
  background: #fff; border-radius: 16rpx; padding: 0 24rpx;
  height: 80rpx; display: flex; align-items: center;
  margin-bottom: 16rpx;
}
.search-input { width: 100%; font-size: 28rpx; }

.suggestions {
  background: #fff; border-radius: 16rpx; overflow: hidden;
  margin-bottom: 16rpx; max-height: 400rpx; overflow-y: auto;
}
.suggestion-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20rpx 24rpx; border-bottom: 1rpx solid #f5f7fa;
}
.sug-word { font-size: 28rpx; color: #1f2937; font-weight: 500; }
.sug-level { font-size: 20rpx; color: #1890ff; background: #e6f7ff; padding: 4rpx 12rpx; border-radius: 8rpx; }

.word-info {
  background: #fff; border-radius: 24rpx; padding: 32rpx;
  margin-bottom: 16rpx; text-align: center;
}
.word-title { font-size: 40rpx; font-weight: 800; color: #1f2937; display: block; }
.word-ipa { font-size: 24rpx; color: #9ca3af; display: block; margin-top: 8rpx; }
.word-def { font-size: 28rpx; color: #4b5563; display: block; margin-top: 12rpx; }

.chart-card {
  background: #fff; border-radius: 24rpx; padding: 32rpx;
}
.chart-title { font-size: 28rpx; font-weight: 600; color: #1f2937; display: block; }
.chart-sub { font-size: 24rpx; color: #9ca3af; display: block; margin-bottom: 24rpx; }

.curve-stages { margin-bottom: 32rpx; }
.stage-row { display: flex; align-items: center; gap: 12rpx; margin-bottom: 12rpx; }
.stage-label { font-size: 20rpx; color: #9ca3af; width: 80rpx; }
.stage-bar-wrap { flex: 1; height: 16rpx; background: #f5f7fa; border-radius: 8rpx; overflow: hidden; }
.stage-bar { height: 100%; background: #1890ff; border-radius: 8rpx; }
.stage-pct { font-size: 20rpx; color: #9ca3af; width: 64rpx; text-align: right; }

.reviews { margin-top: 24rpx; }
.reviews-title { font-size: 24rpx; font-weight: 600; color: #1f2937; display: block; margin-bottom: 16rpx; }
.review-item { display: flex; align-items: center; gap: 12rpx; margin-bottom: 8rpx; }
.review-time { font-size: 20rpx; color: #9ca3af; flex: 1; }
.review-result { font-size: 24rpx; font-weight: 700; }
.review-result.correct { color: #52c41a; }
.review-result.wrong { color: #f56c6c; }
.review-stage { font-size: 20rpx; color: #9ca3af; }

.empty { text-align: center; padding-top: 120rpx; }
.empty-text { font-size: 28rpx; color: #9ca3af; }
</style>
