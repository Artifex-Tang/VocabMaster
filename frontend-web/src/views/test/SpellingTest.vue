<template>
  <div class="test-session">
    <el-skeleton v-if="loading" :rows="5" animated />
    <template v-else-if="session">
      <div class="header">
        <span>{{ currentIdx + 1 }} / {{ session.questions.length }}</span>
        <el-progress :percentage="pct" :show-text="false" style="flex:1;margin:0 16px" />
      </div>

      <div v-if="currentQ" class="question-area">
        <div class="zh-prompt">{{ currentQ.prompt.zh_definition }}</div>
        <el-button v-if="currentQ.prompt.audio_url_uk" circle @click="playAudio">
          <Icon icon="mdi:volume-high" width="22" />
        </el-button>
        <el-input
          ref="inputRef"
          v-model="userAnswer"
          placeholder="输入单词"
          size="large"
          @keyup.enter="submitOne"
          style="max-width:320px;margin-top:24px"
        />
        <el-button type="primary" size="large" style="margin-top:16px" @click="submitOne">
          确认
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import * as testApi from '@/api/test'
import type { TestSession, TestSubmitAnswer } from '@/api/types'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const session = ref<TestSession | null>(null)
const currentIdx = ref(0)
const userAnswer = ref('')
const answers: TestSubmitAnswer[] = []
let questionStart = Date.now()

const currentQ = computed(() => session.value?.questions[currentIdx.value])
const pct = computed(() => session.value ? Math.round(currentIdx.value / session.value.questions.length * 100) : 0)

function playAudio() {
  const url = currentQ.value?.prompt.audio_url_uk
  if (url) new Audio(url).play()
}

function submitOne() {
  if (!currentQ.value) return
  answers.push({
    question_id: currentQ.value.question_id,
    answer: userAnswer.value.trim(),
    duration_ms: Date.now() - questionStart,
  })
  userAnswer.value = ''
  questionStart = Date.now()
  currentIdx.value++
  if (session.value && currentIdx.value >= session.value.questions.length) {
    finish()
  }
}

async function finish() {
  if (!session.value) return
  const result = await testApi.submit(session.value.test_id, answers)
  router.replace({ path: '/test', query: { result: JSON.stringify(result) } })
}

onMounted(async () => {
  session.value = await testApi.generate({
    level_code: route.query.level as string,
    mode: 'spelling',
    size: Number(route.query.size ?? 20),
    source: (route.query.source as import('@/api/types').TestSource) ?? 'due',
  })
  loading.value = false
})
</script>

<style scoped>
.test-session { padding: 24px; max-width: 600px; margin: 0 auto; }
.header { display: flex; align-items: center; margin-bottom: 24px; }
.question-area { display: flex; flex-direction: column; align-items: center; gap: 16px; }
.zh-prompt { font-size: 1.4rem; font-weight: 600; }
</style>
