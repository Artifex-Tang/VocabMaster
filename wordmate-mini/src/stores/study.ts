import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { TodayPlan, Word } from '@/api/types'

export const useStudyStore = defineStore('study', () => {
  const todayPlan = ref<TodayPlan | null>(null)
  const currentLevel = ref<string>('CET4')
  const queue = ref<Word[]>([])
  const currentIdx = ref(0)
  const correctCount = ref(0)
  const sessionStartTime = ref(0)

  function initSession(plan: TodayPlan, level: string) {
    todayPlan.value = plan
    currentLevel.value = level
    queue.value = [...plan.review_words, ...plan.new_words]
    currentIdx.value = 0
    correctCount.value = 0
    sessionStartTime.value = Date.now()
  }

  function nextWord() {
    currentIdx.value++
  }

  function markCorrect() {
    correctCount.value++
  }

  function reset() {
    todayPlan.value = null
    queue.value = []
    currentIdx.value = 0
    correctCount.value = 0
  }

  return {
    todayPlan,
    currentLevel,
    queue,
    currentIdx,
    correctCount,
    sessionStartTime,
    initSession,
    nextWord,
    markCorrect,
    reset,
  }
})
