import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as studyApi from '@/api/study'
import type { TodayPlan, WordBank } from '@/api/types'

export const useStudyStore = defineStore('study', () => {
  const todayPlan = ref<TodayPlan | null>(null)
  const sessionQueue = ref<WordBank[]>([])
  const currentIdx = ref(0)
  const correctCount = ref(0)
  const sessionLevel = ref('')

  async function loadTodayPlan(level: string) {
    sessionLevel.value = level
    const data = await studyApi.today(level)
    todayPlan.value = data
    return data
  }

  function startSession(queue: WordBank[], level: string) {
    sessionQueue.value = queue
    currentIdx.value = 0
    correctCount.value = 0
    sessionLevel.value = level
  }

  function advance(correct: boolean) {
    if (correct) correctCount.value++
    currentIdx.value++
  }

  function reset() {
    sessionQueue.value = []
    currentIdx.value = 0
    correctCount.value = 0
  }

  return {
    todayPlan,
    sessionQueue,
    currentIdx,
    correctCount,
    sessionLevel,
    loadTodayPlan,
    startSession,
    advance,
    reset,
  }
})
