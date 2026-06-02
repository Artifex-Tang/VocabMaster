<template>
  <view class="wrong-book">
    <!-- 等级筛选 -->
    <picker :range="levelNames" @change="onLevelChange">
      <view class="filter-bar">
        <text class="filter-label">{{ selectedLevelName }} ▾</text>
      </view>
    </picker>

    <view v-if="words.length === 0 && !loading" class="empty">
      <text class="empty-emoji">🎉</text>
      <text class="empty-text">错词本是空的，继续保持！</text>
    </view>

    <view v-else class="word-list">
      <view v-for="word in words" :key="word.id" class="word-item">
        <view class="word-main">
          <text class="word-text">{{ word.word }}</text>
          <text class="word-pos">{{ word.pos }}</text>
          <text class="word-ipa">{{ word.ipa_us }}</text>
        </view>
        <text class="word-def">{{ word.zh_definition }}</text>
        <view class="word-actions">
          <text class="action-btn" @click="removeWord(word.id)">移除</text>
        </view>
      </view>
      <!-- 加载更多 -->
      <view v-if="hasMore" class="load-more" @click="loadMore">
        <text class="load-more-text">{{ loadingMore ? '加载中...' : '加载更多' }}</text>
      </view>
    </view>

    <button v-if="words.length > 0" class="btn-review" @click="startReview">
      开始错词复习（{{ total }} 词）
    </button>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getWrongWords, resolveWrongWord, startWrongWordReview } from '@/api/word'
import { useSettingsStore } from '@/stores/settings'
import { useStudyStore } from '@/stores/study'
import { LEVELS } from '@/api/types'
import type { Word } from '@/api/types'

const settingsStore = useSettingsStore()
const studyStore = useStudyStore()
const words = ref<Word[]>([])
const loading = ref(true)
const loadingMore = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 20

const activeLevels = computed(() => settingsStore.settings.active_levels)
const levelIdx = ref(0)
const selectedLevel = computed(() => activeLevels.value[levelIdx.value] ?? '')
const selectedLevelName = computed(() => {
  const l = LEVELS.find(lv => lv.code === selectedLevel.value)
  return l ? `${l.name_en} (${l.name_zh})` : selectedLevel.value
})
const levelNames = computed(() => activeLevels.value.map(code => {
  const l = LEVELS.find(lv => lv.code === code)
  return l ? `${l.name_en} (${l.name_zh})` : code
}))

const hasMore = computed(() => words.value.length < total.value)

onShow(loadData)

async function loadData() {
  loading.value = true
  page.value = 1
  try {
    const result = await getWrongWords(selectedLevel.value, 1, pageSize)
    words.value = result.items
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  page.value++
  try {
    const result = await getWrongWords(selectedLevel.value, page.value, pageSize)
    words.value.push(...result.items)
    total.value = result.total
  } finally {
    loadingMore.value = false
  }
}

function onLevelChange(e: { detail: { value: number } }) {
  levelIdx.value = e.detail.value
  loadData()
}

async function removeWord(id: number) {
  try {
    await resolveWrongWord(id)
    words.value = words.value.filter(w => w.id !== id)
    total.value--
    uni.showToast({ title: '已移除', icon: 'success' })
  } catch {
    // request.ts 已 toast 错误
  }
}

async function startReview() {
  const level = selectedLevel.value
  uni.showLoading({ title: '加载...' })
  try {
    const data = await startWrongWordReview(level)
    // 将错词放入学习 store 走学习流程
    studyStore.initSession(
      { date: '', review_words: data.words, new_words: [], review_count: data.words.length, new_count: 0, estimated_minutes: Math.ceil(data.words.length * 0.5) },
      level,
    )
    uni.navigateTo({ url: `/pages/study/session?level=${level}` })
  } finally {
    uni.hideLoading()
  }
}
</script>

<style lang="scss" scoped>
.wrong-book {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 32rpx;
}

.filter-bar {
  background: #fff; border-radius: 16rpx; padding: 20rpx 24rpx;
  margin-bottom: 16rpx;
}
.filter-label { font-size: 24rpx; color: #1890ff; }

.empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding-top: 200rpx; gap: 24rpx;
}
.empty-emoji { font-size: 120rpx; }
.empty-text { font-size: 28rpx; color: #9ca3af; }

.word-list { display: flex; flex-direction: column; gap: 12rpx; margin-bottom: 32rpx; }
.word-item {
  background: #fff; border-radius: 16rpx; padding: 24rpx 32rpx;
}
.word-main { display: flex; align-items: baseline; gap: 12rpx; margin-bottom: 4rpx; }
.word-text { font-size: 32rpx; font-weight: 700; color: #1f2937; }
.word-pos { font-size: 20rpx; color: #1890ff; }
.word-ipa { font-size: 20rpx; color: #9ca3af; }
.word-def { font-size: 24rpx; color: #6b7280; display: block; margin-bottom: 8rpx; }

.word-actions { display: flex; justify-content: flex-end; }
.action-btn { font-size: 24rpx; color: #f56c6c; padding: 4rpx 16rpx; }

.load-more { text-align: center; padding: 24rpx; }
.load-more-text { font-size: 24rpx; color: #1890ff; }

.btn-review {
  width: 100%; height: 96rpx; background: #1890ff; color: #fff;
  font-size: 32rpx; font-weight: 600; border-radius: 48rpx; border: none;
}
</style>
