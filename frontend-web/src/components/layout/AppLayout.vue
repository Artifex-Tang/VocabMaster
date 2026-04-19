<template>
  <el-container class="app-layout">
    <el-aside width="220px" class="sidebar">
      <AppSidebar />
    </el-aside>
    <el-container direction="vertical">
      <el-header height="56px" class="header">
        <AppHeader />
      </el-header>
      <el-main class="main-content">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'
</script>

<style scoped>
.app-layout { height: 100vh; overflow: hidden; }
.sidebar { border-right: 1px solid #e5e7eb; background: #fff; overflow-y: auto; }
.header { border-bottom: 1px solid #e5e7eb; background: #fff; display: flex; align-items: center; }
.main-content { overflow-y: auto; background: #f3f4f6; }
</style>
