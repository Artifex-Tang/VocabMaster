<template>
  <view class="choice">
    <view class="progress-bar">
      <view class="progress-inner" :style="{ width: progress + '%' }" />
    </view>
    <text class="counter">{{ currentIdx + 1 }} / {{ questions.length }}</text>

    <view v-if="currentQ" class="question-area">
      <text class="word-text">{{ wordOfCurrentQ }}</text>
      <text class="prompt-label">选择正确释义</text>

      <view class="options">
        <view
          v-for="(opt, i) in currentQ.prompt.options"
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

    <view class="actions" v-if="selected !== null">
      <button class="btn-primary" @click="nextQuestion">
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
const selected = ref<number | null>(null)
const correctIdx = ref<number | null>(null)
const answers = ref<Array<{ question_id: string; answer: string; duration_ms: number }>>([])
const qStartTime = ref(Date.now())

const currentQ = computed(() => questions.value[currentIdx.value])
const progress = computed(() =>
  questions.value.length ? Math.round((currentIdx.value / questions.value.length) * 100) : 0,
)
const isLast = computed(() => currentIdx.value + 1 >= questions.value.length)

const wordOfCurrentQ = computed(() => {
  if (!currentQ.value) return ''
  return String((currentQ.value.prompt as Record<string, unknown>).word ?? '')
})

onLoad((opts) => { testId.value = (opts?.test_id as string) ?? '' })

onMounted(async () => {
  if (!testId.value) return
  const data = await http.get<TestData>(`/test/${testId.value}`)
  questions.value = data.questions
  qStartTime.value = Date.now()
})

function selectOption(i: number) {
  if (selected.value !== null) return
  selected.value = i
  answers.value.push({
    question_id: currentQ.value.question_id,
    answer: currentQ.value.prompt.options?.[i] ?? '',
    duration_ms: Date.now() - qStartTime.value,
  })
}

function getOptionClass(i: number) {
  if (selected.value === null) return ''
  if (i === correctIdx.value) return 'opt-correct'
  if (i === selected.value) return 'opt-wrong'
  return 'opt-dim'
}

async function nextQuestion() {
  if (isLast.value) {
    const result = await http.post<{ accuracy: number }>('/test/submit', {
      test_id: testId.value,
      answers: answers.value,
    })
    uni.redirectTo({
      url: `/pages/study/done?correct=${Math.round(result.accuracy * questions.value.length)}&total=${questions.value.length}`,
    })
    return
  }
  currentIdx.value++
  selected.value = null
  correctIdx.value = null
  qStartTime.value = Date.now()
}
</script>

<style lang="scss" scoped>
.choice {
  min-height: 100vh; background: $color-bg-page; padding: $space-md $space-lg;
  display: flex; flex-direction: column;
}
.progress-bar {
  height: 8rpx; background: $color-border; border-radius: 4rpx; overflow: hidden; margin-bottom: $space-sm;
  .progress-inner { height: 100%; background: $color-primary; border-radius: 4rpx; transition: width 0.3s; }
}
.counter { font-size: $font-sm; color: $color-text-secondary; display: block; margin-bottom: $space-xl; }
.question-area { flex: 1; }
.word-text { font-size: $font-3xl; font-weight: 800; color: $color-text-primary; display: block; margin-bottom: $space-sm; }
.prompt-label { font-size: $font-sm; color: $color-text-secondary; display: block; margin-bottom: $space-lg; }
.options { display: flex; flex-direction: column; gap: $space-md; }
.option {
  background: #fff; border-radius: $radius-lg; padding: $space-md;
  display: flex; align-items: center; gap: $space-md;
  border: 2rpx solid transparent;
  &.opt-correct { border-color: $color-success; background: #F0F9EB; }
  &.opt-wrong { border-color: $color-danger; background: #FEF0F0; }
  &.opt-dim { opacity: 0.4; }
}
.option-idx {
  width: 48rpx; height: 48rpx; border-radius: 50%;
  background: $color-bg-page; display: flex; align-items: center; justify-content: center;
  font-size: $font-sm; font-weight: 700; flex-shrink: 0;
}
.option-text { font-size: $font-md; color: $color-text-primary; flex: 1; }
.actions { padding: $space-lg 0 $space-xl; }
.btn-primary {
  width: 100%; height: 96rpx; background: $color-primary; color: #fff;
  font-size: $font-lg; font-weight: 600; border-radius: $radius-xl; border: none;
}
</style>
