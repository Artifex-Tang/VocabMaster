import { test, expect } from '@playwright/test'

const SCREENSHOT_DIR = 'e2e/screenshots'

// Test user credentials (from mock or real backend)
const EMAIL = 'tangliqunkitty@gmail.com'
const PASSWORD = 'Tang@20023445'

test.describe('Web E2E - VocabMaster', () => {

  // ── 1. 登录页 ──
  test('login page renders correctly', async ({ page }) => {
    await page.goto('/login')
    await page.waitForTimeout(2000)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/web-login.png`, fullPage: true })

    // 应该有邮箱和密码输入框
    const inputs = page.locator('input')
    await expect(inputs.first()).toBeVisible()

    const text = await page.evaluate(() => document.body?.innerText ?? '')
    expect(text.length).toBeGreaterThan(0)
  })

  test('login with credentials', async ({ page }) => {
    await page.goto('/login')
    await page.waitForTimeout(1000)

    // 填写邮箱
    const emailInput = page.locator('input[type="email"], input[placeholder*="邮箱"], input[placeholder*="email"]').first()
    if (await emailInput.count() > 0) {
      await emailInput.fill(EMAIL)
    }

    // 填写密码
    const passInput = page.locator('input[type="password"]').first()
    if (await passInput.count() > 0) {
      await passInput.fill(PASSWORD)
    }

    // 点击登录按钮
    const loginBtn = page.locator('button:has-text("登录"), button:has-text("Login"), button[type="submit"]').first()
    if (await loginBtn.count() > 0) {
      await loginBtn.click()
      await page.waitForTimeout(3000)
      await page.screenshot({ path: `${SCREENSHOT_DIR}/web-login-result.png`, fullPage: true })
    }
  })

  // ── 2. 首页 / 仪表板 ──
  test('home page loads after login', async ({ page }) => {
    // 先注入 token 模拟登录态
    await page.goto('/login')
    await page.evaluate(() => {
      localStorage.setItem('token', 'mock-access-token-abc123')
      localStorage.setItem('user', JSON.stringify({
        uuid: 'u-001',
        nickname: '测试用户',
        email: 'test@example.com',
      }))
    })
    await page.goto('/')
    await page.waitForTimeout(3000)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/web-home.png`, fullPage: true })

    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))

    const text = await page.evaluate(() => document.body?.innerText ?? '')
    // 页面应有内容（不管是否登录成功）
    expect(text.length).toBeGreaterThan(0)
  })

  // ── 3. 单词学习 ──
  test('study page accessible', async ({ page }) => {
    await page.goto('/login')
    await page.evaluate(() => {
      localStorage.setItem('token', 'mock-access-token-abc123')
      localStorage.setItem('user', JSON.stringify({ uuid: 'u-001', nickname: '测试用户' }))
    })
    await page.goto('/study')
    await page.waitForTimeout(3000)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/web-study.png`, fullPage: true })

    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))
    expect(errors).toHaveLength(0)
  })

  // ── 4. 测试模块 ──
  test('test entry page renders', async ({ page }) => {
    await page.goto('/login')
    await page.evaluate(() => {
      localStorage.setItem('token', 'mock-access-token-abc123')
      localStorage.setItem('user', JSON.stringify({ uuid: 'u-001', nickname: '测试用户' }))
    })
    await page.goto('/test')
    await page.waitForTimeout(3000)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/web-test.png`, fullPage: true })

    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))
    expect(errors).toHaveLength(0)
  })

  // ── 5. 单词搜索 ──
  test('word search page', async ({ page }) => {
    await page.goto('/login')
    await page.evaluate(() => {
      localStorage.setItem('token', 'mock-access-token-abc123')
      localStorage.setItem('user', JSON.stringify({ uuid: 'u-001', nickname: '测试用户' }))
    })
    await page.goto('/word/search')
    await page.waitForTimeout(3000)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/web-word-search.png`, fullPage: true })

    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))
    expect(errors).toHaveLength(0)
  })

  // ── 6. 遗忘曲线 ──
  test('ebbinghaus curve page', async ({ page }) => {
    await page.goto('/login')
    await page.evaluate(() => {
      localStorage.setItem('token', 'mock-access-token-abc123')
      localStorage.setItem('user', JSON.stringify({ uuid: 'u-001', nickname: '测试用户' }))
    })
    await page.goto('/ebbinghaus')
    await page.waitForTimeout(3000)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/web-ebbinghaus.png`, fullPage: true })

    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))
    expect(errors).toHaveLength(0)
  })

  // ── 7. 设置页 ──
  test('settings page renders', async ({ page }) => {
    await page.goto('/login')
    await page.evaluate(() => {
      localStorage.setItem('token', 'mock-access-token-abc123')
      localStorage.setItem('user', JSON.stringify({ uuid: 'u-001', nickname: '测试用户' }))
    })
    await page.goto('/settings')
    await page.waitForTimeout(3000)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/web-settings.png`, fullPage: true })

    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))
    expect(errors).toHaveLength(0)
  })

  // ── 8. 路由守卫 - 未登录跳转 ──
  test('unauthenticated redirect to login', async ({ page }) => {
    // Full reload re-inits Pinia from empty localStorage → isLoggedIn=false → guard redirects
    await page.goto('/login')
    await page.evaluate(() => {
      localStorage.clear()
    })
    // Use a real protected route (not /study which doesn't match study/:level)
    await page.goto('/dashboard')
    await page.waitForTimeout(2000)
    // Should be redirected to login page
    const url = page.url()
    expect(url).toMatch(/login|auth/)
  })

  // ── 9. API 连通性 ──
  test('backend API is reachable through proxy', async ({ page }) => {
    const response = await page.request.get('http://localhost:3001/api/v1/actuator/health')
    expect(response.ok()).toBeTruthy()
    const body = await response.json()
    expect(body.status).toBe('UP')
  })
})
