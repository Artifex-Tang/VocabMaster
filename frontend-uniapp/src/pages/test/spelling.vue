<template>
  <view class="spelling">
    <view class="progress-bar">
      <view class="progress-inner" :style="{ width: progress + '%' }" />
    </view>
    <text class="counter">{{ currentIdx + 1 }} / {{ questions.length }}</text>

    <view v-if="currentQ" class="question-area">
      <text class="prompt-label">看中文，拼出英文单词</text>
      <text class="zh-def">{{ currentQ.prompt.zh_definition }}</text>

      <view class="input-wrap">
        <input
          v-model="answer"
          class="input"
          placeholder="输入单词..."
          :focus="true"
          @confirm="submitAnswer"
        />
      </view>

      <view class="result-area" v-if="showResult">
        <text class="result-text" :class="isCorrect ? 'correct' : 'wrong'">
          {{ isCorrect ? '✓ 正确！' : `✗ 正确答案：${currentWord}` }}
        </text>
      </view>
    </view>

    <view class="actions">
      <button v-if="!showResult" class="btn-primary" @click="submitAnswer">提交</button>
      <button v-else class="btn-primary" @click="nextQuestion">
        {{ isLast ? '查看结果' : '下一题' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { http } from '@/utils/request'
import type { TestData, TestQuestion } from '@/api/types'

const testId = ref('')
const questions = ref<TestQuestion[]>([])
const currentIdx = ref(0)
const answer = ref('')
const showResult = ref(false)
const isCorrect = ref(false)
const currentWord = ref('')
const correctCount = ref(0)
const answers = ref<Array<{ question_id: string; answer: string; duration_ms: number }>>([])
const qStartTime = ref(Date.now())

const currentQ = computed(() => questions.value[currentIdx.value])
const progress = computed(() =>
  questions.value.length ? Math.round((currentIdx.value / questions.value.length) * 100) : 0,
)
const isLast = computed(() => currentIdx.value + 1 >= questions.value.length)

onLoad((opts) => {
  testId.value = (opts?.test_id as string) ?? ''
})

onMounted(async () => {
  if (!testId.value) return
  const data = await http.get<TestData>(`/test/${testId.value}`)
  questions.value = data.questions
  qStartTime.value = Date.now()
})

function submitAnswer() {
  if (!currentQ.value || !answer.value.trim()) return
  const duration = Date.now() - qStartTime.value
  answers.value.push({
    question_id: currentQ.value.question_id,
    answer: answer.value.trim(),
    duration_ms: duration,
  })
  // 本地校验留给后端，显示结果时从服务端拿
  showResult.value = true
  isCorrect.value = false // 提交后才知道
}

async function nextQuestion() {
  if (isLast.value) {
    const result = await http.post<{ accuracy: number }>(`/test/submit`, {
      test_id: testId.value,
      answers: answers.value,
    })
    uni.redirectTo({
      url: `/pages/study/done?correct=${Math.round(result.accuracy * questions.value.length)}&total=${questions.value.length}`,
    })
    return
  }
  currentIdx.value++
  answer.value = ''
  showResult.value = false
  qStartTime.value = Date.now()
}
</script>

<style lang="scss" scoped>
.spelling {
  min-height: 100vh;
  background: $color-bg-page;
  padding: $space-md $space-lg;
  display: flex;
  flex-direction: column;
}
.progress-bar {
  height: 8rpx; background: $color-border; border-radius: 4rpx; overflow: hidden; margin-bottom: $space-sm;
  .progress-inner { height: 100%; background: $color-primary; border-radius: 4rpx; transition: width 0.3s; }
}
.counter { font-size: $font-sm; color: $color-text-secondary; display: block; margin-bottom: $space-xl; }
.question-area { flex: 1; display: flex; flex-direction: column; gap: $space-lg; }
.prompt-label { font-size: $font-sm; color: $color-text-secondary; display: block; }
.zh-def { font-size: $font-2xl; font-weight: 700; color: $color-text-primary; display: block; }
.input-wrap {
  background: #fff; border-radius: $radius-lg; padding: 0 $space-md;
  height: 96rpx; display: flex; align-items: center;
}
.input { width: 100%; font-size: $font-lg; }
.result-text { font-size: $font-md; font-weight: 600; display: block; margin-top: $space-md; }
.result-text.correct { color: $color-success; }
.result-text.wrong { color: $color-danger; }
.actions { padding: $space-lg 0 $space-xl; }
.btn-primary {
  width: 100%; height: 96rpx; background: $color-primary; color: #fff;
  font-size: $font-lg; font-weight: 600; border-radius: $radius-xl; border: none;
}
</style>
