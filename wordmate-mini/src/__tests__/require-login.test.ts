import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/stores/user'
import { requireLogin } from '@/utils/require-login'

const showModal = vi.fn()
const reLaunch = vi.fn()

vi.stubGlobal('uni', { showModal, reLaunch })

describe('requireLogin', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    showModal.mockClear()
    reLaunch.mockClear()
  })

  it('未登录：返回 false，弹 modal 引导，确认后跳登录页', () => {
    const r = requireLogin()
    expect(r).toBe(false)
    expect(showModal).toHaveBeenCalledTimes(1)
    const opts = showModal.mock.calls[0][0]
    expect(opts.title).toBe('需要登录')
    opts.success({ confirm: true })
    expect(reLaunch).toHaveBeenCalledWith({ url: '/pages/auth/login' })
  })

  it('未登录：modal 取消不跳转', () => {
    requireLogin()
    showModal.mock.calls[0][0].success({ confirm: false })
    expect(reLaunch).not.toHaveBeenCalled()
  })

  it('已登录：返回 true，不打扰', () => {
    const store = useUserStore()
    store.accessToken = 'tok'
    store.userInfo = { uuid: 'u1', nickname: 'n', locale: 'zh', timezone: 'Asia/Shanghai' }
    const r = requireLogin()
    expect(r).toBe(true)
    expect(showModal).not.toHaveBeenCalled()
  })
})
