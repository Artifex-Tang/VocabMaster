<template>
  <div class="dashboard">
    <div class="page-title">今日学习</div>

    <!-- Level selector -->
    <el-select
      v-model="selectedLevel"
      placeholder="选择等级"
      size="large"
      class="level-select"
      @change="loadPlan"
    >
      <el-option
        v-for="lv in LEVELS"
        :key="lv.code"
        :label="lv.nameZh"
        :value="lv.code"
      />
    </el-select>

    <!-- Loading -->
    <el-skeleton v-if="loading" :rows="4" animated />

    <!-- Plan overview -->
    <template v-else-if="plan">
      <div class="overview-cards">
        <div class="stat-card">
          <div class="stat-value">{{ plan.review_count }}</div>
          <div class="stat-label">待复习</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ plan.new_count }}</div>
          <div class="stat-label">新单词</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ plan.estimated_minutes }}</div>
          <div class="stat-label">预计分钟</div>
        </div>
      </div>

      <el-progress
        :percentage="todayProgress"
        :color="progressColor"
        style="margin-bottom: 24px"
      />

      <!-- Word preview lists -->
      <el-tabs>
        <el-tab-pane :label="`复习 (${plan.review_count})`" name="review">
          <WordListPreview :words="plan.review_words" />
        </el-tab-pane>
        <el-tab-pane :label="`新学 (${plan.new_count})`" name="new">
          <WordListPreview :words="plan.new_words" />
        </el-tab-pane>
      </el-tabs>

      <div class="start-actions">
        <el-button
          v-if="plan.review_count > 0 || plan.new_count > 0"
          type="primary"
          size="large"
          @click="startStudy"
        >
          <Icon icon="mdi:play" width="20" />
          开始学习（{{ plan.review_count + plan.new_count }} 词）
        </el-button>
        <el-empty v-else description="今日任务已完成！" />
      </div>
    </template>

    <!-- Today's checkin banner -->
    <el-card v-if="checkinData" class="checkin-banner">
      <Icon icon="mdi:fire" width="24" color="#f97316" />
      连续打卡 <strong>{{ checkinData.current_streak }}</strong> 天
      <el-button link size="small" @click="doCheckin" :disabled="checkedIn">
        {{ checkedIn ? '已打卡' : '打卡' }}
      </el-button>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, defineAsyncComponent } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { useStudyStore } from '@/stores/study'
import { LEVELS } from '@/utils/constants'
import * as checkinApi from '@/api/checkin'
import type { TodayPlan, CheckinResult } from '@/api/types'

const WordListPreview = defineAsyncComponent(() => import('./WordListPreview.vue'))

const router = useRouter()
const studyStore = useStudyStore()

const selectedLevel = ref(LEVELS[7].code) // default CET4
const loading = ref(false)
const plan = ref<TodayPlan | null>(null)
const checkinData = ref<CheckinResult | null>(null)
const checkedIn = ref(false)

const todayProgress = computed(() => {
  if (!plan.value) return 0
  const total = plan.value.review_count + plan.value.new_count
  return total === 0 ? 100 : 0
})

const progressColor = computed(() =>
  todayProgress.value === 100 ? '#10b981' : '#1890ff',
)

async function loadPlan() {
  loading.value = true
  try {
    plan.value = await studyStore.loadTodayPlan(selectedLevel.value)
  } finally {
    loading.value = false
  }
}

async function doCheckin() {
  checkinData.value = await checkinApi.checkinToday()
  checkedIn.value = true
}

function startStudy() {
  if (!plan.value) return
  const queue = [...plan.value.review_words, ...plan.value.new_words]
  studyStore.startSession(queue, selectedLevel.value)
  router.push(`/study/${selectedLevel.value}`)
}

onMounted(async () => {
  await loadPlan()
  try {
    checkinData.value = await checkinApi.checkinToday()
    checkedIn.value = true
  } catch {
    // not yet checked in today
  }
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.dashboard { padding: $space-6; max-width: 800px; }

.page-title { font-size: 1.5rem; font-weight: 700; margin-bottom: $space-4; }

.level-select { width: 200px; margin-bottom: $space-6; }

.overview-cards {
  display: flex;
  gap: $space-4;
  margin-bottom: $space-4;
}

.stat-card {
  flex: 1;
  background: #fff;
  border-radius: $radius-card;
  box-shadow: $shadow-card;
  padding: $space-4;
  text-align: center;
}

.stat-value { font-size: 2rem; font-weight: 700; color: $color-primary; font-family: $font-en; }
.stat-label { font-size: 0.8rem; color: #6b7280; margin-top: $space-1; }

.start-actions { margin-top: $space-6; text-align: center; }

.checkin-banner {
  margin-top: $space-4;
  display: flex;
  align-items: center;
  gap: $space-2;
  font-size: 0.9rem;
}
</style>
