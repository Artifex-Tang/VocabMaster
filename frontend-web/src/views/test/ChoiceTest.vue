<template>
  <div class="test-session">
    <el-skeleton v-if="loading" :rows="5" animated />
    <template v-else-if="currentQ">
      <div class="header">
        <span>{{ currentIdx + 1 }} / {{ session!.questions.length }}</span>
        <el-progress :percentage="pct" :show-text="false" style="flex:1;margin:0 16px" />
      </div>
      <div class="question">{{ currentQ.prompt.zh_definition }}</div>
      <div class="options">
        <el-button
          v-for="opt in currentQ.prompt.options"
          :key="opt"
          size="large"
          class="option-btn"
          :type="selected === opt ? (isCorrect(opt) ? 'success' : 'danger') : 'default'"
          :disabled="!!selected"
          @click="choose(opt)"
        >
          {{ opt }}
        </el-button>
      </div>
      <el-button v-if="selected" type="primary" style="margin-top:24px" @click="next">
        {{ currentIdx + 1 >= session!.questions.length ? '完成' : '下一题' }}
      </el-button>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as testApi from '@/api/test'
import type { TestSession, TestSubmitAnswer } from '@/api/types'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const session = ref<TestSession | null>(null)
const currentIdx = ref(0)
const selected = ref('')
const answers: TestSubmitAnswer[] = []
let questionStart = Date.now()

const currentQ = computed(() => session.value?.questions[currentIdx.value])
const pct = computed(() => session.value ? Math.round(currentIdx.value / session.value.questions.length * 100) : 0)

function isCorrect(_opt: string) { return false /* revealed after backend result */ }

function choose(opt: string) {
  if (selected.value) return
  selected.value = opt
  answers.push({ question_id: currentQ.value!.question_id, answer: opt, duration_ms: Date.now() - questionStart })
}

function next() {
  selected.value = ''
  questionStart = Date.now()
  currentIdx.value++
  if (session.value && currentIdx.value >= session.value.questions.length) finish()
}

async function finish() {
  const result = await testApi.submit(session.value!.test_id, answers)
  router.replace({ path: '/test', query: { result: JSON.stringify(result) } })
}

onMounted(async () => {
  session.value = await testApi.generate({
    level_code: route.query.level as string,
    mode: 'choice',
    size: Number(route.query.size ?? 20),
    source: (route.query.source as import('@/api/types').TestSource) ?? 'due',
  })
  loading.value = false
})
</script>

<style scoped>
.test-session { padding: 24px; max-width: 600px; margin: 0 auto; }
.header { display: flex; align-items: center; margin-bottom: 24px; }
.question { font-size: 1.3rem; font-weight: 600; margin-bottom: 24px; text-align: center; }
.options { display: flex; flex-direction: column; gap: 12px; }
.option-btn { width: 100%; text-align: left; justify-content: flex-start; }
</style>
