import { defineStore } from 'pinia'
import { ref } from 'vue'

// 微信小程序「用户隐私保护指引」授权回调
// resolve 由 wx.onNeedPrivacyAuthorization 下发，同意/拒绝时回传以放行或中断隐私接口调用
type PrivacyResolver = (params: { event: 'agree' | 'disagree'; buttonId: string }) => void

export const usePrivacyStore = defineStore('privacy', () => {
  const visible = ref(false)
  let pending: PrivacyResolver | null = null

  function request(resolve?: PrivacyResolver) {
    pending = resolve ?? null
    visible.value = true
  }

  function agree() {
    visible.value = false
    pending?.({ event: 'agree', buttonId: 'agree-btn' })
    pending = null
  }

  function disagree() {
    visible.value = false
    pending?.({ event: 'disagree', buttonId: 'agree-btn' })
    pending = null
  }

  return { visible, request, agree, disagree }
})
