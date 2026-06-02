export interface TtsOptions {
  text: string
  accent?: 'uk' | 'us'
  rate?: number
  audioUrl?: string
}

// 有预录音频时，所有端优先用音频 URL
export function speak(opts: TtsOptions): Promise<void> {
  if (opts.audioUrl) return playAudio(opts.audioUrl)

  // #ifdef MP-WEIXIN
  return playFromDict(opts.text, opts.accent ?? 'us')
  // #endif

  // #ifdef APP-PLUS
  return appSpeak(opts)
  // #endif

  // #ifdef H5
  return h5Speak(opts)
  // #endif
}

function playAudio(url: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const ctx = uni.createInnerAudioContext()
    ctx.src = url
    ctx.onEnded(() => { ctx.destroy(); resolve() })
    ctx.onError((e) => { ctx.destroy(); reject(e) })
    ctx.play()
  })
}

// 有道词典音频 CDN（小程序需在管理台配置合法域名 dict.youdao.com）
function playFromDict(word: string, accent: 'uk' | 'us'): Promise<void> {
  const type = accent === 'uk' ? 1 : 2
  const url = `https://dict.youdao.com/dictvoice?audio=${encodeURIComponent(word)}&type=${type}`
  return playAudio(url)
}

// #ifdef APP-PLUS
function appSpeak(opts: TtsOptions): Promise<void> {
  return new Promise((resolve, reject) => {
    const p = (plus as unknown) as Record<string, unknown>
    if (p['speech']) {
      const speech = p['speech'] as {
        speak: (opts: { text: string; language: string; rate: number }, cb: () => void, err: (e: unknown) => void) => void
      }
      speech.speak(
        {
          text: opts.text,
          language: opts.accent === 'uk' ? 'en-GB' : 'en-US',
          rate: opts.rate ?? 1,
        },
        resolve,
        reject,
      )
    } else {
      playFromDict(opts.text, opts.accent ?? 'us').then(resolve).catch(reject)
    }
  })
}
// #endif

// #ifdef H5
function h5Speak(opts: TtsOptions): Promise<void> {
  return new Promise((resolve) => {
    if (typeof window === 'undefined' || !window.speechSynthesis) {
      return playFromDict(opts.text, opts.accent ?? 'us')
        .then(resolve)
        .catch(() => resolve())
    }
    const u = new SpeechSynthesisUtterance(opts.text)
    u.lang = opts.accent === 'uk' ? 'en-GB' : 'en-US'
    u.rate = opts.rate ?? 1
    u.onend = () => resolve()
    window.speechSynthesis.cancel()
    window.speechSynthesis.speak(u)
  })
}
// #endif
