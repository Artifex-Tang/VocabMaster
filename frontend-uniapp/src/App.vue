<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useOfflineSync } from '@/composables/useOfflineSync'

onLaunch(() => {
  const userStore = useUserStore()
  userStore.initFromStorage()
})

onShow(() => {
  const { flushQueue } = useOfflineSync()
  flushQueue()
})
</script>

<template>
  <layout />
</template>
