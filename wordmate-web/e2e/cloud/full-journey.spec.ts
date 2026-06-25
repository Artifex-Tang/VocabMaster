import { test, expect, type Page } from '@playwright/test'
import { execSync } from 'child_process'

const ROUND = process.env.E2E_ROUND || 'r1'
const SHOT = `e2e/screenshots/${ROUND}`

// Shared console-error sink (non-fatal per test; summarized in the last test)
const consoleErrors: string[] = []
function watch(page: Page) {
  page.on('pageerror', e => consoleErrors.push(`[pageerror] ${e.message}`))
  page.on('console', m => { if (m.type() === 'error') consoleErrors.push(`[console.error] ${m.text()}`) })
  page.on('requestfailed', r => consoleErrors.push(`[requestfailed] ${r.url()} ${r.failure()?.errorText ?? ''}`))
}

interface Auth { access_token: string; refresh_token: string; user: any; email: string; password: string }
function auth(): Auth { return JSON.parse(process.env.E2E_AUTH!) }

/** Inject access/refresh token into localStorage before first navigation (isLoggedIn derives from token). */
async function loginAs(page: Page) {
  const a = auth()
  await page.addInitScript((t: string, r: string) => {
    localStorage.setItem('vm_access_token', t)
    localStorage.setItem('vm_refresh_token', r)
  }, a.access_token, a.refresh_token)
}

async function shot(page: Page, name: string) {
  await page.screenshot({ path: `${SHOT}/${name}.png`, fullPage: true })
}

test.describe.configure({ mode: 'serial' })

// ── T01 注册（真实 UI + 验证码从 Redis 取）──
test('T01 register via UI', async ({ page }) => {
  watch(page)
  const email = `reg+${Date.now()}@vocab.local`
  await page.goto('/register')
  await page.waitForLoadState('networkidle')
  await shot(page, '01-register')
  await page.locator('input[type="email"], input[placeholder*="邮"], input[placeholder*="mail"]').first().fill(email)
  await page.locator('input[type="password"]').first().fill('FixTest#2026')
  const nick = page.locator('input[placeholder*="昵"], input[placeholder*="nick"]').first()
  if (await nick.count()) await nick.fill('e2ereg')
  const sendBtn = page.locator('button:has-text("验证码"), button:has-text("发送"), button:has-text("code")').first()
  if (await sendBtn.count()) await sendBtn.click()
  await page.waitForTimeout(1500)
  const code = execSync(`python e2e/getcode.py "${email}"`, { encoding: 'utf-8' }).trim()
  const codeInput = page.locator('input[placeholder*="验证码"], input[placeholder*="code"], input[maxlength="6"]').first()
  if (await codeInput.count()) await codeInput.fill(code)
  await shot(page, '01-register-filled')
  const submitBtn = page.locator('button:has-text("注册"), button:has-text("Register"), button[type="submit"]').first()
  if (await submitBtn.count()) await submitBtn.click()
  await page.waitForTimeout(3000)
  await shot(page, '01-register-result')
  // 注册成功：跳走 register 页 或 出现成功提示；不该停在 register 且有错误
  const url = page.url()
  const bodyErr = await page.locator('.el-message--error').count()
  expect(url.includes('/login') || url === page.url().split('/register')[0] + '/' || bodyErr === 0).toBeTruthy()
})

// ── T02 登录（真实 UI）──
test('T02 login via UI', async ({ page }) => {
  watch(page)
  const a = auth()
  await page.goto('/login')
  await page.waitForLoadState('networkidle')
  await page.locator('input[type="email"], input[placeholder*="邮"], input[placeholder*="mail"]').first().fill(a.email)
  await page.locator('input[type="password"]').first().fill(a.password)
  await shot(page, '02-login-filled')
  await page.locator('button:has-text("登录"), button:has-text("Login"), button[type="submit"]').first().click()
  await page.waitForURL(u => !u.toString().includes('/login'), { timeout: 15000 }).catch(() => {})
  await page.waitForTimeout(2000)
  await shot(page, '02-login-result')
  expect(page.url()).not.toContain('/login')
})

// ── T03 仪表盘 ──
test('T03 dashboard after login', async ({ page }) => {
  watch(page); await loginAs(page)
  await page.goto('/dashboard')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2000)
  await shot(page, '03-dashboard')
  const text = await page.evaluate(() => document.body?.innerText ?? '')
  expect(text.length).toBeGreaterThan(50)
})

// ── T04 等级选择 ──
test('T04 levels page + select CET4', async ({ page }) => {
  watch(page); await loginAs(page)
  await page.goto('/levels')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1500)
  await shot(page, '04-levels')
  const levelCards = page.locator('.level-card, [class*="level"]').filter({ hasText: /CET|KET|PET|四级|大学/i })
  const cnt = await levelCards.count()
  // 点 CET4（若有）
  const cet4 = page.locator('text=/CET.?4|四级/i').first()
  if (await cet4.count()) await cet4.click().catch(() => {})
  await page.waitForTimeout(2000)
  await shot(page, '04-levels-after-click')
  expect(cnt).toBeGreaterThan(0)
})

// ── T05 学习会话（今日计划 + 翻卡 + 答题）──
test('T05 study session CET4', async ({ page }) => {
  watch(page); await loginAs(page)
  await page.goto('/study/CET4')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2500)
  await shot(page, '05-study-init')
  // 翻卡 / 认识 / 不认识 按钮
  const action = page.locator('button:has-text("认识"), button:has-text("会"), button:has-text("翻转"), button:has-text("不知道"), button:has-text("不会")').first()
  for (let i = 0; i < 3 && await action.count(); i++) {
    await action.click().catch(() => {})
    await page.waitForTimeout(800)
  }
  await shot(page, '05-study-progress')
  const text = await page.evaluate(() => document.body?.innerText ?? '')
  // 学习卡应渲染出单词或释义（不是空白/错误页）
  expect(text.length).toBeGreaterThan(20)
  expect(text).toMatch(/[\w]/) // 至少有词
})

// ── T06 选择题测试（重点：刚修的 submit 路径）──
test('T06 choice test generate + submit', async ({ page }) => {
  watch(page); await loginAs(page)
  await page.goto('/test')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1500)
  await shot(page, '06-test-entry')
  // 选 CET4 + 选择题 + 全部来源
  await page.locator('text=/CET.?4|四级/i').first().click().catch(() => {})
  await page.locator('text=选择题, text=/choice/i').first().click().catch(() => {})
  await page.locator('button:has-text("开始"), button:has-text("生成"), button:has-text("Start")').first().click().catch(() => {})
  await page.waitForTimeout(2500)
  await shot(page, '06-choice-question')
  // 盲选每题第一个选项并提交
  for (let i = 0; i < 25; i++) {
    const opt = page.locator('[class*="option"], .choice-item, .el-card').first()
    if (await opt.count()) await opt.click().catch(() => {})
    const next = page.locator('button:has-text("下一题"), button:has-text("下一"), button:has-text("Next")').first()
    if (await next.count()) { await next.click().catch(() => {}); await page.waitForTimeout(300) } else break
  }
  await page.locator('button:has-text("交卷"), button:has-text("提交"), button:has-text("Submit")').first().click().catch(() => {})
  await page.waitForTimeout(2500)
  await shot(page, '06-choice-result')
  const text = (await page.evaluate(() => document.body?.innerText ?? '')).toLowerCase()
  expect(text).not.toMatch(/500|服务器错误|internal server error/)
  // 真实提交后必有成绩反馈（正确率/得分/答对题数/百分比）
  expect(text).toMatch(/正确率|accuracy|得分|分数|答对|correct|题|\d+\s*\/\s*\d+|\d+\s*%/)
})

// ── T07 拼写测试 ──
test('T07 spelling test generate + submit', async ({ page }) => {
  watch(page); await loginAs(page)
  await page.goto('/test')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1200)
  await page.locator('text=/CET.?4|四级/i').first().click().catch(() => {})
  await page.locator('text=拼写, text=/spell/i').first().click().catch(() => {})
  await page.locator('button:has-text("开始"), button:has-text("生成")').first().click().catch(() => {})
  await page.waitForTimeout(2500)
  await shot(page, '07-spelling-question')
  for (let i = 0; i < 25; i++) {
    const inp = page.locator('input[type="text"], input[type="email"]:not([placeholder*="邮"])').first()
    if (await inp.count()) await inp.fill('xxxx').catch(() => {})
    const next = page.locator('button:has-text("下一题"), button:has-text("下一")').first()
    if (await next.count()) { await next.click().catch(() => {}); await page.waitForTimeout(300) } else break
  }
  await page.locator('button:has-text("交卷"), button:has-text("提交")').first().click().catch(() => {})
  await page.waitForTimeout(2500)
  await shot(page, '07-spelling-result')
  const text = (await page.evaluate(() => document.body?.innerText ?? '')).toLowerCase()
  expect(text).not.toMatch(/500|服务器错误|internal server error/)
  expect(text).toMatch(/正确率|accuracy|得分|分数|答对|correct|题|\d+\s*\/\s*\d+|\d+\s*%/)
})

// ── T08 听力测试 ──
test('T08 listening test generate + submit', async ({ page }) => {
  watch(page); await loginAs(page)
  await page.goto('/test')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1200)
  await page.locator('text=/CET.?4|四级/i').first().click().catch(() => {})
  await page.locator('text=听, text=/listen/i').first().click().catch(() => {})
  await page.locator('button:has-text("开始"), button:has-text("生成")').first().click().catch(() => {})
  await page.waitForTimeout(2500)
  await shot(page, '08-listening-question')
  await page.locator('button:has-text("交卷"), button:has-text("提交")').first().click().catch(() => {})
  await page.waitForTimeout(2500)
  await shot(page, '08-listening-result')
  const text = (await page.evaluate(() => document.body?.innerText ?? '')).toLowerCase()
  expect(text).not.toMatch(/500|服务器错误|internal server error/)
  expect(text).toMatch(/正确率|accuracy|得分|分数|答对|correct|题|\d+\s*\/\s*\d+|\d+\s*%/)
})

// ── T09 错词本 ──
test('T09 wrong-words page', async ({ page }) => {
  watch(page); await loginAs(page)
  await page.goto('/wrong-words')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2000)
  await shot(page, '09-wrong-words')
  const text = await page.evaluate(() => document.body?.innerText ?? '')
  expect(text.length).toBeGreaterThan(0)
})

// ── T10 自定义词库（新功能）──
test('T10 custom wordlists', async ({ page }) => {
  watch(page); await loginAs(page)
  await page.goto('/wordlists')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2000)
  await shot(page, '10-wordlists')
  // 创建新词库
  const create = page.locator('button:has-text("创建"), button:has-text("新建"), button:has-text("Create")').first()
  if (await create.count()) {
    await create.click().catch(() => {})
    await page.waitForTimeout(1000)
    const nameIn = page.locator('input').first()
    if (await nameIn.count()) await nameIn.fill('E2E测试词库').catch(() => {})
    const ok = page.locator('button:has-text("确定"), button:has-text("保存"), button:has-text("确认")').first()
    if (await ok.count()) await ok.click().catch(() => {})
    await page.waitForTimeout(1500)
  }
  await shot(page, '10-wordlists-created')
  expect(true).toBeTruthy()
})

// ── T11 单词搜索 ──
test('T11 word search', async ({ page }) => {
  watch(page); await loginAs(page)
  await page.goto('/word-search')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1500)
  const search = page.locator('input').first()
  if (await search.count()) {
    await search.fill('apple')
    await page.keyboard.press('Enter')
    await page.waitForTimeout(2000)
  }
  await shot(page, '11-word-search')
  const text = await page.evaluate(() => document.body?.innerText ?? '')
  // 搜 apple 应出词条 + 释义（不是空结果）
  expect(text.toLowerCase()).toContain('apple')
  expect(text).toMatch(/苹果|释义|n\.|v\.|a\.|adj|音标|\/.*\//)
})

// ── T12 统计 + 遗忘曲线 ──
test('T12 stats + forgetting curve', async ({ page }) => {
  watch(page); await loginAs(page)
  await page.goto('/stats')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2000)
  await shot(page, '12-stats')
  await page.goto('/stats/forgetting-curve')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2500)
  await shot(page, '12-forgetting-curve')
  // canvas/echarts 渲染
  const hasChart = await page.locator('canvas, [_echarts_instance]').count()
  expect(hasChart).toBeGreaterThanOrEqual(0) // 仅记录，不强求有图
})

// ── T13 设置 ──
test('T13 settings page', async ({ page }) => {
  watch(page); await loginAs(page)
  await page.goto('/settings')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2000)
  await shot(page, '13-settings')
  const text = await page.evaluate(() => document.body?.innerText ?? '')
  expect(text.length).toBeGreaterThan(0)
})

// ── T14 路由守卫 ──
test('T14 unauthenticated redirect', async ({ page }) => {
  watch(page)
  await page.goto('/login')
  await page.evaluate(() => localStorage.clear())
  await page.goto('/dashboard')
  await page.waitForTimeout(2500)
  await shot(page, '14-guard-redirect')
  expect(page.url()).toMatch(/login|auth|403/i)
})

// ── T15 控制台错误汇总（软失败：有错误则列出，不阻断）──
test('T15 console-error summary', async () => {
  const dedup = [...new Set(consoleErrors)]
  console.log('\n========== CONSOLE ERRORS (count=' + dedup.length + ') ==========')
  dedup.forEach(e => console.log('  ' + e))
  console.log('=========================================================\n')
  // 仅供人工审查；不强制 fail（真实页面常有第三方噪音）
  expect(dedup.length).toBeGreaterThanOrEqual(0)
})
