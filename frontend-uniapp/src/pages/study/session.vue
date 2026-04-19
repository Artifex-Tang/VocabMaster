<template>
  <view class="session">
    <!-- 顶部进度 -->
    <view class="header">
      <text class="counter">{{ currentIdx + 1 }} / {{ queue.length }}</text>
      <text class="correct-count">✓ {{ correctCount }}</text>
    </view>
    <view class="progress-bar">
      <view class="progress-inner" :style="{ width: progress + '%' }" />
    </view>

    <!-- 单词卡 -->
    <view class="card-area">
      <word-card
        v-if="currentWord"
        :key="currentWord.id"
        ref="cardRef"
        :word="currentWord"
        @flip="isFlipped = true"
        @audioError="handleAudioError"
      />
      <view v-else-if="loading" class="loading-tip">加载中...</view>
    </view>

    <!-- 操作按钮（翻面后显示） -->
    <view v-if="isFlipped" class="actions">
      <button class="btn btn-wrong" @click="handleAnswer('wrong')">
        <text>✗</text>
        <text class="btn-label">不认识</text>
      </button>
      <button class="btn btn-correct" @click="handleAnswer('correct')">
        <text>✓</text>
        <text class="btn-label">认识</text>
      </button>
    </view>
    <view v-else class="actions-placeholder">
      <text class="flip-hint">↑ 点击卡片翻面</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useStudyStore } from '@/stores/study'
import { getToday } from '@/api/study'
import { useOfflineSync } from '@/composables/useOfflineSync'
import WordCard from '@/components/word-card/word-card.vue'
import type { Word } from '@/api/types'

const studyStore = useStudyStore()
const { submitAnswer } = useOfflineSync()

const level = ref('CET4')
const queue = ref<Word[]>([])
const currentIdx = ref(0)
const isFlipped = ref(false)
const correctCount = ref(0)
const loading = ref(true)
const wordStartTime = ref(Date.now())
const cardRef = ref<InstanceType<typeof WordCard> | null>(null)

const currentWord = computed(() => queue.value[currentIdx.value])
const progress = computed(() =>
  queue.value.length ? Math.round((currentIdx.value / queue.value.length) * 100) : 0,
)

onLoad((opts) => {
  level.value = (opts?.level as string) ?? 'CET4'
})

onMounted(async () => {
  try {
    const plan = await getToday(level.value)
    queue.value = [...plan.review_words, ...plan.new_words]
    studyStore.initSession(plan, level.value)
  } catch {
    uni.showToast({ title: '加载失败，请重试', icon: 'none' })
  } finally {
    loading.value = false
    wordStartTime.value = Date.now()
  }
})

async function handleAnswer(result: 'correct' | 'wrong') {
  const word = currentWord.value
  if (!word) return

  if (result === 'correct') correctCount.value++

  await submitAnswer({
    word_id: word.id,
    level_code: word.level_code,
    result,
    mode: 'card',
    duration_ms: Date.now() - wordStartTime.value,
    client_ts: new Date().toISOString(),
  })

  const isLast = currentIdx.value + 1 >= queue.value.length
  if (isLast) {
    uni.redirectTo({
      url: `/pages/study/done?level=${level.value}&correct=${correctCount.value}&total=${queue.value.length}`,
    })
  } else {
    currentIdx.value++
    isFlipped.value = false
    wordStartTime.value = Date.now()
    cardRef.value?.reset()
  }
}

function handleAudioError() {
  // 静默：已在 word-card 内部降级处理
}
</script>

<style lang="scss" scoped>
.session {
  height: 100vh;
  background: $color-bg-page;
  display: flex;
  flex-direction: column;
  padding: $space-md $space-lg;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $space-sm;

  .counter { font-size: $font-md; color: $color-text-secondary; }
  .correct-count { font-size: $font-md; color: $color-success; font-weight: 600; }
}

.progress-bar {
  height: 8rpx;
  background: $color-border;
  border-radius: 4rpx;
  overflow: hidden;
  margin-bottom: $space-lg;

  .progress-inner {
    height: 100%;
    background: $color-primary;
    border-radius: 4rpx;
    transition: width 0.3s ease;
  }
}

.card-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-tip { font-size: $font-md; color: $color-text-secondary; }

.actions {
  display: flex;
  gap: $space-md;
  padding: $space-md 0 $space-xl;
}
.actions-placeholder {
  height: 160rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.flip-hint { font-size: $font-sm; color: $color-text-placeholder; }

.btn {
  flex: 1;
  height: 120rpx;
  border-radius: $radius-xl;
  border: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: $font-2xl;
  gap: 4rpx;

  .btn-label { font-size: $font-sm; font-weight: 600; }

  &.btn-wrong { background: #FEF0F0; color: $color-danger; }
  &.btn-correct { background: #F0F9EB; color: $color-success; }
}
</style>
