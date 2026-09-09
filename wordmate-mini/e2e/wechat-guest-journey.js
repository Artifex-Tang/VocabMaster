'use strict'
/**
 * VocabMaster — 游客旅程仿真测试（微信审核整改：先体验后授权）
 *
 * G0 复位游客态（清 storage + 清 Pinia 内存 token，见 CLAUDE.md 坑#13：只清 storage 不够）
 * G1 首页停留不跳登录
 * G2 游客 hero 卡存在
 * G3 搜词入口存在
 * G4 进入搜索页
 * G5 搜词出结果（游客可体验核心功能）
 * G6 测试 tab 登录空态
 * G7 统计 tab 登录空态
 * G8 我的 tab 登录空态
 *
 * 前置：本地栈（backend-java 于 18080）+ mp 构建指向同端口。
 * 用法：cd wordmate-mini && WX_AUTO_PORT=60671 node e2e/wechat-guest-journey.js
 */
const { launchMiniProgram } = require('./launch-devtools')

const sleep = (ms) => new Promise(r => setTimeout(r, ms))
let pass = 0, fail = 0
const log = (ok, id, name, extra = '') => {
  console.log(`${ok ? '✓' : '✗'} [${id}] ${name}${extra ? `  (${extra})` : ''}`)
  if (ok) pass++; else fail++
}

async function main() {
  const mp = await launchMiniProgram()

  // ---- G0 复位游客态（evaluate 作用域无 uni 全局，用 wx/getApp；清 storage 用 callWxMethod）----
  await mp.callWxMethod('clearStorageSync')
  const cleared = await mp.evaluate(() => {
    const app = getApp()
    const gctx = app.$vm && app.$vm.$ && app.$vm.$.appContext
    const pinia = gctx && gctx.config && gctx.config.globalProperties.$pinia
    if (!pinia || !pinia._s) return 'no-pinia'
    const user = pinia._s.get('user')
    if (!user) return 'no-user-store'
    user.$patch({ accessToken: null, userInfo: null })
    return 'ok'
  })
  log(cleared === 'ok', 'G0', '复位游客态', String(cleared))
  if (cleared !== 'ok') {
    console.log('FATAL: 无法复位游客态，pinia 取法需按运行时结构调整')
    mp.disconnect()
    process.exit(1)
  }

  // ---- G1 首页停留 ----
  await mp.reLaunch('/pages/index/index')
  await sleep(1500)
  let page = await mp.currentPage()
  log(page && page.path === 'pages/index/index', 'G1', '游客首页不跳登录', page && page.path)

  // ---- G2/G3 游客元素 ----
  page = await mp.currentPage()
  const hero = await page.$('.guest-hero')
  log(!!hero, 'G2', '游客 hero 卡存在')
  const searchEntry = await page.$('.wordlist-entry')
  log(!!searchEntry, 'G3', '搜词入口存在')

  // ---- G4 进搜索页 ----
  if (searchEntry) {
    await searchEntry.tap()
    await sleep(1500)
  }
  page = await mp.currentPage()
  log(page && page.path === 'pages/word/search', 'G4', '进入搜索页', page && page.path)

  // ---- G5 搜词出结果（搜索只在 @confirm 触发，需模拟键盘确认）----
  const inp = await page.$('.search-input')
  if (inp) {
    await inp.input('cat')
    await sleep(300)
    await inp.trigger('confirm')
  }
  await sleep(2500) // 本地后端响应
  const items = await page.$$('.word-item')
  log(items.length > 0, 'G5', `游客搜词出结果 ${items.length} 条`)

  // ---- G6/G7/G8 tab 空态 ----
  await mp.switchTab('/pages/test/index')
  await sleep(1200)
  page = await mp.currentPage()
  log(!!(await page.$('.guest-empty')), 'G6', '测试 tab 登录空态')

  await mp.switchTab('/pages/stats/index')
  await sleep(1200)
  page = await mp.currentPage()
  log(!!(await page.$('.guest-empty')), 'G7', '统计 tab 登录空态')

  await mp.switchTab('/pages/mine/index')
  await sleep(1200)
  page = await mp.currentPage()
  log(!!(await page.$('.guest-empty')), 'G8', '我的 tab 登录空态')

  console.log('\n============================================')
  console.log(`游客旅程结果: ${pass} 通过 / ${fail} 失败`)
  console.log('============================================')
  mp.disconnect()
  process.exit(fail ? 1 : 0)
}

main().catch(e => { console.error('guest journey failed:', e); process.exit(1) })
