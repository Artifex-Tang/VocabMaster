<template>
  <div class="stats-page">
    <div class="page-title">学习统计</div>

    <el-row :gutter="16" class="today-row">
      <el-col :span="6" v-for="item in todayItems" :key="item.label">
        <div class="stat-card card-shadow">
          <div class="stat-val">{{ item.value }}</div>
          <div class="stat-lbl">{{ item.label }}</div>
        </div>
      </el-col>
    </el-row>

    <el-card style="margin-top: 24px">
      <template #header>近 30 天每日学习量</template>
      <div ref="barRef" style="height: 240px" />
    </el-card>

    <el-button style="margin-top: 16px" @click="router.push('/stats/forgetting-curve')">
      查看遗忘曲线
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import * as statsApi from '@/api/stats'
import type { TodayStats } from '@/api/types'

const router = useRouter()
const barRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const todayData = ref<TodayStats | null>(null)

const todayItems = computed(() => {
  const d = todayData.value
  if (!d) return []
  return [
    { label: '新学单词', value: d.words_learned },
    { label: '复习单词', value: d.words_reviewed },
    { label: '正确率', value: `${Math.round(d.accuracy * 100)}%` },
    { label: '学习时长', value: `${Math.round(d.duration_seconds / 60)}min` },
  ]
})

onMounted(async () => {
  todayData.value = await statsApi.todayStats().catch(() => null)

  if (barRef.value) {
    chart = echarts.init(barRef.value)
    chart.setOption({
      grid: { top: 16, left: 40, right: 8, bottom: 36 },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: [], itemStyle: { color: '#1890ff' } }],
      tooltip: { trigger: 'axis' },
    })
  }
})

onUnmounted(() => chart?.dispose())
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.stats-page { padding: $space-6; }
.page-title { font-size: 1.5rem; font-weight: 700; margin-bottom: $space-6; }

.today-row { margin-bottom: $space-4; }
.stat-card {
  background: #fff;
  padding: $space-4;
  text-align: center;
}
.stat-val { font-size: 1.8rem; font-weight: 700; color: $color-primary; font-family: $font-en; }
.stat-lbl { font-size: 0.8rem; color: #6b7280; margin-top: 4px; }
</style>
