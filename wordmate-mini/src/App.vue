<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { usePrivacyStore } from '@/stores/privacy'
import { useOfflineSync } from '@/composables/useOfflineSync'

onLaunch(() => {
  const userStore = useUserStore()
  userStore.initFromStorage()

  // #ifdef MP-WEIXIN
  // 隐私接口（如 wx.login）调用前，微信 runtime 触发授权请求
  // 弹出隐私保护指引同意弹窗，用户同意后放行
  const g = globalThis as unknown as {
    onNeedPrivacyAuthorization?: (cb: (e: { event: string }, resolve: (p: { event: 'agree' | 'disagree'; buttonId: string }) => void) => void) => void
  }
  g.onNeedPrivacyAuthorization?.((_e, resolve) => {
    usePrivacyStore().request(resolve)
  })
  // #endif
})

onShow(() => {
  const { flushQueue } = useOfflineSync()
  flushQueue()
})
</script>

<template>
  <layout />
</template>
