import type { MockMethod } from 'vite-plugin-mock'

export default [
  {
    url: '/api/v1/stats/today',
    method: 'get',
    response: () => ({
      code: 0, msg: 'ok',
      data: {
        date: new Date().toISOString().slice(0, 10),
        words_learned: 8,
        words_reviewed: 12,
        correct_count: 17,
        accuracy: 0.85,
        duration_seconds: 480,
        goal_progress: { new: '8/20', review: '12/100' },
      },
    }),
  },
  {
    url: '/api/v1/stats/summary',
    method: 'get',
    response: () => ({
      code: 0, msg: 'ok',
      data: {
        period: 'week',
        start_date: '2026-04-13',
        end_date: '2026-04-19',
        days_active: 5,
        total_learned: 40,
        total_reviewed: 120,
        avg_accuracy: 0.82,
        daily_breakdown: Array.from({ length: 7 }, (_, i) => ({
          date: new Date(Date.now() - (6 - i) * 86400000).toISOString().slice(0, 10),
          learned: Math.floor(Math.random() * 20),
          reviewed: Math.floor(Math.random() * 40),
          accuracy: 0.7 + Math.random() * 0.25,
        })),
        level_breakdown: [{ level_code: 'CET4', mastered: 45, learning: 30 }],
        topic_heatmap: [],
      },
    }),
  },
  {
    url: '/api/v1/stats/forgetting-curve',
    method: 'get',
    response: ({ query }: { query: Record<string, string> }) => ({
      code: 0, msg: 'ok',
      data: {
        word_id: Number(query.word_id ?? 1),
        word: 'abandon',
        first_learned_at: new Date(Date.now() - 30 * 3600000).toISOString(),
        reviews: [
          { ts: new Date(Date.now() - 30 * 3600000).toISOString(), result: 'correct', stage_after: 1 },
          { ts: new Date(Date.now() - 29.5 * 3600000).toISOString(), result: 'correct', stage_after: 2 },
          { ts: new Date(Date.now() - 29 * 3600000).toISOString(), result: 'wrong',   stage_after: 1 },
          { ts: new Date(Date.now() - 17 * 3600000).toISOString(), result: 'correct', stage_after: 2 },
          { ts: new Date(Date.now() - 5 * 3600000).toISOString(),  result: 'correct', stage_after: 3 },
        ],
        theoretical_curve: {
          type: 'ebbinghaus',
          stages: [0.083, 0.5, 12, 24, 48, 96, 168, 360, 720],
        },
      },
    }),
  },
  {
    url: '/api/v1/stats/level-overview',
    method: 'get',
    response: () => ({
      code: 0, msg: 'ok',
      data: {
        level_code: 'CET4',
        total_words: 4523,
        not_started: 4400,
        learning: 100,
        mastered: 23,
        mastery_rate: 0.005,
        stage_distribution: Array.from({ length: 9 }, (_, i) => ({ stage: i + 1, count: Math.floor(Math.random() * 30) })),
      },
    }),
  },
] as MockMethod[]
