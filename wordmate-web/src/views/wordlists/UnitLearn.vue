<template>
  <div class="unit-learn">
    <!-- nav -->
    <div class="nav-bar">
      <el-button text @click="confirmExit">
        <Icon icon="mdi:arrow-left" width="20" /> 返回
      </el-button>
      <span class="unit-label">Unit {{ currentUnit }}</span>
      <el-progress
        v-if="!finished && queue.length"
        :percentage="progressPct"
        :show-text="false"
        style="flex: 1; margin: 0 8px"
      />
      <span class="progress-text" v-if="!finished && queue.length">{{ idx + 1 }} / {{ queue.length }}</span>
    </div>

    <!-- loading -->
    <div v-if="loading" class="center-msg">
      <el-icon class="is-loading"><Icon icon="mdi:loading" width="28" /></el-icon> 加载新词…
    </div>

    <!-- empty: 本单元已无新词 -->
    <div v-else-if="!queue.length && !finished" class="center-msg">
      <Icon icon="mdi:check-decagram" width="56" color="#10b981" />
      <div class="done-title">本单元新词已全部学过</div>
      <div class="done-sub">{{ nextUnitHint }}</div>
      <div class="done-actions">
        <el-button @click="$router.push(`/wordlists/${id}`)">返回词库</el-button>
        <el-button v-if="hasNext" type="primary" @click="goNextUnit">进入第 {{ currentUnit + 1 }} 单元</el-button>
      </div>
    </div>

    <!-- finished: 学完一批 -->
    <div v-else-if="finished" class="center-msg">
      <Icon icon="mdi:trophy-award" width="56" color="#f59e0b" />
      <div class="done-title">本单元学习完成 🎉</div>
      <div class="done-sub">学了 {{ learnedCount }} 个新词，艾宾浩斯已安排复习</div>
      <div class="done-actions">
        <el-button @click="$router.push(`/wordlists/${id}`)">返回词库</el-button>
        <el-button v-if="hasNext" type="primary" @click="goNextUnit">进入第 {{ currentUnit + 1 }} 单元</el-button>
        <el-button v-if="originLevel" @click="goReview">去复习</el-button>
      </div>
    </div>

    <!-- card session -->
    <template v-else-if="currentWord">
      <div class="card-area">
        <WordCard
          ref="cardRef"
          :key="currentWord.id"
          :word="currentWord"
          @flip="onFlip"
          style="height: 380px; width: 100%"
        />
      </div>
      <Transition name="fade">
        <div v-if="isFlipped" class="action-bar">
          <el-button class="action-btn wrong" size="large" @click="answer('wrong')">
            <Icon icon="mdi:close" width="22" /> 不认识
          </el-button>
          <el-button class="action-btn correct" type="success" size="large" @click="answer('correct')">
            <Icon icon="mdi:check" width="22" /> 认识
          </el-button>
        </div>
      </Transition>
      <div v-if="!isFlipped" class="flip-hint">点击卡片查看释义</div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Icon } from '@iconify/vue'
import WordCard from '@/components/WordCard.vue'
import { learn, advanceUnit, detail } from '@/api/wordList'
import { useSettingsStore } from '@/stores/settings'
import { useOfflineSync } from '@/composables/useOfflineSync'
import { nowIso } from '@/utils/date'
import type { WordBank } from '@/api/types'

const route = useRoute()
const router = useRouter()
const settingsStore = useSettingsStore()
const { submitAnswer } = useOfflineSync()

const id = Number(route.params.id)
const currentUnit = computed(() => (route.query.unit ? Number(route.query.unit) : 1))

const loading = ref(true)
const queue = ref<WordBank[]>([])
const idx = ref(0)
const isFlipped = ref(false)
const finished = ref(false)
const learnedCount = ref(0)
const unitCount = ref(0)
const originLevel = ref('')
const answerStart = ref(Date.now())
const cardRef = ref<InstanceType<typeof WordCard> | null>(null)

const currentWord = computed(() => queue.value[idx.value])
const progressPct = computed(() =>
  queue.value.length ? Math.round(((idx.value + 1) / queue.value.length) * 100) : 0,
)
const hasNext = computed(() => unitCount.value > 0 && currentUnit.value < unitCount.value)
const nextUnitHint = computed(() =>
  hasNext.value ? `可进入第 ${currentUnit.value + 1} 单元继续` : '已是最后一个单元，去复习巩固吧',
)

async function loadUnit() {
  loading.value = true
  finished.value = false
  learnedCount.value = 0
  idx.value = 0
  isFlipped.value = false
  try {
    const limit = settingsStore.settings?.daily_new_words_goal || 20
    const [words, d] = await Promise.all([learn(id, currentUnit.value, limit), detail(id)])
    queue.value = words
    unitCount.value = d.unit_count
    originLevel.value = d.origin_level_code
    answerStart.value = Date.now()
  } finally {
    loading.value = false
  }
}

function onFlip() {
  isFlipped.value = true
  answerStart.value = Date.now()
}

function resetCard() {
  isFlipped.value = false
  cardRef.value?.reset()
  answerStart.value = Date.now()
}

async function answer(result: 'correct' | 'wrong') {
  if (!currentWord.value) return
  const w = currentWord.value
  await submitAnswer({
    word_id: w.id,
    level_code: w.level_code,
    result,
    mode: 'card',
    duration_ms: Date.now() - answerStart.value,
    client_ts: nowIso(),
  }).catch(() => {})
  if (result === 'correct') learnedCount.value++
  idx.value++
  if (idx.value >= queue.value.length) {
    finished.value = true
    return
  }
  resetCard()
}

async function goNextUnit() {
  const next = currentUnit.value + 1
  await advanceUnit(id, next).catch(() => {})
  await router.replace({ path: `/wordlists/${id}/learn`, query: { unit: next } })
  // watch(currentUnit) 会触发 loadUnit
}

function goReview() {
  if (!originLevel.value) return
  router.push({ path: '/test', query: { level: originLevel.value, source: 'due' } })
}

async function confirmExit() {
  try {
    await ElMessageBox.confirm('退出本次学习？已学进度已记录。', '退出', {
      confirmButtonText: '退出',
      cancelButtonText: '继续',
      type: 'warning',
    })
    router.push(`/wordlists/${id}`)
  } catch {
    /* 取消 */
  }
}

watch(currentUnit, loadUnit)
onMounted(loadUnit)
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.unit-learn {
  max-width: 640px;
  margin: 0 auto;
  padding: $space-4;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}
.nav-bar {
  display: flex;
  align-items: center;
  gap: $space-2;
  margin-bottom: $space-3;
}
.unit-label {
  font-weight: 600;
  color: $color-primary;
  font-size: 0.9rem;
}
.progress-text {
  font-family: $font-en;
  font-size: 0.9rem;
  color: #374151;
  white-space: nowrap;
}
.center-msg {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $space-3;
  text-align: center;
  color: #6b7280;
}
.done-title {
  font-size: 1.3rem;
  font-weight: 700;
  color: #1f2937;
}
.done-sub {
  font-size: 0.9rem;
}
.done-actions {
  display: flex;
  gap: $space-3;
  margin-top: $space-3;
}
.card-area {
  flex: 1;
  padding: $space-4 0;
}
.action-bar {
  display: flex;
  gap: $space-3;
  padding: $space-4 0;
  justify-content: center;
  .action-btn {
    flex: 1;
    max-width: 180px;
    height: 60px;
    font-size: 1rem;
    &.wrong {
      border-color: $color-error;
      color: $color-error;
    }
  }
}
.flip-hint {
  text-align: center;
  font-size: 0.8rem;
  color: #9ca3af;
  padding-bottom: $space-4;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
