<template>
  <!--
    备案信息（法规要求网站底部展示）。
    公安备案号下来后填下面的 GONGAN_BEIAN（自动显示）。
    公安号按规定旁边还要补一枚国徽图标 <img>。
  -->
  <footer class="app-footer">
    <div class="links">
      <a href="https://beian.miit.gov.cn" target="_blank" rel="noopener noreferrer">{{ ICP_BEIAN }}</a>
      <template v-if="GONGAN_BEIAN">
        <span class="sep">·</span>
        <a href="https://beian.mps.gov.cn" target="_blank" rel="noopener noreferrer">{{ GONGAN_BEIAN }}</a>
      </template>
    </div>
    <div class="copy">© 2026 VocabMaster</div>
  </footer>
</template>

<script setup lang="ts">
// 备案号常量，方便维护
const ICP_BEIAN = '京ICP备2026044898号-1'
// 公安联网备案号（beian.mps.gov.cn 通过后填，如 '京公网安备 11010102000001号'）；空则不显示
const GONGAN_BEIAN = ''
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
