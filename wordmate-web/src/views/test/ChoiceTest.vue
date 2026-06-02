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
        <div class="question">{{ currentQ.prompt.zh_definition }}</div>
        <div class="options">
          <div
            v-for="opt in currentQ.choices"
            :key="opt"
            class="option-btn"
            :class="{ selected: currentSelected === opt }"
            @click="choose(opt)"
          >
            {{ opt }}
          </div>
        </div>

        <!-- 状态 + 底部 -->
        <div v-if="isAnswered(currentIdx)" class="status answered">已选择: {{ getAnswer(currentIdx) }}</div>
        <div v-else class="status unanswered">未作答</div>

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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import * as testApi from '@/api/test'
import type { TestSession, TestSubmitAnswer } from '@/api/types'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const session = ref<TestSession | null>(null)
const currentIdx = ref(0)
const questionStart = ref(Date.now())

// Map: question_id -> answer (reactive so computed updates)
const answerMap = reactive(new Map<string, { answer: string; duration_ms: number }>())

const currentQ = computed(() => session.value?.questions[currentIdx.value])
const pct = computed(() => session.value ? Math.round((currentIdx.value + 1) / session.value.questions.length * 100) : 0)

// 当前题选中状态（从 answerMap 读取）
const currentSelected = computed(() => {
  const q = currentQ.value
  if (!q) return ''
  return answerMap.get(q.question_id)?.answer ?? ''
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

function choose(opt: string) {
  const q = currentQ.value
  if (!q) return
  answerMap.set(q.question_id, { answer: opt, duration_ms: Date.now() - questionStart.value })
  // 自动跳下一题
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

watch(currentIdx, () => { questionStart.value = Date.now() })

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
      mode: 'choice',
      size: Number(route.query.size ?? 20),
      source: (route.query.source as import('@/api/types').TestSource) ?? 'due',
    })
    loading.value = false
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

.question { font-size: 1.3rem; font-weight: 600; margin-bottom: 24px; text-align: center; }

.options { display: flex; flex-direction: column; gap: 10px; }
.option-btn {
  padding: 12px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 15px;
  line-height: 1.5;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
  user-select: none;
}
.option-btn:hover { border-color: #409eff; color: #409eff; }
.option-btn.selected { border-color: #409eff; background: #ecf5ff; color: #409eff; }

.status { text-align: center; font-size: 13px; margin-top: 16px; }
.answered { color: #67c23a; }
.unanswered { color: #909399; }

.bottom-actions { display: flex; gap: 12px; justify-content: center; margin-top: 20px; }
</style>
