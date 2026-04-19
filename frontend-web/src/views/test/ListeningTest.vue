<template>
  <div class="test-session">
    <el-skeleton v-if="loading" :rows="5" animated />
    <template v-else-if="currentQ">
      <div class="header">
        <span>{{ currentIdx + 1 }} / {{ session!.questions.length }}</span>
        <el-progress :percentage="pct" :show-text="false" style="flex:1;margin:0 16px" />
      </div>
      <el-button type="primary" circle size="large" @click="playAudio" style="margin:24px auto;display:block;width:72px;height:72px">
        <Icon icon="mdi:volume-high" width="32" />
      </el-button>
      <el-input v-model="userAnswer" placeholder="输入您听到的单词" size="large" style="max-width:300px;margin:0 auto;display:block" @keyup.enter="submitOne" />
      <el-button type="primary" size="large" style="margin:16px auto;display:block" @click="submitOne">确认</el-button>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { useTts } from '@/composables/useTts'
import * as testApi from '@/api/test'
import type { TestSession, TestSubmitAnswer } from '@/api/types'

const route = useRoute()
const router = useRouter()
const { speakOrFallback } = useTts()

const loading = ref(true)
const session = ref<TestSession | null>(null)
const currentIdx = ref(0)
const userAnswer = ref('')
const answers: TestSubmitAnswer[] = []
let questionStart = Date.now()

const currentQ = computed(() => session.value?.questions[currentIdx.value])
const pct = computed(() => session.value ? Math.round(currentIdx.value / session.value.questions.length * 100) : 0)

function playAudio() {
  const q = currentQ.value
  if (!q) return
  speakOrFallback('', q.prompt.audio_url_uk, 'uk')
  if (q.prompt.audio_url_uk) new Audio(q.prompt.audio_url_uk).play()
}

function submitOne() {
  if (!currentQ.value) return
  answers.push({ question_id: currentQ.value.question_id, answer: userAnswer.value.trim(), duration_ms: Date.now() - questionStart })
  userAnswer.value = ''
  questionStart = Date.now()
  currentIdx.value++
  if (session.value && currentIdx.value >= session.value.questions.length) finish()
  else playAudio()
}

async function finish() {
  const result = await testApi.submit(session.value!.test_id, answers)
  router.replace({ path: '/test', query: { result: JSON.stringify(result) } })
}

onMounted(async () => {
  session.value = await testApi.generate({
    level_code: route.query.level as string,
    mode: 'listening',
    size: Number(route.query.size ?? 20),
    source: (route.query.source as import('@/api/types').TestSource) ?? 'due',
  })
  loading.value = false
  setTimeout(playAudio, 400)
})
</script>

<style scoped>
.test-session { padding: 24px; max-width: 600px; margin: 0 auto; text-align: center; }
.header { display: flex; align-items: center; margin-bottom: 24px; }
</style>
