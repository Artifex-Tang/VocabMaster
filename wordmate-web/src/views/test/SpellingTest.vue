<template>
  <div class="test-session">
    <el-skeleton v-if="loading" :rows="5" animated />
    <template v-else-if="session">
      <!-- 进度条 -->
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

      <!-- 题目 -->
      <div v-if="currentQ" class="question-area">
        <!-- 释义切换 -->
        <div class="prompt-toggle">
          <el-button-group>
            <el-button :type="promptLang === 'zh' ? 'primary' : 'default'" size="small" @click="promptLang = 'zh'">中文释义</el-button>
            <el-button :type="promptLang === 'en' ? 'primary' : 'default'" size="small" @click="promptLang = 'en'">英文释义</el-button>
          </el-button-group>
        </div>

        <!-- 题面 -->
        <div class="prompt-text">
          {{ promptLang === 'en' ? (currentQ.prompt.en_definition || '暂无英文释义') : currentQ.prompt.zh_definition }}
        </div>

        <!-- 播放发音 -->
        <el-button circle @click="playAudio" title="播放发音">
          <Icon icon="mdi:volume-high" width="22" />
        </el-button>

        <!-- 输入区 -->
        <div class="input-row">
          <el-input
            ref="inputRef"
            v-model="currentInput"
            placeholder="输入单词"
            size="large"
            @keyup.enter="confirmCurrent"
            style="flex:1;max-width:320px"
          />
          <el-button type="primary" size="large" @click="confirmCurrent">确认</el-button>
        </div>

        <!-- 状态指示 -->
        <div v-if="isAnswered(currentIdx)" class="status answered">已作答</div>
        <div v-else class="status unanswered">未作答</div>

        <!-- 底部操作 -->
        <div class="bottom-actions">
          <el-button @click="skipCurrent">跳过此题</el-button>
          <el-button type="success" @click="submitAll" :disabled="!hasAnyAnswer">
            交卷 ({{ answeredCount }}/{{ session.questions.length }})
          </el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
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
const promptLang = ref<'zh' | 'en'>('zh')
const inputRef = ref()
const questionStart = ref(Date.now())

// Map: question_id -> answer (reactive so computed updates)
const answerMap = reactive(new Map<string, { answer: string; duration_ms: number }>())

const currentQ = computed(() => session.value?.questions[currentIdx.value])
const pct = computed(() => session.value ? Math.round((currentIdx.value + 1) / session.value.questions.length * 100) : 0)

// 当前题的输入值（双向绑定）
const currentInput = computed({
  get: () => {
    const q = currentQ.value
    if (!q) return ''
    return answerMap.get(q.question_id)?.answer ?? ''
  },
  set: (val: string) => {
    // 仅用于输入框显示，不自动保存
  }
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

// 保存当前题答案
function saveCurrent() {
  const q = currentQ.value
  if (!q) return
  const val = currentInput.value?.trim() ?? '' // re-read from input
  // Actually read from DOM via ref
}

function confirmCurrent() {
  const q = currentQ.value
  if (!q) return
  const input = inputRef.value?.input?.value ?? ''
  const val = input.trim()
  if (!val) return
  answerMap.set(q.question_id, { answer: val, duration_ms: Date.now() - questionStart.value })
  // 自动跳到下一题
  if (currentIdx.value < (session.value?.questions.length ?? 0) - 1) {
    currentIdx.value++
  }
}

function skipCurrent() {
  const q = currentQ.value
  if (!q) return
  // 标记为空答案（跳过）
  answerMap.set(q.question_id, { answer: '', duration_ms: Date.now() - questionStart.value })
  if (currentIdx.value < (session.value?.questions.length ?? 0) - 1) {
    currentIdx.value++
  }
}

function prev() {
  if (currentIdx.value > 0) currentIdx.value--
}

function next() {
  if (session.value && currentIdx.value < session.value.questions.length - 1) currentIdx.value++
}

// 切题时重置计时器
watch(currentIdx, () => {
  questionStart.value = Date.now()
  nextTick(() => inputRef.value?.focus?.())
})

function playAudio() {
  const q = currentQ.value
  if (!q) return
  const accent = settingsStore.settings.preferred_accent || 'uk'
  const url = accent === 'us' ? q.prompt.audio_url_us : q.prompt.audio_url_uk
  if (url) {
    new Audio(url).play()
  }
}

async function submitAll() {
  if (!session.value) return
  // 收集所有答案，未答的填空字符串
  const answers: TestSubmitAnswer[] = session.value.questions.map(q => {
    const saved = answerMap.get(q.question_id)
    return {
      question_id: q.question_id,
      answer: saved?.answer ?? '',
      duration_ms: saved?.duration_ms ?? 0,
    }
  })
  const result = await testApi.submit(session.value.test_id, answers)
  router.replace({ path: '/test', query: { result: JSON.stringify(result) } })
}

onMounted(async () => {
  try {
    session.value = await testApi.generate({
      level_code: route.query.level as string,
      mode: 'spelling',
      size: Number(route.query.size ?? 20),
      source: (route.query.source as import('@/api/types').TestSource) ?? 'due',
    })
    loading.value = false
    nextTick(() => inputRef.value?.focus?.())
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

.question-area { display: flex; flex-direction: column; align-items: center; gap: 16px; }

.prompt-toggle { margin-bottom: 4px; }

.prompt-text {
  font-size: 1.3rem;
  font-weight: 600;
  text-align: center;
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  width: 100%;
  line-height: 1.6;
}

.input-row { display: flex; gap: 8px; align-items: center; width: 100%; justify-content: center; }

.status { font-size: 13px; }
.answered { color: #67c23a; }
.unanswered { color: #909399; }

.bottom-actions { display: flex; gap: 12px; margin-top: 8px; }
</style>
