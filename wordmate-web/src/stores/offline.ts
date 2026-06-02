import { defineStore } from 'pinia'
import { ref } from 'vue'
import { db } from '@/utils/storage'

export const useOfflineStore = defineStore('offline', () => {
  const pendingCount = ref(0)
  const isSyncing = ref(false)

  async function refreshCount() {
    pendingCount.value = await db.pendingAnswers.where({ synced: 0 }).count()
  }

  return { pendingCount, isSyncing, refreshCount }
})
