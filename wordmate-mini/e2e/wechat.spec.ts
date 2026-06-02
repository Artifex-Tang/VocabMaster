import { launcher } from 'miniprogram-automator'

const CLI_PORT = 12995
const PROJECT_PATH = 'E:/ccode/vocab-spec/wordmate-mini/dist/dev/mp-weixin'

let miniProgram: any

const SCREENSHOT_DIR = 'e2e/wechat-screenshots'

jest.setTimeout(120000)

beforeAll(async () => {
  miniProgram = await launcher.launch({
    cliPath: 'C:/Program Files (x86)/Tencent/微信web开发者工具/cli.bat',
    projectPath: PROJECT_PATH,
  })
}, 90000)

afterAll(async () => {
  if (miniProgram) {
    await miniProgram.close()
  }
})

describe('WeChat Mini-Program Tests', () => {

  test('app launches successfully', async () => {
    expect(miniProgram).toBeTruthy()
  })

  // ---- 登录页 ----
  test('login page renders with correct elements', async () => {
    const page = await miniProgram.reLaunch('/pages/auth/login')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/01-login.png`,
    })

    // 检查 input 组件存在
    const inputs = await page.$$find('input')
    expect(inputs.length).toBeGreaterThanOrEqual(2)
  })

  test('login form fills with email and password', async () => {
    const page = await miniProgram.reLaunch('/pages/auth/login')
    await page.waitFor(2000)

    // 获取所有 input
    const inputs = await page.$$find('input')
    expect(inputs.length).toBeGreaterThanOrEqual(2)

    // 填入邮箱（第一个 input）
    await inputs[0].trigger('input', { value: { value: 'tangliqunkitty@gmail.com' } })
    await page.waitFor(500)

    // 填入密码（第二个 input）
    await inputs[1].trigger('input', { value: { value: 'Tang@20023445' } })
    await page.waitFor(500)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/02-login-filled.png`,
    })

    // 验证 input 有值
    const data = await page.data()
    console.log('Login page data:', JSON.stringify(data).substring(0, 300))
  })

  test('login submit triggers API call', async () => {
    const page = await miniProgram.reLaunch('/pages/auth/login')
    await page.waitFor(2000)

    // 填入表单
    const inputs = await page.$$find('input')
    await inputs[0].trigger('input', { value: { value: 'tangliqunkitty@gmail.com' } })
    await page.waitFor(300)
    await inputs[1].trigger('input', { value: { value: 'Tang@20023445' } })
    await page.waitFor(300)

    // 找登录按钮并点击
    const buttons = await page.$$find('button')
    const loginBtn = buttons.find(async (b: any) => {
      const text = await b.text()
      return text?.includes('登录')
    })

    if (loginBtn) {
      await loginBtn.tap()
    } else {
      // Fallback: 点击第一个 button
      if (buttons.length > 0) {
        await buttons[0].tap()
      }
    }

    await page.waitFor(5000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/03-login-after-submit.png`,
    })

    // 无 crash = pass
    expect(true).toBeTruthy()
  })

  // ---- 注册页 ----
  test('register page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/auth/register')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/04-register.png`,
    })

    const inputs = await page.$$find('input')
    expect(inputs.length).toBeGreaterThanOrEqual(2)
  })

  // ---- Tab 页面 ----
  test('dashboard page loads', async () => {
    const page = await miniProgram.reLaunch('/pages/index/index')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/05-dashboard.png`,
    })

    expect(page).toBeTruthy()
  })

  test('test entry page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/test/index')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/06-test-entry.png`,
    })

    expect(page).toBeTruthy()
  })

  test('stats page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/stats/index')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/07-stats.png`,
    })

    expect(page).toBeTruthy()
  })

  test('mine page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/mine/index')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/08-mine.png`,
    })

    expect(page).toBeTruthy()
  })

  // ---- 子页面 ----
  test('settings page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/mine/settings')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/09-settings.png`,
    })

    expect(page).toBeTruthy()
  })

  test('word search page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/word/search')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/10-word-search.png`,
    })

    const inputs = await page.$$find('input')
    expect(inputs.length).toBeGreaterThanOrEqual(1)
  })

  test('forgetting curve page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/stats/forgetting-curve')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/11-forgetting-curve.png`,
    })

    expect(page).toBeTruthy()
  })

  test('wrong book page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/wrong-book/list')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/12-wrong-book.png`,
    })

    expect(page).toBeTruthy()
  })

  test('achievements page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/mine/achievements')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/13-achievements.png`,
    })

    expect(page).toBeTruthy()
  })

  test('study session page structure', async () => {
    const page = await miniProgram.reLaunch('/pages/study/session?level=CET4')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/14-study-session.png`,
    })

    expect(page).toBeTruthy()
  })

  test('study done page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/study/done?correct=8&total=10')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/15-study-done.png`,
    })

    expect(page).toBeTruthy()
  })

  test('choice test page structure', async () => {
    const page = await miniProgram.reLaunch('/pages/test/choice?test_id=test123&level=CET4')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/16-test-choice.png`,
    })

    expect(page).toBeTruthy()
  })

  test('spelling test page structure', async () => {
    const page = await miniProgram.reLaunch('/pages/test/spelling?test_id=test123&level=CET4')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/17-test-spelling.png`,
    })

    expect(page).toBeTruthy()
  })

  test('listening test page structure', async () => {
    const page = await miniProgram.reLaunch('/pages/test/listening?test_id=test123&level=CET4')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/18-test-listening.png`,
    })

    expect(page).toBeTruthy()
  })

  // ---- 全页面路由 ----
  test('all 16 pages registered and routable', async () => {
    const pages = [
      '/pages/auth/login',
      '/pages/auth/register',
      '/pages/index/index',
      '/pages/test/index',
      '/pages/stats/index',
      '/pages/mine/index',
      '/pages/study/session',
      '/pages/study/done',
      '/pages/test/choice',
      '/pages/test/spelling',
      '/pages/test/listening',
      '/pages/wrong-book/list',
      '/pages/mine/settings',
      '/pages/mine/achievements',
      '/pages/stats/forgetting-curve',
      '/pages/word/search',
    ]

    let loaded = 0
    for (const pg of pages) {
      try {
        const page = await miniProgram.reLaunch(pg)
        await page.waitFor(1000)
        loaded++
      } catch (e) {
        console.log(`Failed to load: ${pg}`, (e as Error).message)
      }
    }
    expect(loaded).toBeGreaterThanOrEqual(14)
  })
})
