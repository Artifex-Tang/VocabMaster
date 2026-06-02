<template>
  <view class="choice">
    <view class="progress-bar">
      <view class="progress-inner" :style="{ width: progress + '%' }" />
    </view>
    <view class="header-row">
      <text class="counter">{{ currentIdx + 1 }} / {{ questions.length }}</text>
      <text class="skip-btn" @click="skipQuestion">跳过</text>
    </view>

    <view v-if="currentQ" class="question-area">
      <text class="word-text">{{ promptWord }}</text>
      <text class="prompt-label">选择正确释义</text>

      <view class="options">
        <view
          v-for="(opt, i) in options"
          :key="i"
          class="option"
          :class="getOptionClass(i)"
          @click="selectOption(i)"
        >
          <text class="option-idx">{{ ['A','B','C','D'][i] }}</text>
          <text class="option-text">{{ opt }}</text>
        </view>
      </view>
    </view>

    <view class="actions" v-if="submitted">
      <button class="btn-primary" @click="nextQuestion">
        {{ isLast ? '查看结果' : '下一题' }}
      </button>
    </view>

    <view v-else-if="!submitted && questions.length > 0" class="actions">
      <button class="btn-primary" @click="submitAll" :disabled="answers.length < questions.length">
        交卷 ({{ answers.length }}/{{ questions.length }})
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad, onBackPress } from '@dcloudio/uni-app'
import { http } from '@/utils/request'
import { submit as submitTest } from '@/api/test'
import type { TestData, TestQuestion, TestResult } from '@/api/types'

const testId = ref('')
const level = ref('')
const questions = ref<TestQuestion[]>([])
const currentIdx = ref(0)
const selected = ref<number | null>(null)
const answers = ref<Map<string, { answer: string; duration_ms: number }>>(new Map())
const qStartTime = ref(Date.now())
const submitted = ref(false)
const lastResult = ref<TestResult | null>(null)

const currentQ = computed(() => questions.value[currentIdx.value])
const progress = computed(() =>
  questions.value.length ? Math.round((currentIdx.value / questions.value.length) * 100) : 0,
)
const isLast = computed(() => currentIdx.value + 1 >= questions.value.length)

// 兼容 choices(顶层) 和 prompt.options
const options = computed(() => {
  if (!currentQ.value) return []
  return currentQ.value.choices ?? currentQ.value.prompt.options ?? []
})

const promptWord = computed(() => {
  if (!currentQ.value) return ''
  return currentQ.value.prompt.word ?? ''
})

onLoad((opts) => {
  testId.value = (opts?.test_id as string) ?? ''
  level.value = (opts?.level as string) ?? 'CET4'
})

onBackPress(() => {
  if (!submitted.value) {
    uni.showModal({
      title: '确认退出',
      content: '退出后将丢失本次测试进度',
      success: (res) => { if (res.confirm) uni.navigateBack() },
    })
    return true
  }
  return false
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

function selectOption(i: number) {
  if (submitted.value) return
  // 允许改答案
  selected.value = i
  answers.value.set(currentQ.value.question_id, {
    answer: options.value[i] ?? '',
    duration_ms: Date.now() - qStartTime.value,
  })
}

function skipQuestion() {
  if (submitted.value) return
  // 记录空答案
  if (!answers.value.has(currentQ.value.question_id)) {
    answers.value.set(currentQ.value.question_id, {
      answer: '',
      duration_ms: Date.now() - qStartTime.value,
    })
  }
  nextQuestion()
}

function getOptionClass(i: number) {
  if (!lastResult.value) return selected.value === i ? 'opt-selected' : ''
  const detail = lastResult.value.details.find(d => d.question_id === currentQ.value.question_id)
  if (!detail) return ''
  // 交卷后显示正确/错误
  if (options.value[i] === detail.correct_answer) return 'opt-correct'
  if (options.value[i] === detail.user_answer && !detail.correct) return 'opt-wrong'
  return 'opt-dim'
}

function nextQuestion() {
  if (isLast.value) {
    submitAll()
    return
  }
  currentIdx.value++
  selected.value = null
  qStartTime.value = Date.now()
}

async function submitAll() {
  if (submitted.value) {
    // 已提交，跳转结果
    const correct = lastResult.value?.correct ?? 0
    const total = questions.value.length
    uni.redirectTo({
      url: `/pages/study/done?correct=${correct}&total=${total}`,
    })
    return
  }

  try {
    const answerList = Array.from(answers.value.entries()).map(([question_id, a]) => ({
      question_id,
      ...a,
    }))
    const result = await submitTest(testId.value, answerList)
    lastResult.value = result
    submitted.value = true
  } catch {
    uni.showToast({ title: '提交失败', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
.choice {
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
.header-row {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 32rpx;
}
.counter { font-size: 24rpx; color: #9ca3af; }
.skip-btn { font-size: 24rpx; color: #1890ff; }

.question-area { flex: 1; }
.word-text { font-size: 48rpx; font-weight: 800; color: #1f2937; display: block; margin-bottom: 8rpx; }
.prompt-label { font-size: 24rpx; color: #9ca3af; display: block; margin-bottom: 32rpx; }

.options { display: flex; flex-direction: column; gap: 16rpx; }
.option {
  background: #fff; border-radius: 16rpx; padding: 24rpx;
  display: flex; align-items: center; gap: 16rpx;
  border: 2rpx solid transparent;
  &.opt-selected { border-color: #1890ff; background: #e6f7ff; }
  &.opt-correct { border-color: #52c41a; background: #f0f9eb; }
  &.opt-wrong { border-color: #f56c6c; background: #fef0f0; }
  &.opt-dim { opacity: 0.4; }
}
.option-idx {
  width: 48rpx; height: 48rpx; border-radius: 50%;
  background: #f5f7fa; display: flex; align-items: center; justify-content: center;
  font-size: 24rpx; font-weight: 700; flex-shrink: 0;
}
.option-text { font-size: 28rpx; color: #1f2937; flex: 1; }

.actions { padding: 32rpx 0 48rpx; }
.btn-primary {
  width: 100%; height: 96rpx; background: #1890ff; color: #fff;
  font-size: 32rpx; font-weight: 600; border-radius: 48rpx; border: none;
  &[disabled] { opacity: 0.5; }
}
</style>
