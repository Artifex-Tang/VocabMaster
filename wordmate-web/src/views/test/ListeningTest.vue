<template>
  <div class="test-session">
    <el-skeleton v-if="loading" :rows="5" animated />
    <template v-else-if="session">
      <!-- 导航栏 -->
      <div class="header">
        <el-button :disabled="currentIdx <= 0" @click="prev" text>
          <Icon icon="mdi:chevron-left" width="22" />
        </el-button>
        <el-progress :percentage="pct" :show-text="false" style="flex:1;margin:0 8px" />
        <span class="progress-text">{{ currentIdx + 1 }} / {{ session.questions.length }}</span>
        <el-button :disabled="currentIdx >= session.questions.length - 1" @click="next" text>
          <Icon icon="mdi:chevron-right" width="22" />
        </el-button>
      </div>

      <template v-if="currentQ">
        <!-- 播放按钮 -->
        <el-button type="primary" circle size="large" @click="playAudio" class="audio-btn">
          <Icon icon="mdi:volume-high" width="32" />
        </el-button>

        <!-- 输入区 -->
        <div class="input-row">
          <el-input
            ref="inputRef"
            v-model="currentInput"
            placeholder="输入您听到的单词"
            size="large"
            @keyup.enter="confirmCurrent"
          />
          <el-button type="primary" size="large" @click="confirmCurrent">确认</el-button>
        </div>

        <!-- 状态 -->
        <div v-if="isAnswered(currentIdx)" class="status answered">已作答: {{ getAnswer(currentIdx) || '(空)' }}</div>
        <div v-else class="status unanswered">未作答</div>

        <!-- 底部 -->
        <div class="bottom-actions">
          <el-button @click="skipCurrent">跳过</el-button>
          <el-button type="success" @click="submitAll" :disabled="!hasAnyAnswer">
            交卷 ({{ answeredCount }}/{{ session.questions.length }})
          </el-button>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { useTts } from '@/composables/useTts'
import { useSettingsStore } from '@/stores/settings'
import * as testApi from '@/api/test'
import type { TestSession, TestSubmitAnswer } from '@/api/types'

const route = useRoute()
const router = useRouter()
const { speakOrFallback } = useTts()
const settingsStore = useSettingsStore()

const loading = ref(true)
const session = ref<TestSession | null>(null)
const currentIdx = ref(0)
const inputRef = ref()
const questionStart = ref(Date.now())

// Map: question_id -> answer (reactive so computed updates)
const answerMap = reactive(new Map<string, { answer: string; duration_ms: number }>())

const currentQ = computed(() => session.value?.questions[currentIdx.value])
const pct = computed(() => session.value ? Math.round((currentIdx.value + 1) / session.value.questions.length * 100) : 0)

const currentInput = computed({
  get: () => {
    const q = currentQ.value
    if (!q) return ''
    return answerMap.get(q.question_id)?.answer ?? ''
  },
  set: () => {},
})

const answeredCount = computed(() => {
  if (!session.value) return 0
  return session.value.questions.filter(q => answerMap.has(q.question_id)).length
})

const hasAnyAnswer = computed(() => answerMap.size > 0)

function isAnswered(idx: number): boolean {
  const q = session.value?.questions[idx]
  return !!q && answerMap.has(q.question_id)
}

function getAnswer(idx: number): string {
  const q = session.value?.questions[idx]
  return q ? (answerMap.get(q.question_id)?.answer ?? '') : ''
}

function confirmCurrent() {
  const q = currentQ.value
  if (!q) return
  const input = inputRef.value?.input?.value ?? ''
  const val = input.trim()
  if (!val) return
  answerMap.set(q.question_id, { answer: val, duration_ms: Date.now() - questionStart.value })
  if (session.value && currentIdx.value < session.value.questions.length - 1) {
    currentIdx.value++
  }
}

function skipCurrent() {
  const q = currentQ.value
  if (!q) return
  answerMap.set(q.question_id, { answer: '', duration_ms: Date.now() - questionStart.value })
  if (session.value && currentIdx.value < session.value.questions.length - 1) {
    currentIdx.value++
  }
}

function prev() { if (currentIdx.value > 0) currentIdx.value-- }
function next() {
  if (session.value && currentIdx.value < session.value.questions.length - 1) currentIdx.value++
}

watch(currentIdx, () => {
  questionStart.value = Date.now()
  nextTick(() => {
    inputRef.value?.focus?.()
    playAudio()
  })
})

function playAudio() {
  const q = currentQ.value
  if (!q) return
  const accent = settingsStore.settings.preferred_accent || 'uk'
  const url = accent === 'us' ? q.prompt.audio_url_us : q.prompt.audio_url_uk
  speakOrFallback(q.prompt.word || '', url, accent)
}

async function submitAll() {
  if (!session.value) return
  const answers: TestSubmitAnswer[] = session.value.questions.map(q => {
    const saved = answerMap.get(q.question_id)
    return { question_id: q.question_id, answer: saved?.answer ?? '', duration_ms: saved?.duration_ms ?? 0 }
  })
  const result = await testApi.submit(session.value.test_id, answers)
  router.replace({ path: '/test', query: { result: JSON.stringify(result) } })
}

onMounted(async () => {
  try {
    session.value = await testApi.generate({
      level_code: route.query.level as string,
      mode: 'listening',
      size: Number(route.query.size ?? 20),
      source: (route.query.source as import('@/api/types').TestSource) ?? 'due',
    })
    loading.value = false
    setTimeout(playAudio, 400)
  } catch {
    loading.value = false
    router.replace('/test')
  }
})
</script>

<style scoped>
.test-session { padding: 24px; max-width: 600px; margin: 0 auto; }
.header { display: flex; align-items: center; margin-bottom: 20px; gap: 4px; }
.progress-text { font-size: 14px; color: #606266; white-space: nowrap; min-width: 60px; text-align: right; }

.audio-btn { display: block; margin: 0 auto 24px; width: 72px; height: 72px; }

.input-row { display: flex; gap: 8px; justify-content: center; }

.status { text-align: center; font-size: 13px; margin-top: 16px; }
.answered { color: #67c23a; }
.unanswered { color: #909399; }

.bottom-actions { display: flex; gap: 12px; justify-content: center; margin-top: 20px; }
</style>
