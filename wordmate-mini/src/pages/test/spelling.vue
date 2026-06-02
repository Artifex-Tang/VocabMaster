<template>
  <view class="spelling">
    <view class="progress-bar">
      <view class="progress-inner" :style="{ width: progress + '%' }" />
    </view>
    <view class="header-row">
      <text class="counter">{{ currentIdx + 1 }} / {{ questions.length }}</text>
      <text class="skip-btn" @click="skipQuestion">跳过</text>
    </view>

    <view v-if="currentQ" class="question-area">
      <!-- 中/英切换 -->
      <view class="lang-toggle">
        <text :class="['toggle-btn', promptLang === 'zh' && 'active']" @click="promptLang = 'zh'">中文释义</text>
        <text :class="['toggle-btn', promptLang === 'en' && 'active']" @click="promptLang = 'en'">英文释义</text>
      </view>

      <text class="prompt-text">{{ promptText }}</text>

      <!-- 音频播放 -->
      <text class="audio-btn" @click="playAudio">🔊 播放发音</text>

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
const answers = ref<Array<{ question_id: string; answer: string; duration_ms: number }>>([])
const qStartTime = ref(Date.now())
const promptLang = ref<'zh' | 'en'>('zh')
const submitted = ref(false)
const lastResult = ref<TestResult | null>(null)

const currentQ = computed(() => questions.value[currentIdx.value])
const progress = computed(() =>
  questions.value.length ? Math.round((currentIdx.value / questions.value.length) * 100) : 0,
)
const isLast = computed(() => currentIdx.value + 1 >= questions.value.length)

const promptText = computed(() => {
  if (!currentQ.value) return ''
  return promptLang.value === 'zh'
    ? (currentQ.value.prompt.zh_definition ?? '')
    : (currentQ.value.prompt.en_definition ?? '')
})

onLoad((opts) => {
  testId.value = (opts?.test_id as string) ?? ''
})

// 加载题目
;(async () => {
  if (!testId.value) return
  try {
    const data = await http.get<TestData>(`/test/${testId.value}`)
    questions.value = data.questions
    qStartTime.value = Date.now()
  } catch {
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
})()

function playAudio() {
  if (!currentQ.value) return
  const accent = settingsStore.settings.preferred_accent
  const url = accent === 'uk' ? currentQ.value.prompt.audio_url_uk : currentQ.value.prompt.audio_url_us
  const word = currentQ.value.prompt.word ?? ''
  speak({ text: word, accent, audioUrl: url ?? undefined })
}

function recordAnswer() {
  if (!currentQ.value || !answer.value.trim()) return
  answers.value.push({
    question_id: currentQ.value.question_id,
    answer: answer.value.trim(),
    duration_ms: Date.now() - qStartTime.value,
  })
  // 暂不显示正确性，交给后端判断
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
    // 显示当前题结果
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
.spelling {
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

.lang-toggle { display: flex; gap: 16rpx; }
.toggle-btn {
  font-size: 24rpx; color: #9ca3af; padding: 8rpx 20rpx; border-radius: 12rpx;
  background: #fff;
}
.toggle-btn.active { color: #1890ff; background: #e6f7ff; font-weight: 600; }

.prompt-text { font-size: 36rpx; font-weight: 700; color: #1f2937; display: block; }

.audio-btn {
  font-size: 28rpx; color: #1890ff; padding: 12rpx 0;
}

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
