<template>
  <div class="done-page">
    <div class="done-card card-shadow">
      <Icon icon="mdi:trophy-variant" width="64" color="#f59e0b" />
      <h2>本次学习完成！</h2>

      <div class="stats-row">
        <div class="stat-item">
          <div class="val">{{ total }}</div>
          <div class="lbl">共学习</div>
        </div>
        <div class="stat-item">
          <div class="val text-mastered">{{ correct }}</div>
          <div class="lbl">答对</div>
        </div>
        <div class="stat-item">
          <div class="val">{{ accuracyPct }}%</div>
          <div class="lbl">正确率</div>
        </div>
      </div>

      <el-progress
        type="circle"
        :percentage="accuracyPct"
        :color="progressColor"
        :width="100"
        style="margin: 16px 0"
      />

      <div class="actions">
        <el-button type="primary" size="large" @click="router.push('/dashboard')">
          返回首页
        </el-button>
        <el-button size="large" @click="router.push('/stats')">
          查看统计
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Icon } from '@iconify/vue'

const router = useRouter()
const route = useRoute()

const correct = Number(route.query.correct ?? 0)
const total = Number(route.query.total ?? 0)
const accuracyPct = computed(() => (total ? Math.round((correct / total) * 100) : 0))
const progressColor = computed(() =>
  accuracyPct.value >= 80 ? '#10b981' : accuracyPct.value >= 60 ? '#f59e0b' : '#ef4444',
)
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.done-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
}

.done-card {
  background: #fff;
  padding: $space-8 $space-12;
  text-align: center;
  h2 { margin: $space-3 0 $space-6; }
}

.stats-row {
  display: flex;
  gap: $space-8;
  justify-content: center;
  margin-bottom: $space-4;
}

.stat-item .val {
  font-size: 2rem;
  font-weight: 700;
  font-family: $font-en;
}

.stat-item .lbl { font-size: 0.8rem; color: #6b7280; }

.actions { display: flex; gap: $space-3; justify-content: center; margin-top: $space-6; }
</style>
