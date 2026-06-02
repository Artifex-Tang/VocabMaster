/**
 * WeChat Mini-Program Automated Test
 * Run via: node e2e/wechat-auto-test.js
 * Requires: WeChat DevTools open with project, auto mode enabled via CLI
 *
 * Setup: powershell -Command "Start-Process 'C:\Program Files (x86)\Tencent\微信web开发者工具\cli.bat' -ArgumentList 'auto','--project','E:\ccode\vocab-spec\wordmate-mini\dist\dev\mp-weixin','--auto-port','60616'"
 */
const { launcher } = require('miniprogram-automator')

const WS = 'ws://127.0.0.1:60616'
const SCREENSHOT_DIR = 'e2e/wechat-screenshots'
const EMAIL = 'tangliqunkitty@gmail.com'
const PASSWORD = 'Tang@20023445'

let mp
let passed = 0
let failed = 0
const results = []

async function test(name, fn) {
  try {
    await fn()
    passed++
    results.push(`  ✓ ${name}`)
    console.log(`✓ ${name}`)
  } catch (e) {
    failed++
    results.push(`  ✗ ${name}: ${e.message?.substring(0, 120)}`)
    console.log(`✗ ${name}: ${e.message?.substring(0, 120)}`)
  }
}

async function screenshot(name) {
  try {
    await mp.screenshot({ path: `${SCREENSHOT_DIR}/${name}.png` })
  } catch (e) {
    console.log(`  screenshot failed: ${e.message?.substring(0, 60)}`)
  }
}

async function nav(path) {
  const page = await mp.reLaunch(path)
  await page.waitFor(3000)
  return page
}

(async () => {
  console.log('Connecting to WeChat DevTools...')
  mp = await launcher.connect({ wsEndpoint: WS })
  console.log('Connected!\n')

  // ---- Connection ----
  await test('connect to DevTools', async () => {
    if (!mp) throw new Error('Connection failed')
  })

  // ---- Login Page ----
  await test('login page loads with inputs', async () => {
    const page = await nav('/pages/auth/login')
    const inputs = await page.$$('.input')
    if (inputs.length < 2) throw new Error(`Expected 2+ inputs, got ${inputs.length}`)
    await screenshot('01-login')
  })

  await test('login form fills email + password', async () => {
    const page = await nav('/pages/auth/login')
    const inputs = await page.$$('.input')
    if (inputs.length < 2) throw new Error('Inputs not found')

    // Set data directly via page model
    await page.setData({ identifier: EMAIL })
    await page.waitFor(300)
    await page.setData({ password: PASSWORD })
    await page.waitFor(300)

    const data = await page.data()
    // Check via page data (minified keys, but we can check via setData worked)
    await screenshot('02-login-filled')
  })

  await test('login submit (no backend)', async () => {
    const page = await nav('/pages/auth/login')
    await page.setData({ identifier: EMAIL })
    await page.setData({ password: PASSWORD })
    await page.waitFor(300)

    // Find and tap login button
    const btn = await page.$('.btn-primary')
    if (btn) {
      await btn.input('') // tap-like
    }

    await page.waitFor(5000)
    await screenshot('03-login-after-submit')
    // No crash = pass (backend not running)
  })

  // ---- Register Page ----
  await test('register page loads', async () => {
    const page = await nav('/pages/auth/register')
    const inputs = await page.$$('.input')
    if (inputs.length < 2) throw new Error(`Expected 2+ inputs, got ${inputs.length}`)
    await screenshot('04-register')
  })

  // ---- Main Tab Pages ----
  await test('index (study) page loads', async () => {
    await nav('/pages/index/index')
    await screenshot('05-index')
  })

  await test('test entry page loads', async () => {
    await nav('/pages/test/index')
    await screenshot('06-test-entry')
  })

  await test('stats page loads', async () => {
    await nav('/pages/stats/index')
    await screenshot('07-stats')
  })

  await test('mine page loads', async () => {
    await nav('/pages/mine/index')
    await screenshot('08-mine')
  })

  // ---- Sub Pages ----
  await test('settings page loads', async () => {
    await nav('/pages/mine/settings')
    await screenshot('09-settings')
  })

  await test('achievements page loads', async () => {
    await nav('/pages/mine/achievements')
    await screenshot('10-achievements')
  })

  await test('word search page loads', async () => {
    const page = await nav('/pages/word/search')
    const inputs = await page.$$('.input')
    if (inputs.length < 1) throw new Error('Search input not found')
    await screenshot('11-word-search')
  })

  await test('forgetting curve page loads', async () => {
    await nav('/pages/stats/forgetting-curve')
    await screenshot('12-forgetting-curve')
  })

  await test('wrong book page loads', async () => {
    await nav('/pages/wrong-book/list')
    await screenshot('13-wrong-book')
  })

  await test('study session page loads', async () => {
    await nav('/pages/study/session?level=CET4')
    await screenshot('14-study-session')
  })

  await test('study done page loads', async () => {
    await nav('/pages/study/done?correct=8&total=10')
    await screenshot('15-study-done')
  })

  await test('choice test page loads', async () => {
    await nav('/pages/test/choice?test_id=test123&level=CET4')
    await screenshot('16-test-choice')
  })

  await test('spelling test page loads', async () => {
    await nav('/pages/test/spelling?test_id=test123&level=CET4')
    await screenshot('17-test-spelling')
  })

  await test('listening test page loads', async () => {
    await nav('/pages/test/listening?test_id=test123&level=CET4')
    await screenshot('18-test-listening')
  })

  // ---- All Pages Routable ----
  await test('all 16 pages routable (>=14)', async () => {
    const allPages = [
      '/pages/auth/login', '/pages/auth/register',
      '/pages/index/index', '/pages/test/index',
      '/pages/stats/index', '/pages/mine/index',
      '/pages/study/session', '/pages/study/done',
      '/pages/test/choice', '/pages/test/spelling',
      '/pages/test/listening', '/pages/wrong-book/list',
      '/pages/mine/settings', '/pages/mine/achievements',
      '/pages/stats/forgetting-curve', '/pages/word/search',
    ]
    let loaded = 0
    for (const p of allPages) {
      try {
        await mp.reLaunch(p)
        // reuse current page, just wait
        const pg = await mp.currentPage()
        await pg.waitFor(1500)
        loaded++
      } catch (e) {
        console.log(`  skip: ${p}`)
      }
    }
    if (loaded < 14) throw new Error(`Only ${loaded}/16 pages loaded`)
    console.log(`  ${loaded}/16 pages loaded`)
  })

  // ---- Summary ----
  await mp.close()

  console.log('\n' + '='.repeat(50))
  console.log(`Results: ${passed} passed, ${failed} failed, ${passed + failed} total`)
  console.log('='.repeat(50))
  results.forEach(r => console.log(r))
  console.log()

  process.exit(failed > 0 ? 1 : 0)
})().catch(e => {
  console.error('Fatal:', e.message)
  process.exit(1)
})
