'use strict'
/** diag6 — 游客态首页截图：复位游客态 → 截首页/测试tab，验证视觉渲染 */
const { launchMiniProgram } = require('./launch-devtools')
const sleep = (ms) => new Promise(r => setTimeout(r, ms))

async function main() {
  const mp = await launchMiniProgram()
  await mp.callWxMethod('clearStorageSync')
  await mp.evaluate(() => {
    const pinia = getApp().$vm.$.appContext.config.globalProperties.$pinia
    pinia._s.get('user').$patch({ accessToken: null, userInfo: null })
  })
  await mp.reLaunch('/pages/index/index')
  await sleep(2000)
  const page = await mp.currentPage()
  console.log('page:', page.path)
  await mp.screenshot({ path: 'e2e/wechat-screenshots/guest-home.png' })

  // 页面数据快照：游客视图元素在不在
  const hero = await page.$('.guest-hero')
  const search = await page.$('.wordlist-entry')
  const plan = await page.$('.plan-card')
  console.log('hero:', !!hero, 'searchEntry:', !!search, 'planCard(should be false):', !!plan)

  await mp.disconnect()
  process.exit(0)
}
main().catch(e => { console.error(e); process.exit(1) })
