<template>
  <view class="chart-wrap">
    <qiun-data-charts
      type="mix"
      :chartData="chartData"
      :opts="opts"
      :canvas2d="true"
      canvasId="ebbinghausChart"
    />
  </view>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

interface ReviewPoint {
  ts: string
  result: string
  stage_after: number
}

const props = defineProps<{
  reviews: ReviewPoint[]
  firstLearnedAt: string
}>()

const chartData = ref<Record<string, unknown>>({ categories: [], series: [] })
const opts = ref({
  color: ['#1890FF', '#67C23A', '#F56C6C'],
  padding: [15, 10, 0, 15],
  xAxis: { disableGrid: true, labelCount: 6 },
  yAxis: { gridType: 'dash', dashLength: 2, data: [{ min: 0, max: 1, toFixed: 1 }] },
  legend: { show: true, position: 'top' },
  extra: { line: { type: 'curve' } },
})

watch(() => props.reviews, rebuild, { immediate: true, deep: true })

function rebuild() {
  const start = new Date(props.firstLearnedAt).getTime()
  const toHours = (ts: string) => (new Date(ts).getTime() - start) / 3_600_000

  // 生成理论遗忘曲线（每段 10 个插值点）
  let stability = 24 // 初始记忆稳定性（小时）
  let prevT = 0
  const theoreticalX: string[] = []
  const theoreticalY: number[] = []

  for (const r of props.reviews) {
    const t = toHours(r.ts)
    const steps = 10
    for (let i = 0; i <= steps; i++) {
      const ti = prevT + ((t - prevT) * i) / steps
      theoreticalX.push(`${ti.toFixed(1)}h`)
      theoreticalY.push(parseFloat(Math.exp(-(ti - prevT) / stability).toFixed(3)))
    }
    stability *= r.result === 'correct' ? 2 : 0.5
    prevT = t
  }

  // 实际复习点（散点）
  const actualY = props.reviews.map(r => (r.result === 'correct' ? 1 : 0.3))

  chartData.value = {
    categories: theoreticalX,
    series: [
      { name: '理论曲线', type: 'line', data: theoreticalY, smooth: true },
      {
        name: '复习点',
        type: 'point',
        data: props.reviews.map((r, i) => ({
          value: actualY[i],
          // 在理论曲线 x 轴上找对应位置
          index: Math.min(i * 11, theoreticalX.length - 1),
        })),
      },
    ],
  }
}
</script>

<style lang="scss" scoped>
.chart-wrap {
  width: 100%;
  height: 500rpx;
}
</style>
