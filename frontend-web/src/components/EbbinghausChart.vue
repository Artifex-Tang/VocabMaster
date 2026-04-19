<template>
  <div ref="chartRef" class="ebbinghaus-chart" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import { dayjs } from '@/utils/date'
import type { ForgettingCurveData } from '@/api/types'

const props = defineProps<{
  data: ForgettingCurveData
}>()

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

function buildOptions() {
  const { reviews, first_learned_at } = props.data as ForgettingCurveData & { first_learned_at?: string }
  const start = dayjs(first_learned_at ?? reviews[0]?.ts ?? new Date())
  const now = dayjs()
  const totalH = now.diff(start, 'hour', true)

  // Build theoretical decay segments between reviews
  const theoreticalPts: [number, number][] = []
  let S = 24
  let lastT = 0

  for (const r of reviews) {
    const t = dayjs(r.ts).diff(start, 'hour', true)
    const steps = Math.max(10, Math.floor((t - lastT) * 2))
    for (let i = 0; i <= steps; i++) {
      const ti = lastT + (t - lastT) * (i / steps)
      theoreticalPts.push([ti, Math.exp(-(ti - lastT) / S)])
    }
    S = S * (r.result === 'correct' ? 2 : 0.5)
    lastT = t
  }
  for (let t = lastT; t <= totalH; t += 0.25) {
    theoreticalPts.push([t, Math.exp(-(t - lastT) / S)])
  }

  // Actual review scatter
  const scatterPts = reviews.map(r => ({
    value: [dayjs(r.ts).diff(start, 'hour', true), r.result === 'correct' ? 1 : 0.25],
    itemStyle: { color: r.result === 'correct' ? '#10b981' : '#ef4444' },
  }))

  return {
    grid: { top: 36, left: 56, right: 16, bottom: 44 },
    xAxis: {
      type: 'value',
      name: '时间',
      axisLabel: {
        formatter: (v: number) => (v < 24 ? `${v.toFixed(0)}h` : `${(v / 24).toFixed(0)}d`),
      },
    },
    yAxis: {
      type: 'value',
      name: '记忆保留率',
      min: 0,
      max: 1,
      axisLabel: { formatter: (v: number) => `${Math.round(v * 100)}%` },
    },
    tooltip: { trigger: 'axis' },
    legend: { top: 4 },
    series: [
      {
        name: '理论曲线',
        type: 'line',
        data: theoreticalPts,
        smooth: true,
        showSymbol: false,
        lineStyle: { color: '#1890ff', width: 2 },
        areaStyle: { color: 'rgba(24,144,255,0.06)' },
      },
      {
        name: '复习节点',
        type: 'scatter',
        data: scatterPts,
        symbolSize: 10,
      },
    ],
  }
}

function draw() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption(buildOptions())
}

onMounted(draw)
watch(() => props.data, draw, { deep: true })

const resizeObserver = new ResizeObserver(() => chart?.resize())

onMounted(() => {
  if (chartRef.value) resizeObserver.observe(chartRef.value)
})
onUnmounted(() => {
  resizeObserver.disconnect()
  chart?.dispose()
})
</script>

<style scoped>
.ebbinghaus-chart {
  width: 100%;
  height: 320px;
}
</style>
