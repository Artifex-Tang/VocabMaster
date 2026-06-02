# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: full-flow.spec.ts >> Web E2E - VocabMaster >> unauthenticated redirect to login
- Location: e2e\full-flow.spec.ts:155:3

# Error details

```
Error: expect(received).toMatch(expected)

Expected pattern: /login|auth/
Received string:  "http://localhost:3001/study"
```

# Page snapshot

```yaml
- generic [ref=e3]:
  - generic [ref=e4]: "404"
  - generic [ref=e5]: 页面不存在
  - link "返回首页" [ref=e6] [cursor=pointer]:
    - /url: /
```

# Test source

```ts
  64  |     await page.screenshot({ path: `${SCREENSHOT_DIR}/web-home.png`, fullPage: true })
  65  | 
  66  |     const errors: string[] = []
  67  |     page.on('pageerror', err => errors.push(err.message))
  68  | 
  69  |     const text = await page.evaluate(() => document.body?.innerText ?? '')
  70  |     // 页面应有内容（不管是否登录成功）
  71  |     expect(text.length).toBeGreaterThan(0)
  72  |   })
  73  | 
  74  |   // ── 3. 单词学习 ──
  75  |   test('study page accessible', async ({ page }) => {
  76  |     await page.goto('/login')
  77  |     await page.evaluate(() => {
  78  |       localStorage.setItem('token', 'mock-access-token-abc123')
  79  |       localStorage.setItem('user', JSON.stringify({ uuid: 'u-001', nickname: '测试用户' }))
  80  |     })
  81  |     await page.goto('/study')
  82  |     await page.waitForTimeout(3000)
  83  |     await page.screenshot({ path: `${SCREENSHOT_DIR}/web-study.png`, fullPage: true })
  84  | 
  85  |     const errors: string[] = []
  86  |     page.on('pageerror', err => errors.push(err.message))
  87  |     expect(errors).toHaveLength(0)
  88  |   })
  89  | 
  90  |   // ── 4. 测试模块 ──
  91  |   test('test entry page renders', async ({ page }) => {
  92  |     await page.goto('/login')
  93  |     await page.evaluate(() => {
  94  |       localStorage.setItem('token', 'mock-access-token-abc123')
  95  |       localStorage.setItem('user', JSON.stringify({ uuid: 'u-001', nickname: '测试用户' }))
  96  |     })
  97  |     await page.goto('/test')
  98  |     await page.waitForTimeout(3000)
  99  |     await page.screenshot({ path: `${SCREENSHOT_DIR}/web-test.png`, fullPage: true })
  100 | 
  101 |     const errors: string[] = []
  102 |     page.on('pageerror', err => errors.push(err.message))
  103 |     expect(errors).toHaveLength(0)
  104 |   })
  105 | 
  106 |   // ── 5. 单词搜索 ──
  107 |   test('word search page', async ({ page }) => {
  108 |     await page.goto('/login')
  109 |     await page.evaluate(() => {
  110 |       localStorage.setItem('token', 'mock-access-token-abc123')
  111 |       localStorage.setItem('user', JSON.stringify({ uuid: 'u-001', nickname: '测试用户' }))
  112 |     })
  113 |     await page.goto('/word/search')
  114 |     await page.waitForTimeout(3000)
  115 |     await page.screenshot({ path: `${SCREENSHOT_DIR}/web-word-search.png`, fullPage: true })
  116 | 
  117 |     const errors: string[] = []
  118 |     page.on('pageerror', err => errors.push(err.message))
  119 |     expect(errors).toHaveLength(0)
  120 |   })
  121 | 
  122 |   // ── 6. 遗忘曲线 ──
  123 |   test('ebbinghaus curve page', async ({ page }) => {
  124 |     await page.goto('/login')
  125 |     await page.evaluate(() => {
  126 |       localStorage.setItem('token', 'mock-access-token-abc123')
  127 |       localStorage.setItem('user', JSON.stringify({ uuid: 'u-001', nickname: '测试用户' }))
  128 |     })
  129 |     await page.goto('/ebbinghaus')
  130 |     await page.waitForTimeout(3000)
  131 |     await page.screenshot({ path: `${SCREENSHOT_DIR}/web-ebbinghaus.png`, fullPage: true })
  132 | 
  133 |     const errors: string[] = []
  134 |     page.on('pageerror', err => errors.push(err.message))
  135 |     expect(errors).toHaveLength(0)
  136 |   })
  137 | 
  138 |   // ── 7. 设置页 ──
  139 |   test('settings page renders', async ({ page }) => {
  140 |     await page.goto('/login')
  141 |     await page.evaluate(() => {
  142 |       localStorage.setItem('token', 'mock-access-token-abc123')
  143 |       localStorage.setItem('user', JSON.stringify({ uuid: 'u-001', nickname: '测试用户' }))
  144 |     })
  145 |     await page.goto('/settings')
  146 |     await page.waitForTimeout(3000)
  147 |     await page.screenshot({ path: `${SCREENSHOT_DIR}/web-settings.png`, fullPage: true })
  148 | 
  149 |     const errors: string[] = []
  150 |     page.on('pageerror', err => errors.push(err.message))
  151 |     expect(errors).toHaveLength(0)
  152 |   })
  153 | 
  154 |   // ── 8. 路由守卫 - 未登录跳转 ──
  155 |   test('unauthenticated redirect to login', async ({ page }) => {
  156 |     await page.goto('/login')
  157 |     await page.evaluate(() => {
  158 |       localStorage.clear()
  159 |     })
  160 |     await page.goto('/study')
  161 |     await page.waitForTimeout(2000)
  162 |     // 应该被重定向到登录页
  163 |     const url = page.url()
> 164 |     expect(url).toMatch(/login|auth/)
      |                 ^ Error: expect(received).toMatch(expected)
  165 |   })
  166 | 
  167 |   // ── 9. API 连通性 ──
  168 |   test('backend API is reachable through proxy', async ({ page }) => {
  169 |     const response = await page.request.get('http://localhost:3001/api/v1/actuator/health')
  170 |     expect(response.ok()).toBeTruthy()
  171 |     const body = await response.json()
  172 |     expect(body.status).toBe('UP')
  173 |   })
  174 | })
  175 | 
```