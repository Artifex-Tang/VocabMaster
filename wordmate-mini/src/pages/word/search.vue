<template>
  <view class="word-search">
    <!-- 搜索框 -->
    <view class="search-bar">
      <input
        v-model="query"
        class="search-input"
        placeholder="搜索单词..."
        :focus="true"
        @confirm="doSearch"
      />
    </view>

    <!-- 结果列表 -->
    <view v-if="words.length" class="word-list">
      <view v-for="word in words" :key="`${word.id}-${word.level_code}`" class="word-item" @click="showDetail(word)">
        <view class="word-main">
          <text class="word-text">{{ word.word }}</text>
          <text class="word-level">{{ word.level_code }}</text>
        </view>
        <text class="word-ipa">{{ word.ipa_us }}</text>
        <text class="word-def">{{ word.zh_definition }}</text>
      </view>

      <!-- 分页 -->
      <view v-if="hasMore" class="load-more" @click="loadMore">
        <text class="load-more-text">{{ loadingMore ? '加载中...' : '加载更多' }}</text>
      </view>
    </view>

    <view v-else-if="!loading && searched" class="empty">
      <text class="empty-text">没有找到匹配的单词</text>
    </view>

    <!-- 单词详情弹窗 -->
    <view v-if="detailWord" class="modal-mask" @click="detailWord = null">
      <view class="modal-content" @click.stop>
        <text class="detail-word">{{ detailWord.word }}</text>
        <text class="detail-ipa">{{ detailWord.ipa_uk }} / {{ detailWord.ipa_us }}</text>
        <text class="detail-pos">{{ detailWord.pos }}</text>
        <text class="detail-def-en">{{ detailWord.en_definition }}</text>
        <text class="detail-def-zh">{{ detailWord.zh_definition }}</text>
        <view v-if="detailWord.example_en" class="detail-example">
          <text class="example-en">{{ detailWord.example_en }}</text>
          <text class="example-zh">{{ detailWord.example_zh }}</text>
        </view>
        <text class="detail-level">等级：{{ detailWord.level_code }}</text>
        <text class="detail-emoji" v-if="detailWord.emoji">{{ detailWord.emoji }}</text>
        <button class="btn-close" @click="detailWord = null">关闭</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { searchWords } from '@/api/word'
import type { Word } from '@/api/types'

const query = ref('')
const words = ref<Word[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const loadingMore = ref(false)
const searched = ref(false)
const detailWord = ref<Word | null>(null)

const hasMore = computed(() => words.value.length < total.value)

async function doSearch() {
  if (!query.value.trim()) return
  loading.value = true
  page.value = 1
  searched.value = true
  try {
    const result = await searchWords(query.value.trim(), undefined, 1, 20)
    words.value = result.items
    total.value = result.total
  } catch {
    words.value = []
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  page.value++
  try {
    const result = await searchWords(query.value.trim(), undefined, page.value, 20)
    words.value.push(...result.items)
    total.value = result.total
  } finally {
    loadingMore.value = false
  }
}

function showDetail(word: Word) {
  detailWord.value = word
}
</script>

<style lang="scss" scoped>
.word-search {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32rpx;
}

.search-bar {
  background: #fff; border-radius: 16rpx; padding: 0 24rpx;
  height: 80rpx; display: flex; align-items: center;
  margin-bottom: 24rpx;
}
.search-input { width: 100%; font-size: 28rpx; }

.word-list { display: flex; flex-direction: column; gap: 12rpx; }
.word-item {
  background: #fff; border-radius: 16rpx; padding: 24rpx 32rpx;
}
.word-main { display: flex; align-items: baseline; gap: 12rpx; margin-bottom: 4rpx; }
.word-text { font-size: 32rpx; font-weight: 700; color: #1f2937; }
.word-level { font-size: 20rpx; color: #1890ff; background: #e6f7ff; padding: 2rpx 12rpx; border-radius: 8rpx; }
.word-ipa { font-size: 22rpx; color: #9ca3af; display: block; margin-bottom: 4rpx; }
.word-def { font-size: 26rpx; color: #4b5563; display: block; }

.load-more { text-align: center; padding: 24rpx; }
.load-more-text { font-size: 24rpx; color: #1890ff; }

.empty { text-align: center; padding-top: 120rpx; }
.empty-text { font-size: 28rpx; color: #9ca3af; }

.modal-mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); display: flex;
  align-items: center; justify-content: center; z-index: 999;
}
.modal-content {
  background: #fff; border-radius: 24rpx; padding: 40rpx;
  width: 85%; max-height: 80vh; overflow-y: auto;
}
.detail-word { font-size: 44rpx; font-weight: 800; color: #1f2937; display: block; text-align: center; }
.detail-ipa { font-size: 24rpx; color: #9ca3af; display: block; text-align: center; margin-top: 8rpx; }
.detail-pos { font-size: 22rpx; color: #1890ff; display: block; text-align: center; margin-top: 4rpx; }
.detail-def-en { font-size: 26rpx; color: #4b5563; display: block; margin-top: 16rpx; }
.detail-def-zh { font-size: 28rpx; color: #1f2937; font-weight: 600; display: block; margin-top: 8rpx; }
.detail-example { margin-top: 16rpx; padding-top: 16rpx; border-top: 1rpx solid #f0f0f0; }
.example-en { font-size: 24rpx; color: #4b5563; display: block; font-style: italic; }
.example-zh { font-size: 24rpx; color: #9ca3af; display: block; margin-top: 4rpx; }
.detail-level { font-size: 22rpx; color: #9ca3af; display: block; margin-top: 16rpx; text-align: center; }
.detail-emoji { font-size: 64rpx; display: block; text-align: center; margin-top: 12rpx; }

.btn-close {
  margin-top: 24rpx; width: 100%; height: 80rpx; background: #f5f7fa;
  color: #4b5563; font-size: 28rpx; border-radius: 40rpx; border: none;
}
</style>
