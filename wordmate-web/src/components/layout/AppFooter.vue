<template>
  <!--
    备案信息（法规要求网站底部展示）。
    拿到真实备案号后替换下面的占位：
      ICP 备案号  → 工信部发，ICP 备案通过后有
      公安备案号  → beian.mps.gov.cn 公安联网备案通过后有（含 32 位数字）
    公安号按规定还需旁边放一枚国徽图标，号下来补 <img>。
  -->
  <footer class="app-footer">
    <div class="links">
      <a href="https://beian.miit.gov.cn" target="_blank" rel="noopener noreferrer">沪ICP备XXXXXXXX号-X</a>
      <span class="sep">·</span>
      <a href="https://beian.mps.gov.cn" target="_blank" rel="noopener noreferrer">沪公网安备 XXXXXXXXXXXX号</a>
    </div>
    <div class="copy">© 2026 VocabMaster</div>
  </footer>
</template>

<script setup lang="ts">
// 纯展示组件，无逻辑
</script>

<style scoped>
.app-footer {
  flex-shrink: 0;
  padding: 8px 16px;
  text-align: center;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
  background: var(--el-bg-color);
  border-top: 1px solid var(--el-border-color-lighter);
}
.links a {
  color: var(--el-text-color-secondary);
  text-decoration: none;
}
.links a:hover {
  color: var(--el-color-primary);
}
.sep {
  margin: 0 6px;
  opacity: 0.5;
}
.copy {
  opacity: 0.7;
}
</style>
