import { useUserStore } from '@/stores/user'

/**
 * 受限功能统一登录引导。未登录弹 modal，确认跳登录页。
 * @returns true=已登录可继续；false=已拦截（调用方应 return）
 */
export function requireLogin(): boolean {
  if (useUserStore().isLoggedIn) return true
  uni.showModal({
    title: '需要登录',
    content: '登录后即可使用该功能',
    confirmText: '去登录',
    success: (r) => {
      if (r.confirm) uni.reLaunch({ url: '/pages/auth/login' })
    },
  })
  return false
}
