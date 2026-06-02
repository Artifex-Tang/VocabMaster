import Dexie, { type Table } from 'dexie'

export interface PendingAnswer {
  id?: number
  word_id: number
  level_code: string
  result: 'correct' | 'wrong' | 'skip'
  mode: 'card' | 'spelling' | 'choice' | 'listening'
  duration_ms: number
  client_ts: string
  synced: 0 | 1
}

export interface CachedWord {
  id: number
  level_code: string
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  data: any
  cached_at: string
  version: string
}

class VocabDB extends Dexie {
  pendingAnswers!: Table<PendingAnswer>
  cachedWords!: Table<CachedWord>

  constructor() {
    super('VocabMaster')
    this.version(1).stores({
      pendingAnswers: '++id, synced, client_ts',
      cachedWords: 'id, level_code, version',
    })
  }
}

export const db = new VocabDB()
