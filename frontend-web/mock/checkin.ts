import type { MockMethod } from 'vite-plugin-mock'

export default [
  {
    url: '/api/v1/checkin/today',
    method: 'post',
    response: () => ({
      code: 0, msg: 'ok',
      data: {
        date: new Date().toISOString().slice(0, 10),
        current_streak: 7,
        longest_streak: 30,
        total_days: 42,
        new_achievements: [],
      },
    }),
  },
  {
    url: '/api/v1/checkin/calendar',
    method: 'get',
    response: ({ query }: { query: Record<string, string> }) => {
      const [year, month] = (query.month ?? '2026-04').split('-').map(Number)
      const daysInMonth = new Date(year, month, 0).getDate()
      return {
        code: 0, msg: 'ok',
        data: {
          month: query.month ?? '2026-04',
          days: Array.from({ length: daysInMonth }, (_, i) => ({
            date: `${query.month}-${String(i + 1).padStart(2, '0')}`,
            checked_in: Math.random() > 0.3,
            words_count: Math.floor(Math.random() * 30),
          })),
          current_streak: 7,
          longest_streak: 30,
        },
      }
    },
  },
  {
    url: '/api/v1/checkin/achievements',
    method: 'get',
    response: () => ({
      code: 0, msg: 'ok',
      data: {
        unlocked: [
          { code: 'FIRST_WORD', name_zh: '第一个单词', icon: '🌱', achieved_at: '2026-03-01T00:00:00+08:00' },
          { code: 'STREAK_7',   name_zh: '坚持 7 天',  icon: '🔥', achieved_at: '2026-03-08T00:00:00+08:00' },
        ],
        locked: [
          { code: 'STREAK_30', name_zh: '坚持 30 天', progress: '7/30' },
          { code: 'WORDS_100', name_zh: '掌握 100 词', progress: '23/100' },
        ],
      },
    }),
  },
] as MockMethod[]
