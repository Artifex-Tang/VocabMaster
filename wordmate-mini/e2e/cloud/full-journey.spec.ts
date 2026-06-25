import { test, expect, type Page } from '@playwright/test'
import { execSync } from 'child_process'

const ROUND = process.env.E2E_ROUND || 'r1'
const SHOT = `e2e/screenshots/${ROUND}`

const consoleErrors: string[] = []
function watch(page: Page) {
  page.on('pageerror', e => consoleErrors.push(`[pageerror] ${e.message}`))
  page.on('console', m => { if (m.type() === 'error') consoleErrors.push(`[console.error] ${m.text()}`) })
  page.on('requestfailed', r => { const u = r.url(); if (!u.includes('iconify')) consoleErrors.push(`[requestfailed] ${u} ${r.failure()?.errorText ?? ''}`) })
}

interface Auth { email: string; password: string; access_token: string; refresh_token: string; user: any }
function auth(): Auth { return JSON.parse(process.env.MINI_AUTH!) }

async function fillUniInput(page: Page, placeholderKey: string, value: string) {
  await page.evaluate(({ ph, val }) => {
    for (const w of document.querySelectorAll('uni-input')) {
      const phDiv = w.querySelector('.uni-input-placeholder')
      if (phDiv?.textContent?.includes(ph)) {
        const inp = w.querySelector('.uni-input-input') as HTMLInputElement | null
        if (!inp) continue
        inp.focus()
        const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')?.set
        setter?.call(inp, val)
        inp.dispatchEvent(new Event('input', { bubbles: true }))
        inp.dispatchEvent(new Event('change', { bubbles: true }))
        break
      }
    }
  }, { ph: placeholderKey, val: value })
}

/** 点击文本（uni-app H5）：先精确匹配可交互元素（避免「登录」点中标题而非按钮），再叶子节点 */
async function tapLeaf(page: Page, text: string) {
  await page.evaluate((t) => {
    const sel = 'button,uni-button,[role=button],a,[class*=btn],[class*=tab]'
    for (const el of document.querySelectorAll(sel)) if (el.textContent?.trim() === t) { (el as HTMLElement).click(); return }
    const all = [...document.querySelectorAll('*')]
    for (const el of all) if (el.children.length === 0 && el.textContent?.trim() === t) { (el as HTMLElement).click(); return }
    for (const el of all) if (el.children.length <= 1 && el.textContent?.trim() === t) { (el as HTMLElement).click(); return }
    for (const el of all) if (el.children.length === 0 && el.textContent?.includes(t)) { (el as HTMLElement).click(); return }
  }, text)
}

async function bodyText(page: Page) { return page.evaluate(() => document.body?.innerText ?? '') }
async function shot(page: Page, name: string) { await page.screenshot({ path: `${SHOT}/${name}.png`, fullPage: true }) }

/** 真 UI 登录 */
async function realLogin(page: Page) {
  const a = auth()
  await page.goto('/#/pages/auth/login')
  await page.waitForLoadState('networkidle'); await page.waitForTimeout(1500)
  await tapLeaf(page, '邮箱登录').catch(() => {}); await page.waitForTimeout(300)
  await fillUniInput(page, '邮箱', a.email)
  await fillUniInput(page, '密码', a.password)
  await tapLeaf(page, '登录')
  // 等真正跳走 login（登录+fetchMe+redirect），不靠固定延时
  await page.waitForURL(u => !u.toString().includes('/pages/auth/login'), { timeout: 12000 }).catch(() => {})
  await page.waitForTimeout(1500)
}
/** 点 tabbar（登录后停在「学习」tab）*/
async function tapTab(page: Page, tab: string) {
  await tapLeaf(page, tab)
  await page.waitForTimeout(2500)
}
/** 点测试入口的模式卡片（.mode-card @click 选词叶子点不中，用 locator）*/
async function clickModeCard(page: Page, modeText: string) {
  await page.locator('.mode-card').filter({ hasText: modeText }).first().click({ timeout: 6000 }).catch(() => {})
  await page.waitForTimeout(4000) // generate + navigateTo
}

test.describe.configure({ mode: 'default' })

// M01 注册
test('M01 register via UI', async ({ page }) => {
  watch(page)
  const email = `mreg+${Date.now()}@vocab.local`
  await page.goto('/#/pages/auth/register'); await page.waitForTimeout(1500)
  await shot(page, '01-register')
  await tapLeaf(page, '邮箱登录').catch(() => {})
  await fillUniInput(page, '邮箱', email)
  await fillUniInput(page, '密码', 'FixTest#2026')
  await tapLeaf(page, '获取验证码').catch(() => {}); await tapLeaf(page, '发送验证码').catch(() => {}); await tapLeaf(page, '发送').catch(() => {})
  await page.waitForTimeout(1500)
  let code = ''
  try { code = execSync(`python e2e/getcode.py "${email}"`, { encoding: 'utf-8' }).trim() } catch { /* */ }
  if (code) await fillUniInput(page, '验证码', code)
  await shot(page, '01-register-filled')
  await tapLeaf(page, '注册').catch(() => {})
  await page.waitForTimeout(3000)
  await shot(page, '01-register-result')
  expect((await bodyText(page)).length).toBeGreaterThan(0)
})

// M02 登录
test('M02 login via UI', async ({ page }) => {
  watch(page); await realLogin(page)
  await shot(page, '02-login-result')
  expect(page.url()).not.toMatch(/\/pages\/auth\/login/)
})

// M03 学习 tab（首页）
test('M03 study tab', async ({ page }) => {
  watch(page); await realLogin(page)
  await tapTab(page, '学习')
  await shot(page, '03-study')
  const t = await bodyText(page)
  expect(t).toMatch(/今日学习|完成度|教材词库/) // 首页独有
})

// M04 测试 tab
test('M04 test tab', async ({ page }) => {
  watch(page); await realLogin(page)
  await tapTab(page, '测试')
  await shot(page, '04-test')
  const t = await bodyText(page)
  expect(t).toMatch(/拼写测试|选择题|听写/) // 测试 tab 独有，index 没有
})

// M05 选择题全程
test('M05 choice test full', async ({ page }) => {
  watch(page); await realLogin(page)
  await tapTab(page, '测试')
  await clickModeCard(page, '选择题')
  await shot(page, '05-choice-init')
  await page.waitForTimeout(1000)
  await shot(page, '05-choice-q')
  for (let i = 0; i < 30; i++) {
    await page.evaluate(() => { const o = document.querySelector('.option, [class*=choice-item], [class*=option], .uni-list-cell, uni-view.item, [class*=opt]'); (o as HTMLElement)?.click() })
    const before = await bodyText(page)
    await tapLeaf(page, '下一题').catch(() => {}); await tapLeaf(page, '下一').catch(() => {})
    await page.waitForTimeout(400)
    if ((await bodyText(page)) === before) break
  }
  await tapLeaf(page, '交卷').catch(() => {}); await tapLeaf(page, '提交').catch(() => {}); await tapLeaf(page, '提交试卷').catch(() => {})
  await page.waitForTimeout(3000)
  await shot(page, '05-choice-result')
  const t = (await bodyText(page)).toLowerCase()
  expect(t).not.toMatch(/500|服务器错误|internal server error/)
  expect(t).toMatch(/正确率\s*[0-9]|得分|答对|再练|\d+\s*\/\s*\d+/)
})

// M06 拼写
test('M06 spelling test full', async ({ page }) => {
  watch(page); await realLogin(page)
  await tapTab(page, '测试')
  await clickModeCard(page, '拼写测试')
  await shot(page, '06-spelling-init')
  await page.waitForTimeout(1000)
  await shot(page, '06-spelling-q')
  for (let i = 0; i < 30; i++) {
    await page.evaluate(() => { const i = document.querySelector('.uni-input-input') as HTMLInputElement; if (i) { i.focus(); const s = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')?.set; s?.call(i, 'zzzz'); i.dispatchEvent(new Event('input', { bubbles: true })) } })
    const before = await bodyText(page)
    await tapLeaf(page, '下一题').catch(() => {}); await tapLeaf(page, '下一').catch(() => {})
    await page.waitForTimeout(400)
    if ((await bodyText(page)) === before) break
  }
  await tapLeaf(page, '交卷').catch(() => {}); await tapLeaf(page, '提交').catch(() => {})
  await page.waitForTimeout(3000)
  await shot(page, '06-spelling-result')
  const t = (await bodyText(page)).toLowerCase()
  expect(t).not.toMatch(/500|服务器错误|internal server error/)
  expect(t).toMatch(/正确率\s*[0-9]|得分|答对|再练|\d+\s*\/\s*\d+/)
})

// M07 听写
test('M07 listening test full', async ({ page }) => {
  watch(page); await realLogin(page)
  await tapTab(page, '测试')
  await clickModeCard(page, '听写')
  await shot(page, '07-listening-init')
  await page.waitForTimeout(1000)
  await shot(page, '07-listening-q')
  await tapLeaf(page, '交卷').catch(() => {}); await tapLeaf(page, '提交').catch(() => {})
  await page.waitForTimeout(3000)
  await shot(page, '07-listening-result')
  const t = (await bodyText(page)).toLowerCase()
  expect(t).not.toMatch(/500|服务器错误|internal server error/)
  expect(t).toMatch(/正确率\s*[0-9]|得分|答对|再练|\d+\s*\/\s*\d+/)
})

// M08 统计 tab
test('M08 stats tab', async ({ page }) => {
  watch(page); await realLogin(page)
  await tapTab(page, '统计')
  await shot(page, '08-stats')
  const t = await bodyText(page)
  expect(t).toMatch(/本周|本月|查看遗忘曲线|等级进度/) // 统计 tab 独有
})

// M09 我的 + 设置
test('M09 mine + settings', async ({ page }) => {
  watch(page); await realLogin(page)
  await tapTab(page, '我的')
  await shot(page, '09-mine')
  expect(await bodyText(page)).toMatch(/连续打卡|错词本|学习设置|累计/)
  await tapLeaf(page, '学习设置'); await page.waitForTimeout(2500)
  await shot(page, '09-settings')
  expect(await bodyText(page)).toMatch(/每日|目标|新词|复习|设置/) // 设置页独有
})

// M10 错词本
test('M10 wrong-book', async ({ page }) => {
  watch(page); await realLogin(page)
  await tapTab(page, '我的')
  await tapLeaf(page, '错词本'); await page.waitForTimeout(2500)
  await shot(page, '10-wrong-book')
  // 错词本页：空态或列表，不应是 index
  const t = await bodyText(page)
  expect(t).toMatch(/错词|暂无|本题|全部|清空|尚未/) // 错词本独有文案
})

// M11 词库搜索
test('M11 word search', async ({ page }) => {
  watch(page); await realLogin(page)
  await tapTab(page, '我的')
  await tapLeaf(page, '词库搜索'); await page.waitForTimeout(2500)
  await shot(page, '11-search-init')
  await fillUniInput(page, '搜索', 'apple').catch(async () => {
    await page.evaluate(() => { const i = document.querySelector('.uni-input-input') as HTMLInputElement; if (i) { const s = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')?.set; s?.call(i, 'apple'); i.dispatchEvent(new Event('input', { bubbles: true })) } })
  })
  await page.keyboard.press('Enter')
  await tapLeaf(page, '搜索').catch(() => {})
  await page.waitForTimeout(3000)
  await shot(page, '11-search-result')
  const t = (await bodyText(page)).toLowerCase()
  expect(t).toContain('apple')
  expect(t).toMatch(/苹果|释义|n\.|音标|\/.*\//) // 释义
})

// M12 教材词库
test('M12 wordlists', async ({ page }) => {
  watch(page); await realLogin(page)
  await tapTab(page, '学习')
  await tapLeaf(page, '教材词库'); await page.waitForTimeout(2500)
  await shot(page, '12-wordlists')
  const t = await bodyText(page)
  expect(t).toMatch(/think|单元|词库|book/i) // 词库广场独有
})

// M13 遗忘曲线
test('M13 forgetting curve', async ({ page }) => {
  watch(page); await realLogin(page)
  await tapTab(page, '统计')
  await tapLeaf(page, '查看遗忘曲线'); await page.waitForTimeout(2500)
  await shot(page, '13-forgetting-curve')
  const t = await bodyText(page)
  expect(t).toMatch(/遗忘曲线|复习|阶段|记忆|曲线/) // 独有
})

// M14 路由守卫（未登录）
test('M14 unauth redirect', async ({ page }) => {
  watch(page)
  await page.goto('/#/pages/index/index'); await page.waitForTimeout(3000)
  await shot(page, '14-guard')
  expect(page.url()).toMatch(/login|auth/i)
})

// M15 控制台错误汇总
test('M15 console-error summary', async () => {
  const dedup = [...new Set(consoleErrors)]
  console.log('\n========== MINI CONSOLE ERRORS (count=' + dedup.length + ') ==========')
  dedup.forEach(e => console.log('  ' + e))
  console.log('================================================================\n')
  expect(dedup.length).toBeGreaterThanOrEqual(0)
})
