import { describe, it, expect, beforeEach } from 'vitest'
import { formatDate, formatTime, fromNow, untilReview, nowIso } from '@/utils/date'
import { storage, LocalTable } from '@/utils/storage'

describe('date utils', () => {
  describe('nowIso', () => {
    it('should return ISO string without milliseconds and Z', () => {
      const result = nowIso()
      // 格式: 2026-06-02T12:34:56
      expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/)
      expect(result).not.toContain('.')
      expect(result).not.toContain('Z')
    })
  })

  describe('formatDate', () => {
    it('should format Date to YYYY-MM-DD', () => {
      expect(formatDate(new Date('2026-06-02T12:00:00Z'))).toBe('2026-06-02')
    })

    it('should format string to YYYY-MM-DD', () => {
      expect(formatDate('2026-01-15T00:00:00Z')).toBe('2026-01-15')
    })
  })

  describe('formatTime', () => {
    it('should format Date to HH:MM', () => {
      expect(formatTime(new Date('2026-06-02T14:30:00'))).toBe('14:30')
    })
  })

  describe('fromNow', () => {
    it('should return 刚刚 for < 1 min', () => {
      expect(fromNow(new Date())).toBe('刚刚')
    })

    it('should return X分钟前', () => {
      const d = new Date(Date.now() - 5 * 60000)
      expect(fromNow(d)).toBe('5分钟前')
    })

    it('should return X小时前', () => {
      const d = new Date(Date.now() - 3 * 3600000)
      expect(fromNow(d)).toBe('3小时前')
    })
  })

  describe('untilReview', () => {
    it('should return 立即复习 for past time', () => {
      expect(untilReview(new Date(Date.now() - 10000))).toBe('立即复习')
    })

    it('should return X分钟后', () => {
      const d = new Date(Date.now() + 10 * 60000)
      expect(untilReview(d)).toBe('10分钟后')
    })

    it('should return X小时后', () => {
      const d = new Date(Date.now() + 3 * 3600000)
      expect(untilReview(d)).toBe('3小时后')
    })

    it('should return X天后', () => {
      const d = new Date(Date.now() + 3 * 86400000)
      expect(untilReview(d)).toBe('3天后')
    })
  })
})

describe('storage', () => {
  beforeEach(() => {
    storage.clear()
  })

  it('should set and get string', () => {
    // storage stores strings as-is, but get tries JSON.parse
    // so use JSON.stringify-able value
    storage.set('key', '"value"')
    expect(storage.get<string>('key')).toBe('value')
  })

  it('should set and get object', () => {
    storage.set('obj', { name: 'test', count: 42 })
    expect(storage.get<{ name: string; count: number }>('obj')).toEqual({ name: 'test', count: 42 })
  })

  it('should return null for missing key', () => {
    expect(storage.get('nonexistent')).toBeNull()
  })

  it('should remove key', () => {
    storage.set('key', 'value')
    storage.remove('key')
    expect(storage.get('key')).toBeNull()
  })

  it('should clear all', () => {
    storage.set('a', '1')
    storage.set('b', '2')
    storage.clear()
    expect(storage.get('a')).toBeNull()
    expect(storage.get('b')).toBeNull()
  })
})

describe('LocalTable', () => {
  interface TestItem { id?: string; name: string; done?: boolean }
  let table: LocalTable<TestItem>

  beforeEach(() => {
    storage.clear()
    table = new LocalTable<TestItem>('test_table')
  })

  it('should add item with auto id', () => {
    const item = table.add({ name: 'hello' })
    expect(item.id).toBeTruthy()
    expect(table.all()).toHaveLength(1)
  })

  it('should add item with custom id', () => {
    table.add({ id: 'custom', name: 'hello' })
    expect(table.all()[0].id).toBe('custom')
  })

  it('should remove by id', () => {
    table.add({ id: '1', name: 'a' })
    table.add({ id: '2', name: 'b' })
    table.remove('1')
    expect(table.all()).toHaveLength(1)
    expect(table.all()[0].id).toBe('2')
  })

  it('should update by id', () => {
    table.add({ id: '1', name: 'a', done: false })
    table.update('1', { done: true })
    expect(table.all()[0].done).toBe(true)
    expect(table.all()[0].name).toBe('a')
  })

  it('should filter items', () => {
    table.add({ name: 'a', done: true })
    table.add({ name: 'b', done: false })
    table.add({ name: 'c', done: true })
    expect(table.filter(x => x.done === true)).toHaveLength(2)
  })

  it('should count items', () => {
    table.add({ name: 'a' })
    table.add({ name: 'b' })
    expect(table.count()).toBe(2)
  })

  it('should clear table', () => {
    table.add({ name: 'a' })
    table.clear()
    expect(table.all()).toHaveLength(0)
  })
})
