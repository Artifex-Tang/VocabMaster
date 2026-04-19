import { ref } from 'vue'
import { pendingAnswersTable, type PendingAnswer } from '@/utils/storage'
import { getDeviceId } from '@/utils/device-id'
import * as studyApi from '@/api/study'
import * as syncApi from '@/api/sync'
import type { AnswerPayload } from '@/api/types'

const online = ref(true)
let syncListenerRegistered = false

function ensureListener() {
  if (syncListenerRegistered) return
  syncListenerRegistered = true

  uni.onNetworkStatusChange((res) => {
    online.value = res.isConnected
    if (res.isConnected) flushQueue()
  })

  uni.getNetworkType({
    success: (res) => {
      online.value = res.networkType !== 'none'
    },
  })
}

export async function flushQueue(): Promise<void> {
  const pending = pendingAnswersTable.filter(x => !x.synced)
  if (pending.length === 0) return

  try {
    const answers: AnswerPayload[] = pending.map(({ id: _id, synced: _s, ...rest }) => rest as AnswerPayload)
    await syncApi.push({ device_id: getDeviceId(), answers })
    pending.forEach(p => pendingAnswersTable.remove(p.id))
  } catch {
    // 留在队列，下次联网再试
  }
}

export function useOfflineSync() {
  ensureListener()

  async function submitAnswer(answer: AnswerPayload) {
    if (online.value) {
      try {
        return await studyApi.answer(answer)
      } catch {
        pendingAnswersTable.add({ ...answer, synced: 0 } as PendingAnswer)
        return { queued: true }
      }
    } else {
      pendingAnswersTable.add({ ...answer, synced: 0 } as PendingAnswer)
      return { queued: true }
    }
  }

  return { submitAnswer, online, flushQueue }
}
