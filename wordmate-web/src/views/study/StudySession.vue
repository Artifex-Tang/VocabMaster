<template>
  <div class="study-session">
    <!-- Navigation bar -->
    <div class="nav-bar">
      <el-button text @click="confirmExit">
        <Icon icon="mdi:arrow-left" width="20" />
        返回
      </el-button>
      <el-button :disabled="currentIdx <= 0" text @click="goPrev">
        <Icon icon="mdi:chevron-left" width="22" />
      </el-button>
      <el-progress :percentage="progressPct" :show-text="false" style="flex:1;margin:0 8px" />
      <span class="progress-text">{{ currentIdx + 1 }} / {{ queue.length }}</span>
      <el-button :disabled="currentIdx >= queue.length - 1" text @click="goNext">
        <Icon icon="mdi:chevron-right" width="22" />
      </el-button>
      <div class="correct-info">
        <Icon icon="mdi:check-circle-outline" width="16" color="#10b981" />
        {{ correctCount }}
      </div>
    </div>

    <!-- Card area -->
    <div v-if="currentWord" class="card-area">
      <WordCard
        ref="cardRef"
        :key="currentWord.id"
        :word="currentWord"
        @flip="isFlipped = true"
        style="height: 380px; width: 100%"
      />
    </div>

    <!-- Action buttons (visible after flip) -->
    <Transition name="fade">
      <div v-if="isFlipped" class="action-bar">
        <el-button
          class="action-btn wrong"
          size="large"
          @click="handleAnswer('wrong')"
        >
          <Icon icon="mdi:close" width="22" />
          不认识
          <kbd>1</kbd>
        </el-button>
        <el-button
          class="action-btn skip"
          size="large"
          @click="handleAnswer('skip')"
        >
          <Icon icon="mdi:skip-next" width="22" />
          跳过
          <kbd>3</kbd>
        </el-button>
        <el-button
          class="action-btn correct"
          type="success"
          size="large"
          @click="handleAnswer('correct')"
        >
          <Icon icon="mdi:check" width="22" />
          认识
          <kbd>2</kbd>
        </el-button>
      </div>
    </Transition>

    <!-- Pre-flip hint -->
    <div v-if="!isFlipped && currentWord" class="flip-instruction">
      按 <kbd>Space</kbd> 翻面 · <kbd>←</kbd><kbd>→</kbd> 切换单词 · <kbd>P</kbd> 发音
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Icon } from '@iconify/vue'
import WordCard from '@/components/WordCard.vue'
import { useStudyStore } from '@/stores/study'
import { useOfflineSync } from '@/composables/useOfflineSync'
import { nowIso } from '@/utils/date'

const router = useRouter()
const route = useRoute()
const studyStore = useStudyStore()
const { submitAnswer } = useOfflineSync()

const cardRef = ref<InstanceType<typeof WordCard> | null>(null)
const isFlipped = ref(false)
const answerStartTime = ref(Date.now())

const level = route.params.level as string
const queue = studyStore.sessionQueue
const currentIdx = computed(() => studyStore.currentIdx)
const correctCount = computed(() => studyStore.correctCount)
const currentWord = computed(() => queue[currentIdx.value])
const progressPct = computed(() =>
  queue.length ? Math.round(((currentIdx.value + 1) / queue.length) * 100) : 0,
)

function resetCardState() {
  isFlipped.value = false
  answerStartTime.value = Date.now()
  cardRef.value?.reset()
}

function goPrev() {
  if (currentIdx.value > 0) {
    studyStore.goTo(currentIdx.value - 1)
    resetCardState()
  }
}

function goNext() {
  if (currentIdx.value < queue.length - 1) {
    studyStore.goTo(currentIdx.value + 1)
    resetCardState()
  }
}

async function handleAnswer(result: 'correct' | 'wrong' | 'skip') {
  if (!currentWord.value) return

  const duration = Date.now() - answerStartTime.value

  await submitAnswer({
    word_id: currentWord.value.id,
    level_code: currentWord.value.level_code,
    result,
    mode: 'card',
    duration_ms: duration,
    client_ts: nowIso(),
  }).catch(() => {})

  studyStore.advance(result === 'correct')

  if (currentIdx.value >= queue.length) {
    router.replace({
      path: `/study/${level}/done`,
      query: {
        correct: String(correctCount.value),
        total: String(queue.length),
      },
    })
    return
  }

  resetCardState()
}

function handleKeydown(e: KeyboardEvent) {
  if (e.target instanceof HTMLInputElement) return
  switch (e.code) {
    case 'Space':
      e.preventDefault()
      cardRef.value?.flip()
      break
    case 'ArrowLeft':
      e.preventDefault()
      goPrev()
      break
    case 'ArrowRight':
      e.preventDefault()
      goNext()
      break
    case 'Digit1':
      if (isFlipped.value) handleAnswer('wrong')
      break
    case 'Digit2':
      if (isFlipped.value) handleAnswer('correct')
      break
    case 'Digit3':
      if (isFlipped.value) handleAnswer('skip')
      break
    case 'KeyP':
      cardRef.value?.playAudio()
      break
  }
}

async function confirmExit() {
  await ElMessageBox.confirm('确定退出本次学习？进度将已记录。', '退出', {
    confirmButtonText: '退出',
    cancelButtonText: '继续',
    type: 'warning',
  })
  studyStore.reset()
  router.push('/dashboard')
}

onMounted(() => {
  if (!queue.length) {
    router.replace('/dashboard')
    return
  }
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.study-session {
  max-width: 640px;
  margin: 0 auto;
  padding: $space-4 $space-4 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.nav-bar {
  display: flex;
  align-items: center;
  gap: $space-2;
  margin-bottom: $space-3;
}

.progress-text {
  font-family: $font-en;
  font-size: 0.9rem;
  color: #374151;
  white-space: nowrap;
}

.correct-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.9rem;
  color: #10b981;
}

.card-area {
  flex: 1;
  padding: $space-4 0;
}

.action-bar {
  display: flex;
  gap: $space-3;
  padding: $space-4 0;
  justify-content: center;

  .action-btn {
    flex: 1;
    max-width: 160px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    height: 64px;
    font-size: 1rem;

    &.wrong { border-color: $color-error; color: $color-error; }
    &.skip { border-color: #d1d5db; color: #6b7280; }
    &.correct { }

    kbd {
      font-size: 0.65rem;
      background: rgba(0,0,0,0.06);
      border-radius: 3px;
      padding: 0 5px;
      font-family: $font-en;
    }
  }
}

.flip-instruction {
  text-align: center;
  font-size: 0.8rem;
  color: #9ca3af;
  padding: $space-3 0 $space-4;

  kbd {
    background: #f3f4f6;
    border: 1px solid #d1d5db;
    border-radius: 3px;
    padding: 1px 5px;
    font-size: 0.75rem;
  }
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
