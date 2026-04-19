export type Platform = 'miniprogram' | 'android' | 'ios' | 'h5'

export function getPlatform(): Platform {
  // #ifdef MP-WEIXIN
  return 'miniprogram'
  // #endif

  // #ifdef APP-PLUS
  const sys = uni.getSystemInfoSync()
  return sys.platform === 'ios' ? 'ios' : 'android'
  // #endif

  // #ifdef H5
  return 'h5'
  // #endif
}

export function isMiniProgram(): boolean {
  // #ifdef MP-WEIXIN
  return true
  // #endif
  return false
}

export function isApp(): boolean {
  // #ifdef APP-PLUS
  return true
  // #endif
  return false
}

export function isH5(): boolean {
  // #ifdef H5
  return true
  // #endif
  return false
}
