<template>
  <el-container class="app-layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="sidebar">
      <AppSidebar :collapsed="collapsed" @toggle="collapsed = !collapsed" />
    </el-aside>
    <el-container direction="vertical">
      <el-header height="56px" class="header">
        <AppHeader />
      </el-header>
      <el-main class="main-content">
        <RouterView />
      </el-main>
      <el-footer height="auto" class="footer-wrap">
        <AppFooter />
      </el-footer>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'
import AppFooter from './AppFooter.vue'

const collapsed = ref(false)
</script>

<style scoped>
.app-layout { height: 100vh; overflow: hidden; }
.sidebar {
  border-right: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
  overflow-y: auto;
  transition: width 0.3s;
}
.header { border-bottom: 1px solid var(--el-border-color); background: var(--el-bg-color); display: flex; align-items: center; }
.main-content { overflow-y: auto; background: var(--el-bg-color-page); }
.footer-wrap { padding: 0; }
</style>
