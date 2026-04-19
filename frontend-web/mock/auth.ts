import type { MockMethod } from 'vite-plugin-mock'

const mockUser = {
  uuid: 'u-001',
  nickname: '测试用户',
  email: 'test@example.com',
  phone_masked: '138****0000',
  avatar_url: '',
  locale: 'zh-CN',
  timezone: 'Asia/Shanghai',
  bound_providers: [],
  created_at: '2026-01-01T00:00:00+08:00',
}

const mockTokens = {
  access_token: 'mock-access-token-abc123',
  refresh_token: 'mock-refresh-token-xyz789',
  expires_in: 7200,
}

export default [
  {
    url: '/api/v1/auth/login',
    method: 'post',
    response: ({ body }: { body: Record<string, string> }) => {
      if (body.password && body.password.length >= 8) {
        return { code: 0, msg: 'ok', data: { ...mockTokens, user: mockUser } }
      }
      return { code: 30004, msg: '密码错误', data: null }
    },
  },
  {
    url: '/api/v1/auth/register',
    method: 'post',
    response: () => ({
      code: 0, msg: 'ok', data: { ...mockTokens, user: mockUser },
    }),
  },
  {
    url: '/api/v1/auth/login-by-code',
    method: 'post',
    response: () => ({
      code: 0, msg: 'ok', data: { ...mockTokens, user: mockUser },
    }),
  },
  {
    url: '/api/v1/auth/send-code',
    method: 'post',
    response: () => ({ code: 0, msg: 'ok', data: { expires_in: 300 } }),
  },
  {
    url: '/api/v1/auth/refresh',
    method: 'post',
    response: () => ({
      code: 0, msg: 'ok', data: { access_token: 'mock-new-access-token', expires_in: 7200 },
    }),
  },
  {
    url: '/api/v1/auth/logout',
    method: 'post',
    response: () => ({ code: 0, msg: 'ok', data: null }),
  },
  {
    url: '/api/v1/auth/reset-password',
    method: 'post',
    response: () => ({ code: 0, msg: 'ok', data: null }),
  },
  {
    url: '/api/v1/user/me',
    method: 'get',
    response: () => ({ code: 0, msg: 'ok', data: mockUser }),
  },
] as MockMethod[]
