<template>
  <div class="word-card" :class="{ flipped }" @click="handleFlip">
    <div class="card-inner">
      <!-- Front -->
      <div class="card-face card-front">
        <div v-if="word.emoji" class="word-emoji">{{ word.emoji }}</div>
        <img
          v-else-if="word.image_url"
          :src="word.image_url"
          class="word-image"
          loading="lazy"
        />
        <div class="word-text word-main">{{ word.word }}</div>
        <div class="ipa">
          <span>{{ preferredAccent === 'uk' ? word.ipa_uk : word.ipa_us }}</span>
          <el-button link class="audio-btn" @click.stop="playAudio">
            <Icon icon="mdi:volume-high" width="20" />
          </el-button>
        </div>
        <div v-if="word.topic_code" class="topic-tag">{{ word.topic_code }}</div>
        <div v-if="!flipped" class="flip-hint">点击查看释义</div>
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
          <div class="example-zh">{{ word.example_zh }}</div>
        </div>
        <div v-if="word.related_words?.synonyms?.length" class="back-section">
          <div class="back-label">近义词</div>
          <div class="back-content muted">{{ word.related_words.synonyms.join(' / ') }}</div>
        </div>
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

function handleFlip() {
  if (flipped.value) return
  flipped.value = true
  emit('flip')
  if (settingsStore.settings.auto_play_audio) playAudio()
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

.ipa {
  display: flex;
  align-items: center;
  gap: $space-2;
  margin-bottom: $space-3;
}

.audio-btn { padding: 0; }

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
