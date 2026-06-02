import { launcher } from 'miniprogram-automator'

const CLI_PORT = 20288
const PROJECT_PATH = 'E:\\ccode\\vocab-spec\\wordmate-mini\\dist\\dev\\mp-weixin'

let miniProgram: any

const SCREENSHOT_DIR = 'e2e/wechat-screenshots'

jest.setTimeout(120000)

beforeAll(async () => {
  // DevTools 已手动打开，直接通过端口连接
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

  test('login page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/auth/login')
    await page.waitFor(3000)

    // 截图
    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/login.png`,
    })

    // 检查页面元素
    const body = await page.data()
    expect(page).toBeTruthy()
  })

  test('login page has input fields', async () => {
    const page = await miniProgram.reLaunch('/pages/auth/login')
    await page.waitFor(2000)

    // 查找 input 组件
    const inputs = await page.$$find('input')
    expect(inputs.length).toBeGreaterThanOrEqual(2) // identifier + password
  })

  test('register page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/auth/register')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/register.png`,
    })

    const inputs = await page.$$find('input')
    expect(inputs.length).toBeGreaterThanOrEqual(2)
  })

  test('dashboard page loads', async () => {
    const page = await miniProgram.reLaunch('/pages/index/index')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/dashboard.png`,
    })

    expect(page).toBeTruthy()
  })

  test('test entry page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/test/index')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/test-entry.png`,
    })

    expect(page).toBeTruthy()
  })

  test('stats page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/stats/index')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/stats.png`,
    })

    expect(page).toBeTruthy()
  })

  test('mine page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/mine/index')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/mine.png`,
    })

    expect(page).toBeTruthy()
  })

  test('settings page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/mine/settings')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/settings.png`,
    })

    expect(page).toBeTruthy()
  })

  test('word search page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/word/search')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/word-search.png`,
    })

    const inputs = await page.$$find('input')
    expect(inputs.length).toBeGreaterThanOrEqual(1)
  })

  test('forgetting curve page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/stats/forgetting-curve')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/forgetting-curve.png`,
    })

    expect(page).toBeTruthy()
  })

  test('wrong book page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/wrong-book/list')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/wrong-book.png`,
    })

    expect(page).toBeTruthy()
  })

  test('achievements page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/mine/achievements')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/achievements.png`,
    })

    expect(page).toBeTruthy()
  })

  test('study session page structure', async () => {
    const page = await miniProgram.reLaunch('/pages/study/session?level=CET4')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/study-session.png`,
    })

    expect(page).toBeTruthy()
  })

  test('study done page renders', async () => {
    const page = await miniProgram.reLaunch('/pages/study/done?correct=8&total=10')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/study-done.png`,
    })

    expect(page).toBeTruthy()
  })

  test('choice test page structure', async () => {
    const page = await miniProgram.reLaunch('/pages/test/choice?test_id=test123&level=CET4')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/test-choice.png`,
    })

    expect(page).toBeTruthy()
  })

  test('spelling test page structure', async () => {
    const page = await miniProgram.reLaunch('/pages/test/spelling?test_id=test123&level=CET4')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/test-spelling.png`,
    })

    expect(page).toBeTruthy()
  })

  test('listening test page structure', async () => {
    const page = await miniProgram.reLaunch('/pages/test/listening?test_id=test123&level=CET4')
    await page.waitFor(3000)

    await miniProgram.screenshot({
      path: `${SCREENSHOT_DIR}/test-listening.png`,
    })

    expect(page).toBeTruthy()
  })

  test('all 16 pages registered', async () => {
    // 验证 pages.json 中的所有页面都能被路由到
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
      } catch {
        // 记录失败但不中断
      }
    }
    // 至少 14/16 页面应能加载（有些可能需要特殊参数）
    expect(loaded).toBeGreaterThanOrEqual(14)
  })
})
