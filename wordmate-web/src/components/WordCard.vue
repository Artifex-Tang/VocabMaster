<template>
  <div class="word-card" :class="{ flipped }" @click="handleFlip">
    <div class="card-inner">
      <!-- Front -->
      <div class="card-face card-front">
        <img
          v-if="word.image_url"
          :src="word.image_url"
          class="word-image"
          loading="lazy"
        />
        <div v-else-if="word.emoji" class="word-emoji">{{ word.emoji }}</div>
        <div class="word-text word-main">{{ word.word }}</div>
        <div v-if="posLabel" class="pos-tag">{{ posLabel }}</div>
        <div class="ipa">
          <span>{{ preferredAccent === 'uk' ? word.ipa_uk : word.ipa_us }}</span>
          <el-button link class="audio-btn" @click.stop="playAudio">
            <Icon icon="mdi:volume-high" width="20" />
          </el-button>
        </div>
        <div v-if="formHint" class="form-hint">{{ formHint }}</div>
        <div v-if="word.topic_code" class="topic-tag">{{ word.topic_code }}</div>
        <div class="flip-hint">{{ flipped ? '点击翻回正面' : '点击查看释义' }}</div>
      </div>

      <!-- Back -->
      <div class="card-face card-back">
        <div class="back-section">
          <div class="back-label">English</div>
          <div class="back-content">{{ word.en_definition }}</div>
        </div>
        <div class="back-section">
          <div class="back-label">中文</div>
          <div class="back-content zh">{{ word.zh_definition }}</div>
        </div>
        <div v-if="word.example_en" class="back-section example">
          <div class="back-label">Example</div>
          <div class="back-content en-example">"{{ word.example_en }}"</div>
          <div v-if="word.example_zh" class="example-zh">{{ word.example_zh }}</div>
        </div>
        <div v-if="word.related_words?.synonyms?.length" class="back-section">
          <div class="back-label">近义词</div>
          <div class="back-content muted">{{ word.related_words.synonyms.join(' / ') }}</div>
        </div>
        <div v-if="word.related_words?.derived?.length" class="back-section">
          <div class="back-label">衍生词</div>
          <div class="back-content muted">{{ word.related_words.derived.join(' / ') }}</div>
        </div>
        <div class="flip-hint-back">点击翻回正面</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Icon } from '@iconify/vue'
import { useSettingsStore } from '@/stores/settings'
import { useTts } from '@/composables/useTts'
import type { WordBank } from '@/api/types'

const POS_MAP: Record<string, string> = {
  n: 'n.', v: 'v.', j: 'adj.', r: 'adv.', i: 'interj.',
  p: 'pron.', c: 'conj.', d: 'det.', m: 'num.', u: 'prep.', a: 'art.',
}

const props = defineProps<{
  word: WordBank
}>()

const emit = defineEmits<{
  flip: []
}>()

const flipped = ref(false)
const settingsStore = useSettingsStore()
const { speakOrFallback } = useTts()

const preferredAccent = computed(() => settingsStore.settings.preferred_accent)

const posLabel = computed(() => {
  const code = props.word.pos?.split(':')[0]
  return code ? (POS_MAP[code] || code) : ''
})

const formHint = computed(() => {
  const zh = props.word.zh_definition || ''
  const m = zh.match(/[（(](.+?(?:复数|单数|过去式|过去分词|现在分词|比较级|最高级|第三人称).+?)[）)]/)
  return m ? m[1] : ''
})

function handleFlip() {
  flipped.value = !flipped.value
  if (flipped.value) {
    emit('flip')
    if (settingsStore.settings.auto_play_audio) playAudio()
  }
}

function playAudio() {
  const url =
    preferredAccent.value === 'uk' ? props.word.audio_url_uk : props.word.audio_url_us
  speakOrFallback(props.word.word, url, preferredAccent.value)
}

function reset() {
  flipped.value = false
}

defineExpose({ reset, flip: handleFlip, playAudio })
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.word-emoji { font-size: 3rem; margin-bottom: $space-3; }

.word-image {
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: $radius-btn;
  margin-bottom: $space-3;
}

.word-main { font-size: 2.2rem; margin-bottom: $space-2; }

.pos-tag {
  font-size: 0.8rem;
  color: #6366f1;
  background: #eef2ff;
  padding: 2px 10px;
  border-radius: 12px;
  margin-bottom: $space-2;
}

.ipa {
  display: flex;
  align-items: center;
  gap: $space-2;
  margin-bottom: $space-3;
}

.audio-btn { padding: 0; }

.form-hint {
  font-size: 0.8rem;
  color: #f59e0b;
  background: #fffbeb;
  padding: 2px 8px;
  border-radius: 8px;
  margin-bottom: $space-3;
}

.topic-tag {
  font-size: 0.75rem;
  color: $color-primary;
  background: #e6f4ff;
  padding: 2px 8px;
  border-radius: 12px;
  margin-bottom: $space-3;
}

.flip-hint {
  font-size: 0.8rem;
  color: #9ca3af;
  margin-top: auto;
}

.flip-hint-back {
  font-size: 0.75rem;
  color: #9ca3af;
  margin-top: auto;
  text-align: center;
}

.back-section {
  width: 100%;
  margin-bottom: $space-4;
}

.back-label {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #9ca3af;
  margin-bottom: $space-1;
}

.back-content {
  font-size: 1rem;
  &.zh { font-family: $font-zh; font-size: 1.1rem; }
  &.muted { color: #6b7280; font-size: 0.9rem; }
}

.en-example { font-style: italic; color: #374151; }
.example-zh { font-size: 0.85rem; color: #6b7280; margin-top: 4px; }
</style>
