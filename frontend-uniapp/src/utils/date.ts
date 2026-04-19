// 格式化日期为 YYYY-MM-DD（使用用户本地时区）
export function formatDate(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

export function formatTime(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

// 距现在的可读时间（"5分钟前"、"昨天"等）
export function fromNow(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date
  const diff = Date.now() - d.getTime()
  const min = Math.floor(diff / 60000)
  if (min < 1) return '刚刚'
  if (min < 60) return `${min}分钟前`
  const h = Math.floor(min / 60)
  if (h < 24) return `${h}小时前`
  const days = Math.floor(h / 24)
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return formatDate(d)
}

// 下次复习的距离描述（"5分钟后"、"明天"等）
export function untilReview(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date
  const diff = d.getTime() - Date.now()
  if (diff <= 0) return '立即复习'
  const min = Math.floor(diff / 60000)
  if (min < 60) return `${min}分钟后`
  const h = Math.floor(min / 60)
  if (h < 24) return `${h}小时后`
  return `${Math.floor(h / 24)}天后`
}
