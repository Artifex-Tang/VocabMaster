import { useOnline } from '@vueuse/core'
import { watch } from 'vue'
import { db } from '@/utils/storage'
import * as studyApi from '@/api/study'
import * as syncApi from '@/api/sync'
import { getDeviceId } from '@/utils/device-id'
import { useOfflineStore } from '@/stores/offline'
import { nowIso } from '@/utils/date'
import type { AnswerPayload } from '@/api/types'

// Called once in main.ts — sets up the global online watcher
export function initOfflineSync() {
  const online = useOnline()
  watch(online, isOnline => {
    if (isOnline) flushPendingQueue()
  })
}

export function useOfflineSync() {
  const online = useOnline()
  const offlineStore = useOfflineStore()

  async function submitAnswer(payload: AnswerPayload) {
    if (online.value) {
      try {
        return await studyApi.answer(payload)
      } catch {
        await enqueue(payload)
        throw new Error('network_error')
      }
    } else {
      await enqueue(payload)
      return { queued: true }
    }
  }

  async function enqueue(payload: AnswerPayload) {
    await db.pendingAnswers.add({ ...payload, synced: 0 })
    await offlineStore.refreshCount()
  }

  async function manualFlush() {
    await flushPendingQueue()
    await offlineStore.refreshCount()
  }

  return { submitAnswer, manualFlush, online }
}

async function flushPendingQueue() {
  const offlineStore = useOfflineStore()
  const pending = await db.pendingAnswers.where({ synced: 0 }).toArray()
  if (!pending.length) return

  offlineStore.isSyncing = true
  try {
    await syncApi.push({
      device_id: getDeviceId(),
      answers: pending.map(p => ({
        word_id: p.word_id,
        level_code: p.level_code,
        result: p.result,
        mode: p.mode,
        duration_ms: p.duration_ms,
        client_ts: p.client_ts || nowIso(),
      })),
    })
    const ids = pending.map(p => p.id!).filter(Boolean)
    await db.pendingAnswers.where('id').anyOf(ids).modify({ synced: 1 })
    await db.pendingAnswers.where({ synced: 1 }).delete()
    await offlineStore.refreshCount()
  } catch (e) {
    console.error('[offline-sync] flush failed', e)
  } finally {
    offlineStore.isSyncing = false
  }
}
