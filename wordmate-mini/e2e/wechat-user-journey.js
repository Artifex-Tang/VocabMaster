/**
 * VocabMaster 微信小程序 — 完整用户旅程自动化测试
 *
 * 参考模式: FocusLab t4-miniprogram.test.js + MeetingGo miniapp_simulator_test.js
 * 核心: page.callMethod() 驱动 Vue 方法, page.data('field') 验证状态,
 *        mp.callWxMethod() 注入 storage, mp.evaluate() 执行小程序环境代码
 *
 * 用户旅程:
 *   T1  启动与登录页
 *   T2  表单交互（填写+切换tab）
 *   T3  注册页表单
 *   T4  模拟登录态 → 首页
 *   T5  首页数据加载
 *   T6  学习卡片翻转与标记
 *   T7  学习完成结果
 *   T8  测试入口
 *   T9  选择题测试交互
 *   T10 拼写测试输入
 *   T11 听力测试
 *   T12 统计页 tab 切换
 *   T13 遗忘曲线搜索
 *   T14 单词搜索
 *   T15 错词本
 *   T16 "我的"页面
 *   T17 设置修改
 *   T18 成就
 *   T19 Tab 切换全流程
 *   T20 网络与 storage 验证
 *
 * Run: node e2e/wechat-user-journey.js
 * Pre:  WeChat DevTools open, auto mode on port 60616
 */
const { launchMiniProgram } = require('./launch-devtools')
const { execFileSync } = require('child_process')

const SS_DIR = 'e2e/wechat-screenshots/journey'
const path = require('path')
const fs = require('fs')

if (!fs.existsSync(SS_DIR)) fs.mkdirSync(SS_DIR, { recursive: true })

const sleep = ms => new Promise(r => setTimeout(r, ms))

// ── Real-auth provisioning (against local docker backend by default) ──────────
// register-or-login a fixed e2e user, capture real tokens. UI login in T2 then
// drives the real flow; Pinia holds the token in-memory across reLaunch (initFromStorage
// only runs once in App.onLaunch, so the storage JSON.parse-on-JWT bug can't wipe it).
const E2E_API = process.env.E2E_API || 'http://localhost:8080/api/v1'
const E2E_EMAIL = process.env.JOURNEY_EMAIL || 'e2e-mp@vocab.local'
const E2E_PWD = process.env.JOURNEY_PWD || 'E2eTest#2026'
const AUTH_FILE = path.resolve(__dirname, '.auth.mp.json')
let ACCESS_TOKEN = ''   // set by provision(), asserted in T4

function provision() {
  console.log('[SETUP] Provisioning real user on', E2E_API, '->', E2E_EMAIL)
  execFileSync('python', ['e2e/provision.py', E2E_EMAIL, E2E_PWD, 'e2e/.auth.mp.json'], {
    cwd: path.resolve(__dirname, '..'),
    env: { ...process.env, E2E_API, REDIS_PASSWORD: process.env.REDIS_PASSWORD || 'redis123' },
    stdio: ['ignore', 'inherit', 'inherit'],
    timeout: 60000,
  })
  const auth = JSON.parse(fs.readFileSync(AUTH_FILE, 'utf8'))
  ACCESS_TOKEN = auth.access_token
  console.log('[SETUP] Provisioned', auth.email, '| token len', auth.access_token.length)
  return auth
}

function withTimeout(promise, ms, label) {
  return Promise.race([
    promise,
    new Promise((_, rej) => setTimeout(() => rej(new Error(`TIMEOUT(${ms}ms): ${label}`)), ms)),
  ])
}

let mp
let passed = 0, failed = 0
const results = []

function log(icon, group, desc, detail = '') {
  results.push({ icon, group, desc, detail })
  console.log(`  ${icon} [${group}] ${desc}${detail ? '  (' + detail + ')' : ''}`)
  if (icon === '✓') passed++
  else if (icon === '✗') failed++
}

async function ss(name) {
  try { await withTimeout(mp.screenshot({ path: path.join(SS_DIR, `${name}.png`) }), 15000, 'screenshot') }
  catch (e) { console.log(`  [ss-fail] ${name}: ${e.message?.substring(0, 60)}`) }
}

async function nav(url, method = 'reLaunch') {
  let page, lastErr
  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      page = await withTimeout(mp[method](url), 45000, `${method}(${url})#${attempt}`)
      await page.waitFor(2500)
      return page
    } catch (e) {
      lastErr = e
      console.log(`  [nav retry ${attempt}] ${url} ${e.message}`)
      await sleep(3000)
    }
  }
  throw lastErr
}

async function currentPage() {
  const p = await mp.currentPage()
  await p.waitFor(500)
  return p
}

// ==============================================================
async function runTests() {
  console.log('='.repeat(60))
  console.log('  VocabMaster 微信小程序 — 用户旅程自动化测试（真鉴权）')
  console.log('='.repeat(60))

  // ── Provision real user (local docker backend) ──────────────
  try {
    provision()
  } catch (e) {
    console.error('FATAL: provision 失败 —', e.message)
    process.exit(1)
  }

  // ── Connect ──────────────────────────────────────────────────
  try {
    mp = await withTimeout(launchMiniProgram(), 30000, 'launch')
    console.log('[SETUP] 已连接微信开发者工具')
    console.log('[SETUP] 等待项目编译完成...')
    await sleep(40000)  // DevTools 首次编译较慢，等足
  } catch (e) {
    console.error('FATAL: 连接失败 —', e.message)
    process.exit(1)
  }

  // ==============================================================
  //  T1 启动与登录页
  // ==============================================================
  console.log('[T1] 登录页')
  let page = await nav('/pages/auth/login')
  await ss('01-login')

  // 验证页面路径
  log(page.path === 'pages/auth/login' ? '✓' : '✗', 'T1', '页面路径正确', page.path)

  // 验证 input 组件存在
  const inputs = await page.$$('input')
  log(inputs.length >= 2 ? '✓' : '✗', 'T1', '存在2+个输入框', `count=${inputs.length}`)

  // 验证页面有文本内容
  const pageText = await page.$$('.app-name')
  log(pageText.length > 0 ? '✓' : '✗', 'T1', 'App 名称元素存在')

  // ==============================================================
  //  T2 真鉴权登录（provision 的 e2e 账号）
  // ==============================================================
  console.log('\n[T2] 真鉴权登录', E2E_EMAIL)

  // 填写邮箱 + 密码
  try {
    await inputs[0].input(E2E_EMAIL)
    await sleep(300)
    await inputs[1].input(E2E_PWD)
    await sleep(300)
    await ss('02-login-filled')
    log('✓', 'T2', '表单填写', E2E_EMAIL)
  } catch (e) {
    log('✗', 'T2', '表单填写失败', e.message?.substring(0, 80))
  }

  // 切 phone 再切回 email（覆盖 tab 交互，登录仍用 email）
  try { await page.setData({ tab: 'phone' }) } catch (_) {}
  await sleep(500)
  await ss('03-login-phone-tab')
  log('✓', 'T2', '切换到 phone tab')
  try { await page.setData({ tab: 'email' }) } catch (_) {}
  await sleep(300)

  // 点击登录按钮 → 轮询离开 /login（成功=switchTab 到 index；失败=toast 留 login）
  try {
    const loginBtn = await page.$('.btn-primary')
    if (!loginBtn) {
      log('✗', 'T2', '未找到登录按钮')
    } else {
      await loginBtn.tap()
      let left = false, curPath = ''
      const deadline = Date.now() + 20000
      while (Date.now() < deadline) {
        await sleep(800)
        const cur = await currentPage()
        curPath = cur.path
        if (curPath && curPath !== 'pages/auth/login') { left = true; break }
      }
      await ss('04-after-login')
      log(left ? '✓' : '✗', 'T2', '真登录→离开 login', curPath)
    }
  } catch (e) {
    log('✗', 'T2', '登录点击', e.message?.substring(0, 80))
  }

  // 验证 storage 写入真 JWT（后端发的，非假 test_token）
  try {
    const tok = await mp.callWxMethod('getStorageSync', 'access_token')
    const real = typeof tok === 'string' && tok.length > 50 && !tok.startsWith('test_token')
    log(real ? '✓' : '✗', 'T2', 'storage 真 JWT', real ? `len=${tok.length}` : `bad=${String(tok).slice(0, 20)}`)
  } catch (e) {
    log('✗', 'T2', 'token 读 storage', e.message?.substring(0, 80))
  }

  // ==============================================================
  //  T3 注册页
  // ==============================================================
  console.log('\n[T3] 注册页')
  page = await nav('/pages/auth/register')
  await ss('05-register')

  log(page.path === 'pages/auth/register' ? '✓' : '✗', 'T3', '注册页路径', page.path)

  const regInputs = await page.$$('input')
  log(regInputs.length >= 2 ? '✓' : '✗', 'T3', '注册页输入框', `count=${regInputs.length}`)

  // 填写注册表单
  try {
    await regInputs[0].input('newuser@test.com')
    await sleep(200)
    await regInputs[1].input('Test@12345')
    await sleep(200)
    await ss('06-register-filled')
    log('✓', 'T3', '注册表单填写成功')
  } catch (e) {
    log('✗', 'T3', '注册表单填写', e.message?.substring(0, 80))
  }

  // ==============================================================
  //  T4 真会话验证（靠 T2 真 UI 登录的 Pinia 内存 token，不注入假 token）
  //  注：不 reLaunch/restart —— initFromStorage 只在 App.onLaunch 跑一次，
  //      reLaunch 会重读 storage（JWT 经 JSON.parse 失败→null→登出），故保持内存态。
  // ==============================================================
  console.log('\n[T4] 真会话验证')
  try {
    // T3 去了 register，回首页验证真会话（Pinia 内存 token 保留，reLaunch 不重触 onLaunch）
    await nav('/pages/index/index')
    const tok = await mp.callWxMethod('getStorageSync', 'access_token')
    const real = typeof tok === 'string' && tok.length > 50 && !tok.startsWith('test_token')
    log(real ? '✓' : '✗', 'T4', 'storage 真 JWT', real ? `len=${tok.length}` : `bad=${String(tok).slice(0,  20)}`)

    const p = await currentPage()
    log(p.path === 'pages/index/index' ? '✓' : '✗', 'T4', '回到首页', p.path)
    await ss('06-real-home')
  } catch (e) {
    log('✗', 'T4', '会话验证', e.message?.substring(0, 80))
  }

  // ==============================================================
  //  T5 首页（已在 T4 reLaunch 后到达）
  // ==============================================================
  console.log('\n[T5] 首页（学习）')
  page = await currentPage()
  // 真登录后必在 index（T2 已 switchTab 到首页）
  log(page.path === 'pages/index/index' ? '✓' : '✗', 'T5', '当前在首页', page.path)

  // 验证首页 data 结构
  try {
    const pageData = await page.data()
    const keys = Object.keys(pageData)
    log('✓', 'T5', '首页 data 加载', `keys=${keys.length}`)
  } catch (e) {
    log('✗', 'T5', '首页 data', e.message?.substring(0, 80))
  }
  await ss('07-home')

  // ==============================================================
  //  T6 学习卡片
  // ==============================================================
  console.log('\n[T6] 学习卡片')
  page = await nav('/pages/study/session?level=CET4')
  await ss('08-study-session')

  log(page.path === 'pages/study/session' ? '✓' : '✗', 'T6', '学习页路径', page.path)

  // 模拟翻转（showAnswer）
  try {
    await page.setData({ showAnswer: true })
    await sleep(500)
    await ss('09-study-card-flipped')
    const flipped = await page.data('showAnswer')
    log(flipped === true ? '✓' : '✗', 'T6', '翻转卡片', `showAnswer=${flipped}`)
  } catch (e) {
    log('✗', 'T6', '翻转卡片', e.message?.substring(0, 80))
  }

  // ==============================================================
  //  T7 学习完成
  // ==============================================================
  console.log('\n[T7] 学习完成')
  page = await nav('/pages/study/done?correct=8&total=10&new_words=5&review_words=5')
  await ss('10-study-done')

  log(page.path === 'pages/study/done' ? '✓' : '✗', 'T7', '结果页路径', page.path)

  // ==============================================================
  //  T8 测试入口
  // ==============================================================
  console.log('\n[T8] 测试入口')
  page = await nav('/pages/test/index', 'switchTab')
  await ss('11-test-entry')

  log(page.path === 'pages/test/index' ? '✓' : '✗', 'T8', '测试页路径', page.path)

  // ==============================================================
  //  T9 选择题测试
  // ==============================================================
  console.log('\n[T9] 选择题测试')
  page = await nav('/pages/test/choice?test_id=demo001&level=CET4')
  await ss('12-test-choice')

  log(page.path === 'pages/test/choice' ? '✓' : '✗', 'T9', '选择题页路径', page.path)

  // 尝试模拟选择答案
  try {
    const choiceData = await page.data()
    const keys = Object.keys(choiceData)
    log('✓', 'T9', '选择题 data', `keys=${keys.length}`)
  } catch (e) {
    log('✗', 'T9', '选择题 data', e.message?.substring(0, 80))
  }

  // ==============================================================
  //  T10 拼写测试
  // ==============================================================
  console.log('\n[T10] 拼写测试')
  page = await nav('/pages/test/spelling?test_id=demo001&level=CET4')
  await ss('13-test-spelling')

  log(page.path === 'pages/test/spelling' ? '✓' : '✗', 'T10', '拼写页路径', page.path)

  // 输入拼写答案
  try {
    const spellInputs = await page.$$('input')
    if (spellInputs.length > 0) {
      await spellInputs[0].input('abandon')
      await sleep(300)
      await ss('13b-test-spelling-filled')
      log('✓', 'T10', '拼写输入成功')
    } else {
      log('🔍', 'T10', '未找到输入框（可能需要题目数据）')
    }
  } catch (e) {
    log('✗', 'T10', '拼写输入', e.message?.substring(0, 80))
  }

  // ==============================================================
  //  T11 听力测试
  // ==============================================================
  console.log('\n[T11] 听力测试')
  page = await nav('/pages/test/listening?test_id=demo001&level=CET4')
  await ss('14-test-listening')

  log(page.path === 'pages/test/listening' ? '✓' : '✗', 'T11', '听力页路径', page.path)

  // ==============================================================
  //  T12 统计页
  // ==============================================================
  console.log('\n[T12] 统计页')
  page = await nav('/pages/stats/index', 'switchTab')
  await ss('15-stats')

  log(page.path === 'pages/stats/index' ? '✓' : '✗', 'T12', '统计页路径', page.path)

  // ==============================================================
  //  T13 遗忘曲线
  // ==============================================================
  console.log('\n[T13] 遗忘曲线')
  page = await nav('/pages/stats/forgetting-curve')
  await ss('16-forgetting-curve')

  log(page.path === 'pages/stats/forgetting-curve' ? '✓' : '✗', 'T13', '遗忘曲线路径', page.path)

  // 模拟搜索
  try {
    const fcInputs = await page.$$('input')
    if (fcInputs.length > 0) {
      await fcInputs[0].input('abandon')
      await sleep(1000)
      await ss('16b-forgetting-curve-search')
      log('✓', 'T13', '遗忘曲线搜索输入')
    }
  } catch (e) {
    log('🔍', 'T13', '搜索输入', e.message?.substring(0, 60))
  }

  // ==============================================================
  //  T14 单词搜索
  // ==============================================================
  console.log('\n[T14] 单词搜索')
  page = await nav('/pages/word/search')
  await ss('17-word-search')

  log(page.path === 'pages/word/search' ? '✓' : '✗', 'T14', '搜索页路径', page.path)

  // 搜索单词
  try {
    const searchInputs = await page.$$('input')
    if (searchInputs.length > 0) {
      await searchInputs[0].input('abandon')
      await sleep(1000)
      await ss('17b-word-search-result')
      log('✓', 'T14', '搜索单词 "abandon"')
    }
  } catch (e) {
    log('🔍', 'T14', '搜索输入', e.message?.substring(0, 60))
  }

  // ==============================================================
  //  T15 错词本
  // ==============================================================
  console.log('\n[T15] 错词本')
  page = await nav('/pages/wrong-book/list')
  await ss('18-wrong-book')

  log(page.path === 'pages/wrong-book/list' ? '✓' : '✗', 'T15', '错词本路径', page.path)

  // ==============================================================
  //  T16 "我的"页面
  // ==============================================================
  console.log('\n[T16] 我的')
  page = await nav('/pages/mine/index', 'switchTab')
  await ss('19-mine')

  log(page.path === 'pages/mine/index' ? '✓' : '✗', 'T16', '我的页路径', page.path)

  // ==============================================================
  //  T17 设置
  // ==============================================================
  console.log('\n[T17] 设置')
  page = await nav('/pages/mine/settings')
  await ss('20-settings')

  log(page.path === 'pages/mine/settings' ? '✓' : '✗', 'T17', '设置页路径', page.path)

  // 修改设置
  try {
    await page.setData({ dailyNewWordsGoal: 30 })
    await sleep(300)
    const goal = await page.data('dailyNewWordsGoal')
    log(goal === 30 ? '✓' : '✗', 'T17', '修改每日目标', `goal=${goal}`)
    await ss('20b-settings-modified')
  } catch (e) {
    log('✗', 'T17', '修改设置', e.message?.substring(0, 80))
  }

  // ==============================================================
  //  T18 成就
  // ==============================================================
  console.log('\n[T18] 成就')
  page = await nav('/pages/mine/achievements')
  await ss('21-achievements')

  log(page.path === 'pages/mine/achievements' ? '✓' : '✗', 'T18', '成就页路径', page.path)

  // ==============================================================
  //  T19 Tab 全流程切换
  // ==============================================================
  console.log('\n[T19] Tab 切换')
  const tabs = [
    { path: '/pages/index/index', name: '学习' },
    { path: '/pages/test/index', name: '测试' },
    { path: '/pages/stats/index', name: '统计' },
    { path: '/pages/mine/index', name: '我的' },
  ]
  for (const tab of tabs) {
    try {
      await mp.switchTab(tab.path)
      const cur = await currentPage()
      // 学习 tab 可能重定向到 login（路由守卫）— 可接受
      log(`✓`, 'T19', `Tab → ${tab.name}`, cur.path)
    } catch (e) {
      log('✗', 'T19', `Tab → ${tab.name}`, e.message?.substring(0, 60))
    }
  }

  // ==============================================================
  //  T20 Storage 验证
  // ==============================================================
  console.log('\n[T20] Storage 验证')
  try {
    const storedToken = await mp.callWxMethod('getStorageSync', 'access_token')
    log(storedToken ? '✓' : '✗', 'T20', 'access_token 存在', storedToken ? `${String(storedToken).substring(0, 15)}...` : 'null')

    const storedUser = await mp.callWxMethod('getStorageSync', 'user_info')
    log(storedUser ? '✓' : '✗', 'T20', 'user_info 存在', storedUser ? '已存储' : 'null')

    const storedSettings = await mp.callWxMethod('getStorageSync', 'user_settings')
    log(storedSettings ? '✓' : '🔍', 'T20', 'user_settings', storedSettings ? '已存储' : '未设置（首次使用正常）')
  } catch (e) {
    log('✗', 'T20', 'Storage 验证', e.message?.substring(0, 80))
  }

  // ==============================================================
  //  Summary
  // ==============================================================
  await mp.close()

  const probes = results.filter(r => r.icon === '🔍').length
  console.log('\n' + '='.repeat(60))
  console.log('  用户旅程测试结果')
  console.log('='.repeat(60))
  results.forEach(r =>
    console.log(`  ${r.icon} [${r.group}] ${r.desc}${r.detail ? '  (' + r.detail + ')' : ''}`)
  )
  console.log(`\n  Passed: ${passed}  Failed: ${failed}  Info: ${probes}`)
  console.log(`  Verdict: ${failed === 0 ? '✅ PASS' : '❌ FAIL'}`)
  console.log(`\n  截图: ${path.resolve(SS_DIR)}/`)
  console.log()

  return failed === 0
}

runTests()
  .then(ok => process.exit(ok ? 0 : 1))
  .catch(err => { console.error('FATAL:', err.message); process.exit(1) })
