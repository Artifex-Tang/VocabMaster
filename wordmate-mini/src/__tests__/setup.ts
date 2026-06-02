// 全局 mock uni-app API
const storage: Record<string, string> = {}

const uni = {
  getStorageSync: (key: string) => storage[key] ?? '',
  setStorageSync: (key: string, value: string) => { storage[key] = value },
  removeStorageSync: (key: string) => { delete storage[key] },
  clearStorageSync: () => { for (const k in storage) delete storage[k] },
  getSystemInfoSync: () => ({
    platform: 'devtools',
    model: 'Test',
    system: 'test',
  }),
  getNetworkType: (opts: { success?: (res: { networkType: string }) => void }) => {
    opts.success?.({ networkType: 'wifi' })
  },
  onNetworkStatusChange: () => {},
  createInnerAudioContext: () => ({
    src: '',
    play: () => {},
    onEnded: (cb: () => void) => { cb() },
    onError: () => {},
    destroy: () => {},
  }),
  request: () => {},
  showToast: () => {},
  showLoading: () => {},
  hideLoading: () => {},
  showModal: () => {},
  navigateTo: () => {},
  redirectTo: () => {},
  reLaunch: () => {},
  switchTab: () => {},
  navigateBack: () => {},
  login: (opts: { success?: (res: { code: string }) => void }) => {
    opts.success?.({ code: 'test_code' })
  },
}

// 注入到全局
;(globalThis as Record<string, unknown>).uni = uni

export { uni, storage }
