<template>
  <view class="session">
    <!-- 顶部进度 -->
    <view class="header">
      <text class="counter">{{ currentIdx + 1 }} / {{ queue.length }}</text>
      <text class="unit-label">Unit {{ unit }}</text>
    </view>
    <view class="progress-bar">
      <view class="progress-inner" :style="{ width: progress + '%' }" />
    </view>

    <!-- loading -->
    <view v-if="loading" class="center-tip"><text class="loading-text">加载新词...</text></view>

    <!-- 队列空：本单元无新词 -->
    <view v-else-if="!queue.length && !finished" class="center-tip">
      <text class="done-emoji">✅</text>
      <text class="done-title">本单元新词已全部学过</text>
      <text class="done-sub">{{ hasNext ? `可进入第 ${unit + 1} 单元继续` : '已是最后一个单元，去复习巩固吧' }}</text>
      <view class="done-actions">
        <button class="btn-ghost" @click="backToDetail">返回词库</button>
        <button v-if="hasNext" class="btn-primary" @click="goNextUnit">进入第 {{ unit + 1 }} 单元</button>
      </view>
    </view>

    <!-- 学完一批 -->
    <view v-else-if="finished" class="center-tip">
      <text class="done-emoji">🎉</text>
      <text class="done-title">本单元学习完成</text>
      <text class="done-sub">学了 {{ learnedCount }} 个新词，艾宾浩斯已安排复习</text>
      <view class="done-actions">
        <button class="btn-ghost" @click="backToDetail">返回词库</button>
        <button v-if="hasNext" class="btn-primary" @click="goNextUnit">进入第 {{ unit + 1 }} 单元</button>
        <button v-if="originLevel" class="btn-review" @click="goReview">去复习</button>
      </view>
    </view>

    <!-- 卡片会话 -->
    <template v-else-if="currentWord">
      <view class="card-area">
        <word-card
          :key="currentWord.id"
          ref="cardRef"
          :word="currentWord"
          @flip="onFlip"
        />
      </view>
      <view v-if="isFlipped" class="actions">
        <button class="btn btn-wrong" @click="answer('wrong')">
          <text>✗</text><text class="btn-label">不认识</text>
        </button>
        <button class="btn btn-correct" @click="answer('correct')">
          <text>✓</text><text class="btn-label">认识</text>
        </button>
      </view>
      <view v-else class="actions-placeholder">
        <text class="flip-hint">点击卡片查看释义</text>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { learn, advanceUnit, detail as getDetail } from '@/api/wordList'
import { useSettingsStore } from '@/stores/settings'
import { useOfflineSync } from '@/composables/useOfflineSync'
import { nowIso } from '@/utils/date'
import WordCard from '@/components/word-card/word-card.vue'
import type { Word } from '@/api/types'

const settingsStore = useSettingsStore()
const { submitAnswer } = useOfflineSync()

const id = ref(0)
const unit = ref(1)
const unitCount = ref(0)
const originLevel = ref('')

const loading = ref(true)
const queue = ref<Word[]>([])
const currentIdx = ref(0)
const isFlipped = ref(false)
const finished = ref(false)
const learnedCount = ref(0)
const answerStart = ref(Date.now())
const cardRef = ref<InstanceType<typeof WordCard> | null>(null)

const currentWord = computed(() => queue.value[currentIdx.value])
const progress = computed(() =>
  queue.value.length ? Math.round(((currentIdx.value + 1) / queue.value.length) * 100) : 0,
)
const hasNext = computed(() => unitCount.value > 0 && unit.value < unitCount.value)

onLoad((opts) => {
  id.value = Number(opts?.id ?? 0)
  unit.value = Number(opts?.unit ?? 1)
  loadUnit()
})

async function loadUnit() {
  loading.value = true
  finished.value = false
  learnedCount.value = 0
  currentIdx.value = 0
  isFlipped.value = false
  queue.value = []
  try {
    const limit = settingsStore.settings?.daily_new_words_goal || 20
    const [words, d] = await Promise.all([learn(id.value, unit.value, limit), getDetail(id.value)])
    queue.value = words
    unitCount.value = d.unit_count
    originLevel.value = d.origin_level_code
  } catch {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
    answerStart.value = Date.now()
  }
}

function onFlip() {
  isFlipped.value = true
  answerStart.value = Date.now()
}

function resetCard() {
  isFlipped.value = false
  cardRef.value?.reset?.()
  answerStart.value = Date.now()
}

async function answer(result: 'correct' | 'wrong') {
  const w = currentWord.value
  if (!w) return
  await submitAnswer({
    word_id: w.id,
    level_code: w.level_code,
    result,
    mode: 'card',
    duration_ms: Date.now() - answerStart.value,
    client_ts: nowIso(),
  }).catch(() => {})
  if (result === 'correct') learnedCount.value++
  currentIdx.value++
  if (currentIdx.value >= queue.value.length) {
    finished.value = true
    return
  }
  resetCard()
}

async function goNextUnit() {
  const next = unit.value + 1
  await advanceUnit(id.value, next).catch(() => {})
  unit.value = next
  loadUnit()
}

function goReview() {
  if (!originLevel.value) return
  uni.setStorageSync('test_entry_override', { level: originLevel.value, source: 'due' })
  uni.switchTab({ url: '/pages/test/index' })
}

function backToDetail() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.session {
  height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  padding: 24rpx 32rpx;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
  .counter { font-size: 28rpx; color: #9ca3af; }
  .unit-label { font-size: 28rpx; color: #1890ff; font-weight: 600; }
}

.progress-bar {
  height: 8rpx;
  background: #e5e7eb;
  border-radius: 4rpx;
  overflow: hidden;
  margin-bottom: 32rpx;
  .progress-inner { height: 100%; background: #1890ff; border-radius: 4rpx; transition: width 0.3s ease; }
}

.center-tip {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  text-align: center;
}
.loading-text { font-size: 28rpx; color: #9ca3af; }
.done-emoji { font-size: 96rpx; }
.done-title { font-size: 34rpx; font-weight: 700; color: #1f2937; }
.done-sub { font-size: 26rpx; color: #6b7280; }
.done-actions { display: flex; flex-direction: column; gap: 16rpx; margin-top: 24rpx; width: 80%; }
.btn-primary, .btn-ghost, .btn-review {
  height: 88rpx;
  border-radius: 44rpx;
  border: none;
  font-size: 30rpx;
  font-weight: 600;
}
.btn-primary { background: #1890ff; color: #fff; }
.btn-ghost { background: #f5f7fa; color: #6b7280; border: 2rpx solid #e5e7eb; }
.btn-review { background: #f0f9eb; color: #52c41a; border: 2rpx solid #52c41a; }

.card-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.actions {
  display: flex;
  gap: 24rpx;
  padding: 24rpx 0 48rpx;
}
.actions-placeholder {
  height: 160rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.flip-hint { font-size: 24rpx; color: #ccc; }

.btn {
  flex: 1;
  height: 120rpx;
  border-radius: 48rpx;
  border: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  gap: 4rpx;
  .btn-label { font-size: 24rpx; font-weight: 600; }
  &.btn-wrong { background: #fef0f0; color: #f56c6c; }
  &.btn-correct { background: #f0f9eb; color: #52c41a; }
}
</style>
