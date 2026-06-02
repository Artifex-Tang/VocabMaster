<template>
  <view class="listening">
    <view class="progress-bar">
      <view class="progress-inner" :style="{ width: progress + '%' }" />
    </view>
    <view class="header-row">
      <text class="counter">{{ currentIdx + 1 }} / {{ questions.length }}</text>
      <text class="skip-btn" @click="skipQuestion">跳过</text>
    </view>

    <view v-if="currentQ" class="question-area">
      <text class="prompt-label">听发音，写出单词</text>

      <view class="audio-area">
        <view class="play-btn" @click="playAudio">
          <text class="play-icon">{{ playing ? '⏸' : '🔊' }}</text>
        </view>
        <text class="play-hint">点击播放发音</text>
      </view>

      <view class="input-wrap">
        <input
          v-model="answer"
          class="input"
          placeholder="输入单词..."
          :focus="true"
          @confirm="recordAnswer"
        />
      </view>

      <view class="result-area" v-if="showResult">
        <text class="result-text" :class="isCorrect ? 'correct' : 'wrong'">
          {{ isCorrect ? '✓ 正确！' : `✗ 正确答案：${correctWord}` }}
        </text>
      </view>
    </view>

    <view class="actions">
      <button v-if="!showResult" class="btn-primary" @click="recordAnswer" :disabled="!answer.trim()">
        提交
      </button>
      <button v-else class="btn-primary" @click="nextQuestion">
        {{ isLast ? '交卷' : '下一题' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { http } from '@/utils/request'
import { submit as submitTest } from '@/api/test'
import { speak } from '@/utils/tts'
import { useSettingsStore } from '@/stores/settings'
import type { TestData, TestQuestion, TestResult } from '@/api/types'

const settingsStore = useSettingsStore()

const testId = ref('')
const questions = ref<TestQuestion[]>([])
const currentIdx = ref(0)
const answer = ref('')
const showResult = ref(false)
const isCorrect = ref(false)
const correctWord = ref('')
const playing = ref(false)
const answers = ref<Array<{ question_id: string; answer: string; duration_ms: number }>>([])
const qStartTime = ref(Date.now())
const submitted = ref(false)
const lastResult = ref<TestResult | null>(null)

const currentQ = computed(() => questions.value[currentIdx.value])
const progress = computed(() =>
  questions.value.length ? Math.round((currentIdx.value / questions.value.length) * 100) : 0,
)
const isLast = computed(() => currentIdx.value + 1 >= questions.value.length)

onLoad((opts) => { testId.value = (opts?.test_id as string) ?? '' })

// 加载题目并自动播放
;(async () => {
  if (!testId.value) return
  try {
    const data = await http.get<TestData>(`/test/${testId.value}`)
    questions.value = data.questions
    qStartTime.value = Date.now()
    playAudio()
  } catch {
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
})()

async function playAudio() {
  if (!currentQ.value || playing.value) return
  const accent = settingsStore.settings.preferred_accent
  const url = accent === 'uk' ? currentQ.value.prompt.audio_url_uk : currentQ.value.prompt.audio_url_us
  const word = currentQ.value.prompt.word ?? ''
  playing.value = true
  try {
    await speak({ text: word, accent, audioUrl: url ?? undefined })
  } finally {
    playing.value = false
  }
}

function recordAnswer() {
  if (!answer.value.trim()) return
  answers.value.push({
    question_id: currentQ.value.question_id,
    answer: answer.value.trim(),
    duration_ms: Date.now() - qStartTime.value,
  })
  showResult.value = true
}

function skipQuestion() {
  answers.value.push({
    question_id: currentQ.value.question_id,
    answer: '',
    duration_ms: Date.now() - qStartTime.value,
  })
  nextQuestion()
}

async function nextQuestion() {
  if (isLast.value || submitted.value) {
    await submitAll()
    return
  }
  currentIdx.value++
  answer.value = ''
  showResult.value = false
  qStartTime.value = Date.now()
  await playAudio()
}

async function submitAll() {
  if (submitted.value) {
    const correct = lastResult.value?.correct ?? 0
    const total = questions.value.length
    uni.redirectTo({ url: `/pages/study/done?correct=${correct}&total=${total}` })
    return
  }
  try {
    const result = await submitTest(testId.value, answers.value)
    lastResult.value = result
    submitted.value = true
    const detail = result.details.find(d => d.question_id === currentQ.value?.question_id)
    if (detail) {
      isCorrect.value = detail.correct
      correctWord.value = detail.correct_answer ?? ''
    }
    showResult.value = true
  } catch {
    uni.showToast({ title: '提交失败', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
.listening {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24rpx 32rpx;
  display: flex;
  flex-direction: column;
}
.progress-bar {
  height: 8rpx; background: #e5e7eb; border-radius: 4rpx; overflow: hidden; margin-bottom: 16rpx;
  .progress-inner { height: 100%; background: #1890ff; border-radius: 4rpx; transition: width 0.3s; }
}
.header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 32rpx; }
.counter { font-size: 24rpx; color: #9ca3af; }
.skip-btn { font-size: 24rpx; color: #1890ff; }

.question-area { flex: 1; display: flex; flex-direction: column; gap: 24rpx; }
.prompt-label { font-size: 24rpx; color: #9ca3af; display: block; }

.audio-area {
  display: flex; flex-direction: column; align-items: center; gap: 16rpx;
  padding: 48rpx 0;
}
.play-btn {
  width: 160rpx; height: 160rpx; border-radius: 50%;
  background: #1890ff;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8rpx 32rpx rgba(24,144,255,0.3);
}
.play-icon { font-size: 72rpx; }
.play-hint { font-size: 24rpx; color: #9ca3af; }

.input-wrap {
  background: #fff; border-radius: 16rpx; padding: 0 24rpx;
  height: 96rpx; display: flex; align-items: center;
}
.input { width: 100%; font-size: 32rpx; }

.result-text { font-size: 28rpx; font-weight: 600; display: block; }
.result-text.correct { color: #52c41a; }
.result-text.wrong { color: #f56c6c; }

.actions { padding: 32rpx 0 48rpx; }
.btn-primary {
  width: 100%; height: 96rpx; background: #1890ff; color: #fff;
  font-size: 32rpx; font-weight: 600; border-radius: 48rpx; border: none;
  &[disabled] { opacity: 0.5; }
}
</style>
