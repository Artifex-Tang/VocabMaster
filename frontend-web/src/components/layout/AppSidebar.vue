<template>
  <div class="sidebar-wrap">
    <div class="logo">
      <Icon icon="mdi:book-open-variant" width="28" color="#1890ff" />
      <span class="logo-text">VocabMaster</span>
    </div>

    <el-menu :default-active="route.path" router class="nav-menu">
      <el-menu-item index="/dashboard">
        <Icon icon="mdi:view-dashboard-outline" width="20" />
        <span>今日学习</span>
      </el-menu-item>
      <el-menu-item index="/levels">
        <Icon icon="mdi:layers-outline" width="20" />
        <span>等级选择</span>
      </el-menu-item>
      <el-menu-item index="/test">
        <Icon icon="mdi:pencil-box-outline" width="20" />
        <span>测试练习</span>
      </el-menu-item>
      <el-menu-item index="/wrong-words">
        <Icon icon="mdi:bookmark-minus-outline" width="20" />
        <span>错词本</span>
      </el-menu-item>
      <el-menu-item index="/stats">
        <Icon icon="mdi:chart-line" width="20" />
        <span>学习统计</span>
      </el-menu-item>
      <el-menu-item index="/settings">
        <Icon icon="mdi:cog-outline" width="20" />
        <span>设置</span>
      </el-menu-item>
      <el-menu-item v-if="userStore.isAdmin" index="/admin">
        <Icon icon="mdi:shield-account-outline" width="20" />
        <span>管理后台</span>
      </el-menu-item>
    </el-menu>

    <div class="offline-badge" v-if="offlineStore.pendingCount > 0">
      <Icon icon="mdi:cloud-upload-outline" width="16" />
      <span>{{ offlineStore.pendingCount }} 条待同步</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { Icon } from '@iconify/vue'
import { useUserStore } from '@/stores/user'
import { useOfflineStore } from '@/stores/offline'

const route = useRoute()
const userStore = useUserStore()
const offlineStore = useOfflineStore()
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.sidebar-wrap {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: $space-4 0;
}

.logo {
  display: flex;
  align-items: center;
  gap: $space-2;
  padding: 0 $space-4 $space-4;
  border-bottom: 1px solid #f0f0f0;
}

.logo-text {
  font-family: $font-en;
  font-weight: 700;
  font-size: 1.1rem;
  color: #1f2937;
}

.nav-menu {
  border-right: none;
  flex: 1;

  .el-menu-item {
    display: flex;
    align-items: center;
    gap: $space-2;
  }
}

.offline-badge {
  display: flex;
  align-items: center;
  gap: $space-1;
  padding: $space-2 $space-4;
  font-size: 0.75rem;
  color: #f59e0b;
  background: #fffbeb;
  border-top: 1px solid #fef3c7;
  margin-top: auto;
}
</style>
