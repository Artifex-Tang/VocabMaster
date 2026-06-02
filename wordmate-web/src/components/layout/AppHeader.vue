<template>
  <div class="app-header">
    <div class="left">
      <el-breadcrumb v-if="breadcrumbs.length">
        <el-breadcrumb-item v-for="b in breadcrumbs" :key="b.path" :to="b.path">
          {{ b.label }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="right">
      <el-tooltip v-if="offlineStore.pendingCount > 0" content="点击同步离线数据">
        <el-button
          link
          :loading="offlineStore.isSyncing"
          @click="handleSync"
        >
          <Icon icon="mdi:cloud-sync-outline" width="20" color="#f59e0b" />
        </el-button>
      </el-tooltip>

      <el-dropdown trigger="click" @command="handleCmd">
        <div class="avatar-wrap">
          <el-avatar :size="32" :src="userStore.user?.avatar_url">
            {{ userStore.user?.nickname?.[0] ?? 'U' }}
          </el-avatar>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="settings">设置</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useOfflineStore } from '@/stores/offline'
import { useOfflineSync } from '@/composables/useOfflineSync'
import * as authApi from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const offlineStore = useOfflineStore()
const { manualFlush } = useOfflineSync()

const LABELS: Record<string, string> = {
  dashboard: '今日学习',
  study: '学习会话',
  test: '测试练习',
  stats: '学习统计',
  settings: '设置',
  levels: '等级选择',
  'wrong-words': '错词本',
  admin: '管理后台',
}

const breadcrumbs = computed(() => {
  const segs = route.path.split('/').filter(Boolean)
  return segs.map((s, i) => ({
    label: LABELS[s] ?? s,
    path: '/' + segs.slice(0, i + 1).join('/'),
  }))
})

async function handleSync() {
  await manualFlush()
  ElMessage.success('同步完成')
}

async function handleCmd(cmd: string) {
  if (cmd === 'logout') {
    await authApi.logout().catch(() => {})
    userStore.logout()
    router.push('/login')
  } else if (cmd === 'settings') {
    router.push('/settings')
  }
}
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0 24px;
}
.right { display: flex; align-items: center; gap: 12px; }
.avatar-wrap { cursor: pointer; }
</style>
