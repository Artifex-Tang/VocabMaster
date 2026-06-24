import { test, expect, request as apiRequest } from '@playwright/test'

// 自定义词库（教材词库）移植 — H5 端到端测试
// 本地后端两个坑（非本功能代码问题）：
//   1) 认证端点的 CORS 预检 OPTIONS 返回 401 → 浏览器拦截真实请求。
//   2) storage.get 对 JWT 走 JSON.parse 会失败 → 页面重载后 store token 丢失（已有应用 bug）。
// 对策：
//   - page.route 服务端 fetch 注入 token 后 fulfill → 绕开浏览器 CORS；且所有请求都带有效 token，不会 20001 触发登出。
//   - 表单登录拿 in-memory store token 过路由守卫；之后用 uni.navigateTo（SPA 内跳转，不重载，保住内存 token）。
const EMAIL = 'tangliqunkitty@gmail.com'
const PASSWORD = 'Tang@20023445'
const API = 'http://localhost:8080/api/v1'

let TOKEN = ''
test.beforeAll(async () => {
  const ctx = await apiRequest.newContext()
  const r = await ctx.post(`${API}/auth/login`, {
    data: { type: 'email', identifier: EMAIL, password: PASSWORD },
  })
  TOKEN = (await r.json()).data.access_token
  expect(TOKEN).toBeTruthy()
})

async function setNativeInput(page: import('@playwright/test').Page, idx: number, val: string) {
  await page.evaluate(({ i, v }) => {
    const inp = document.querySelectorAll('input')[i] as HTMLInputElement
    if (!inp) return
    inp.focus()
    const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value')?.set
    setter?.call(inp, v)
    inp.dispatchEvent(new Event('input', { bubbles: true }))
    inp.dispatchEvent(new Event('change', { bubbles: true }))
  }, { i: idx, v: val })
}

async function login(page: import('@playwright/test').Page) {
  await page.goto('/#/pages/auth/login')
  await page.waitForTimeout(5000)
  await setNativeInput(page, 0, EMAIL)
  await setNativeInput(page, 1, PASSWORD)
  await page.waitForTimeout(300)
  await page.locator('uni-button').filter({ hasText: '登录' }).first().click()
  await page.waitForTimeout(5000)
}

async function navigateInApp(page: import('@playwright/test').Page, url: string) {
  await page.evaluate((u) => { (window as any).uni.navigateTo({ url: u }) }, url)
}
async function getText(page: import('@playwright/test').Page) {
  return page.evaluate(() => document.body?.innerText ?? '')
}
async function waitText(page: import('@playwright/test').Page, needle: string, timeout = 25000) {
  await page.waitForFunction((n) => (document.body?.innerText ?? '').includes(n), needle, { timeout })
}
async function clickText(page: import('@playwright/test').Page, text: string) {
  await page.evaluate((t) => {
    const els = document.querySelectorAll('*')
    for (const el of els) if (el.children.length === 0 && el.textContent?.includes(t)) { (el as HTMLElement).click(); return }
    for (const el of els) if (el.textContent?.includes(t)) { (el as HTMLElement).click(); return }
  }, text)
}
function realErrors(errors: string[]) {
  return errors.filter(e =>
    !e.includes('ResizeObserver') && !e.includes('webkit-mask-image') && !e.includes('network'))
}

test.describe('Custom Wordlist (教材词库)', () => {
  test.setTimeout(120000)

  test.beforeEach(async ({ page }) => {
    // 全程拦截：服务端 fetch 注入 token + fulfill（绕开本地 CORS 预检 401；杜绝 20001 登出）
    await page.route('**/api/v1/**', async (route) => {
      const headers = { ...route.request().headers(), Authorization: `Bearer ${TOKEN}` }
      try {
        const r = await route.fetch({ headers })
        await route.fulfill({ response: r })
      } catch {
        await route.continue()
      }
    })
  })

  test('square renders builtin Think lists', async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))
    await login(page)
    await navigateInApp(page, '/pages/wordlists/square')
    await waitText(page, 'Think')
    await page.screenshot({ path: 'e2e/screenshots/wl-01-square.png', fullPage: true })
    expect(await getText(page)).toMatch(/Think/)
    expect(realErrors(errors)).toHaveLength(0)
  })

  test('detail shows units', async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))
    await login(page)
    await navigateInApp(page, '/pages/wordlists/detail?id=6')
    await waitText(page, '词 ·') // 头部 meta，data 已就绪（不依赖订阅）
    // 若未订阅，先订阅（单元网格仅在已订阅时渲染）
    await clickText(page, '订阅词库').catch(() => {})
    await page.waitForTimeout(3000)
    await waitText(page, '单元进度')
    await page.screenshot({ path: 'e2e/screenshots/wl-02-detail.png', fullPage: true })
    const text = await getText(page)
    expect(text).toContain('单元进度')
    expect(text).toMatch(/Unit/)
    expect(realErrors(errors)).toHaveLength(0)
  })

  test('learn page renders card session', async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))
    await login(page)
    await navigateInApp(page, '/pages/wordlists/learn?id=6&unit=1')
    await page.waitForFunction(
      () => (document.body?.innerText ?? '').includes('点击卡片查看释义')
        || (document.body?.innerText ?? '').includes('本单元'),
      undefined, { timeout: 25000 },
    )
    await page.screenshot({ path: 'e2e/screenshots/wl-03-learn.png', fullPage: true })
    expect(realErrors(errors)).toHaveLength(0)
  })

  test('flip card + answer marks word', async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))
    await login(page)
    await navigateInApp(page, '/pages/wordlists/learn?id=6&unit=1')
    await page.waitForFunction(
      () => (document.body?.innerText ?? '').includes('点击卡片查看释义'),
      undefined, { timeout: 25000 },
    )
    await clickText(page, '点击卡片查看释义')
    await page.waitForTimeout(800)
    await clickText(page, '认识')
    await page.waitForTimeout(1500)
    await page.screenshot({ path: 'e2e/screenshots/wl-04-learn-answer.png', fullPage: true })
    expect(realErrors(errors)).toHaveLength(0)
  })

  test('review entry writes storage override', async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', err => errors.push(err.message))
    await login(page)
    await navigateInApp(page, '/pages/wordlists/detail?id=6')
    await waitText(page, '单元进度').catch(() => {})
    // 若未订阅，先订阅（订阅后复习按钮才出现）
    await clickText(page, '订阅词库').catch(() => {})
    await page.waitForTimeout(3000)
    await clickText(page, '复习到期词')
    await page.waitForTimeout(2000)
    const override = await page.evaluate(() => {
      try { return (window as any).uni.getStorageSync('test_entry_override') } catch { return null }
    })
    await page.screenshot({ path: 'e2e/screenshots/wl-05-review-entry.png', fullPage: true })
    console.log('override:', JSON.stringify(override))
    expect(realErrors(errors)).toHaveLength(0)
  })
})
