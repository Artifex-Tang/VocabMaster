'use strict'
/**
 * diag4 — 设置页「代码质量」问题诊断：抓微信开发者工具 console/exception 日志。
 * 流程：连 automator → 真登录(tester@prod) → 进设置页 → stepper/主题/保存 → dump 全部 console。
 * 用法：cd wordmate-mini && WX_AUTO_PORT=60661 node e2e/diag4.js
 */
const { launchMiniProgram } = require('./launch-devtools')

const EMAIL = 'tester@vocab-master.cn'
const PWD = 'VocabMaster2026'
const sleep = (ms) => new Promise(r => setTimeout(r, ms))

async function main() {
  const mp = await launchMiniProgram()
  const logs = []

  mp.on('console', (msg) => {
    logs.push(`[console.${msg.type}] ${msg.args.join(' ')}`)
  })
  mp.on('exception', (exp) => {
    logs.push(`[exception] ${exp.message}\n${exp.stack || ''}`)
  })

  const currentPage = () => mp.currentPage().catch(() => null)
  async function nav(url) {
    await mp.reLaunch(url)
    await sleep(1500)
    return currentPage()
  }

  // ---- 登录 ----
  let page = await nav('/pages/auth/login')
  const inputs = await page.$$('input')
  console.log('inputs:', inputs.length)
  await inputs[0].input(EMAIL)
  await sleep(300)
  await inputs[1].input(PWD)
  await sleep(300)
  const btn = await page.$('.btn-primary')
  await btn.tap()
  let left = false
  for (let i = 0; i < 20; i++) {
    await sleep(800)
    const cur = await currentPage()
    if (cur && cur.path !== 'pages/auth/login') { left = true; page = cur; break }
  }
  console.log('login left page:', left, '→', page && page.path)
  if (!left) { dumpAndExit(mp, logs) }

  // ---- 设置页 ----
  page = await nav('/pages/mine/settings')
  console.log('settings page:', page && page.path)

  // stepper +5
  const plus = await page.$$('.stepper-btn')
  console.log('stepper buttons:', plus.length)
  if (plus.length >= 2) {
    await plus[1].tap() // 第一行第二个 = daily_new_words_goal +
    await sleep(1200)
  }

  // 主题切换
  const toggles = await page.$$('.toggle')
  console.log('toggles:', toggles.length)
  if (toggles.length >= 2) {
    await toggles[1].tap() // 英音/美音 之外第一组? 先按顺序打日志即可
    await sleep(1200)
  }

  // 保存
  const save = await page.$('.btn-save')
  if (save) {
    await save.tap()
    await sleep(2000)
  }

  await sleep(1500)
  dumpAndExit(mp, logs)
}

function dumpAndExit(mp, logs) {
  console.log('\n===== CONSOLE DUMP (' + logs.length + ' entries) =====')
  for (const l of logs) console.log(l)
  console.log('===== END =====')
  try { mp.disconnect() } catch (_) {}
  process.exit(0)
}

main().catch(e => { console.error('diag4 failed:', e); process.exit(1) })
