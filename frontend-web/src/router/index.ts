import { createRouter, createWebHistory } from 'vue-router'
import { setupRouterGuards } from './guards'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ── Public ──────────────────────────────────────────────────────────────
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/Login.vue'),
      meta: { public: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/auth/Register.vue'),
      meta: { public: true },
    },
    {
      path: '/reset-password',
      name: 'ResetPassword',
      component: () => import('@/views/auth/ResetPassword.vue'),
      meta: { public: true },
    },

    // ── App layout ───────────────────────────────────────────────────────────
    {
      path: '/',
      component: () => import('@/components/layout/AppLayout.vue'),
      children: [
        {
          path: '',
          redirect: '/dashboard',
        },
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/Dashboard.vue'),
        },
        {
          path: 'levels',
          name: 'LevelSelection',
          component: () => import('@/views/levels/LevelSelection.vue'),
        },
        {
          path: 'study/:level',
          name: 'StudySession',
          component: () => import('@/views/study/StudySession.vue'),
        },
        {
          path: 'study/:level/done',
          name: 'StudyDone',
          component: () => import('@/views/study/StudyDone.vue'),
        },
        {
          path: 'test',
          name: 'TestEntry',
          component: () => import('@/views/test/TestEntry.vue'),
        },
        {
          path: 'test/spelling',
          name: 'SpellingTest',
          component: () => import('@/views/test/SpellingTest.vue'),
        },
        {
          path: 'test/choice',
          name: 'ChoiceTest',
          component: () => import('@/views/test/ChoiceTest.vue'),
        },
        {
          path: 'test/listening',
          name: 'ListeningTest',
          component: () => import('@/views/test/ListeningTest.vue'),
        },
        {
          path: 'stats',
          name: 'StatsOverview',
          component: () => import('@/views/stats/StatsOverview.vue'),
        },
        {
          path: 'stats/forgetting-curve',
          name: 'ForgettingCurve',
          component: () => import('@/views/stats/ForgettingCurve.vue'),
        },
        {
          path: 'wrong-words',
          name: 'WrongWordList',
          component: () => import('@/views/wrong-word/WrongWordList.vue'),
        },
        {
          path: 'settings',
          name: 'UserSettings',
          component: () => import('@/views/settings/UserSettings.vue'),
        },
        // ── Admin ────────────────────────────────────────────────────────────
        {
          path: 'admin',
          meta: { adminOnly: true },
          children: [
            {
              path: '',
              name: 'AdminDashboard',
              component: () => import('@/views/admin/Dashboard.vue'),
            },
            {
              path: 'words',
              name: 'AdminWords',
              component: () => import('@/views/admin/WordManage.vue'),
            },
            {
              path: 'users',
              name: 'AdminUsers',
              component: () => import('@/views/admin/UserManage.vue'),
            },
          ],
        },
      ],
    },

    { path: '/403', component: () => import('@/views/errors/Forbidden.vue'), meta: { public: true } },
    { path: '/:pathMatch(.*)*', component: () => import('@/views/errors/NotFound.vue'), meta: { public: true } },
  ],
})

setupRouterGuards(router)

export default router
