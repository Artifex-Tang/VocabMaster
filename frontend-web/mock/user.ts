import type { MockMethod } from 'vite-plugin-mock'

export default [
  {
    url: '/api/v1/user/settings',
    method: 'get',
    response: () => ({
      code: 0, msg: 'ok',
      data: {
        daily_new_words_goal: 20,
        daily_review_goal: 100,
        default_sort_mode: 'alpha',
        preferred_accent: 'uk',
        auto_play_audio: true,
        notification_time: '20:00:00',
        theme: 'light',
        active_levels: ['CET4'],
      },
    }),
  },
  {
    url: '/api/v1/user/settings',
    method: 'patch',
    response: ({ body }: { body: Record<string, unknown> }) => ({
      code: 0, msg: 'ok', data: body,
    }),
  },
  {
    url: '/api/v1/user/me',
    method: 'patch',
    response: ({ body }: { body: Record<string, unknown> }) => ({
      code: 0, msg: 'ok', data: body,
    }),
  },
  {
    url: '/api/v1/sync/push',
    method: 'post',
    response: ({ body }: { body: { answers?: unknown[] } }) => ({
      code: 0, msg: 'ok',
      data: {
        results: (body.answers ?? []).map((_: unknown) => ({ status: 'accepted' })),
      },
    }),
  },
  {
    url: '/api/v1/wrong-words',
    method: 'get',
    response: () => ({
      code: 0, msg: 'ok',
      data: { items: [], total: 0, page: 1, page_size: 20 },
    }),
  },
] as MockMethod[]
