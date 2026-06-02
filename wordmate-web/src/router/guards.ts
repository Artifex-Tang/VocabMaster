import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'

export function setupRouterGuards(router: Router) {
  router.beforeEach(async to => {
    const userStore = useUserStore()

    if (to.meta.public) return true

    if (!userStore.isLoggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    if (!userStore.user) {
      try {
        await userStore.fetchMe()
      } catch {
        return '/login'
      }
    }

    if (to.meta.adminOnly && !userStore.isAdmin) {
      return '/403'
    }

    return true
  })
}
