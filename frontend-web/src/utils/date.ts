import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(utc)
dayjs.extend(timezone)
dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

export { dayjs }

export function toUserTz(isoStr: string, tz = 'Asia/Shanghai'): dayjs.Dayjs {
  return dayjs(isoStr).tz(tz)
}

export function nowIso(): string {
  return dayjs().toISOString()
}

export function formatDate(isoStr: string, fmt = 'YYYY-MM-DD'): string {
  return dayjs(isoStr).format(fmt)
}
