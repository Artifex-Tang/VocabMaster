export const storage = {
  get<T>(key: string): T | null {
    try {
      const v = uni.getStorageSync(key)
      if (v === '' || v === null || v === undefined) return null
      return typeof v === 'string' ? JSON.parse(v) : (v as T)
    } catch {
      return null
    }
  },

  set(key: string, value: unknown): void {
    const raw = typeof value === 'string' ? value : JSON.stringify(value)
    uni.setStorageSync(key, raw)
  },

  remove(key: string): void {
    uni.removeStorageSync(key)
  },

  clear(): void {
    uni.clearStorageSync()
  },
}

// 轻量级本地"表"，用于离线队列等场景
export class LocalTable<T extends { id?: number | string }> {
  constructor(private readonly name: string) {}

  all(): T[] {
    return storage.get<T[]>(this.name) ?? []
  }

  add(item: T): T {
    if (!item.id) {
      ;(item as Record<string, unknown>).id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
    }
    const list = this.all()
    list.push(item)
    storage.set(this.name, list)
    return item
  }

  remove(id: T['id']): void {
    storage.set(this.name, this.all().filter(x => x.id !== id))
  }

  update(id: T['id'], patch: Partial<T>): void {
    storage.set(this.name, this.all().map(x => (x.id === id ? { ...x, ...patch } : x)))
  }

  filter(pred: (x: T) => boolean): T[] {
    return this.all().filter(pred)
  }

  count(): number {
    return this.all().length
  }

  clear(): void {
    storage.remove(this.name)
  }
}

export interface PendingAnswer {
  id?: string
  word_id: number
  level_code: string
  result: 'correct' | 'wrong' | 'skip'
  mode: 'card' | 'spelling' | 'choice' | 'listening'
  duration_ms: number
  client_ts: string
  synced?: 0 | 1
}

export const pendingAnswersTable = new LocalTable<PendingAnswer>('pending_answers')
