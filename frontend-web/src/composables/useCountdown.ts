import { ref, onUnmounted } from 'vue'

export function useCountdown(seconds = 60) {
  const remaining = ref(0)
  let timer: ReturnType<typeof setInterval> | null = null

  function start(n = seconds) {
    if (timer) clearInterval(timer)
    remaining.value = n
    timer = setInterval(() => {
      remaining.value--
      if (remaining.value <= 0) stop()
    }, 1000)
  }

  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    remaining.value = 0
  }

  onUnmounted(stop)

  return { remaining, start, stop }
}
