import { ref } from 'vue'

export function useTts() {
  const speaking = ref(false)

  function speak(text: string, accent: 'uk' | 'us' = 'uk', rate = 0.9) {
    if (!('speechSynthesis' in window)) return

    window.speechSynthesis.cancel()
    const utter = new SpeechSynthesisUtterance(text)
    utter.lang = accent === 'uk' ? 'en-GB' : 'en-US'
    utter.rate = rate

    const voices = window.speechSynthesis.getVoices()
    const match = voices.find(v => v.lang.startsWith(accent === 'uk' ? 'en-GB' : 'en-US'))
    if (match) utter.voice = match

    utter.onstart = () => { speaking.value = true }
    utter.onend = () => { speaking.value = false }
    utter.onerror = () => { speaking.value = false }

    window.speechSynthesis.speak(utter)
  }

  function speakOrFallback(
    text: string,
    audioUrl: string | undefined,
    accent: 'uk' | 'us' = 'uk',
  ) {
    if (audioUrl) {
      const audio = new Audio(audioUrl)
      audio.play().catch(() => speak(text, accent))
    } else {
      speak(text, accent)
    }
  }

  function cancel() {
    window.speechSynthesis?.cancel()
    speaking.value = false
  }

  return { speak, speakOrFallback, cancel, speaking }
}
