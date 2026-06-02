# 测试报告 — VocabMaster 移动端

> 日期：2026-06-02
> 测试人：Claude Code 自动化测试
> 项目：wordmate-mini（uni-app 微信小程序 + H5）

---

## 1. 单元测试（Vitest）

```
Test Files  4 passed (4)
     Tests  48 passed (48)
  Duration  6.81s
```

| 文件 | 测试数 | 覆盖内容 |
|------|--------|---------|
| utils.test.ts | 23 | nowIso、formatDate、formatTime、fromNow、untilReview、storage CRUD、LocalTable |
| stores.test.ts | 8 | StudyStore（init/next/markCorrect/reset）、SettingsStore（defaults/sync/load） |
| api.test.ts | 9 | LEVELS 常量、TestQuestion 类型、TestResult、UserSettings、Word related_words、离线队列 |
| composable.test.ts | 8 | TTS audioUrl 优先、艾宾浩斯九阶段晋级/降级、nowIso 格式、detectType |

命令：`pnpm test`

---

## 2. H5 手机仿真测试（Playwright）

```
22 passed (22)
iPhone 15 (393×852) + Pixel 7 (412×915)
Duration 1.1m
```

| 测试 | 说明 |
|------|------|
| login page renders | VocabMaster、邮箱/手机号切换、输入框、登录按钮 |
| register page renders | 邮箱/手机号注册切换 |
| all 4 tab pages load | 首页、测试、统计、我的 — 无 JS 报错 |
| word search page | 搜索输入框可见 |
| forgetting curve page | 搜索输入框可见 |
| wrong book page | 页面加载正常 |
| achievements page | 页面加载正常 |
| settings page | 页面加载正常 |
| login tab switching | 邮箱/手机号切换正常 |
| no JS errors | 登录页无控制台错误 |
| viewport is mobile | 视口宽度 < 500px |

截图保存在 `e2e/screenshots/`（16 张，覆盖全部页面）。

命令：`npx playwright test`

---

## 3. 微信小程序结构测试（Node.js + DevTools API）

```
Total: 19 tests
Passed: 19
Failed: 0
Package size: 378KB (0.37MB) — 远低于 2MB 限制
```

| 测试 | 说明 |
|------|------|
| DevTools CLI API reachable | 开发者工具 HTTP 接口可连接 |
| Project loaded in DevTools | 项目已加载 |
| mp-weixin build output exists | 构建产物完整 |
| pages.json has 16 pages | 16 个页面全部注册 |
| All page files exist | 每个页面的 .wxml 文件存在 |
| Each page has .wxml/.js/.json | 三件套完整 |
| TabBar configured with 4 tabs | 学习/测试/统计/我的 |
| TabBar icons exist | 8 个图标文件全部存在 |
| Static tab icons exist | 静态资源完整 |
| API layer compiled | auth/word/study/stats/sync/test 6 个 API 文件 |
| Stores compiled | user/study/settings 3 个 Store |
| Utils compiled | 工具函数已编译 |
| Components compiled | WordCard/EbbinghausChart |
| Key JS files parseable | 核心文件无语法错误 |
| Package size under 2MB | 378KB，安全 |
| Preview generation succeeds | 预览功能正常 |
| project.config.json valid | AppID 配置正确 |

结果文件：`test-results-wechat.json`

命令：`node e2e/wechat-test.js`

---

## 4. 构建验证

| 平台 | 命令 | 结果 |
|------|------|------|
| H5 build | `uni build --platform h5` | ✅ 零错误 |
| mp-weixin build | `uni build --platform mp-weixin` | ✅ 零错误 |
| dev:h5 | `localhost:3003` | ✅ 正常运行 |
| dev:mp-weixin | `dist/dev/mp-weixin` | ✅ 监听模式 |

---

## 5. 测试覆盖总结

| 类别 | 测试数 | 通过 | 失败 |
|------|--------|------|------|
| 单元测试（Vitest） | 48 | 48 | 0 |
| H5 手机仿真（Playwright） | 22 | 22 | 0 |
| 微信小程序结构（Node.js） | 19 | 19 | 0 |
| **合计** | **89** | **89** | **0** |

---

## 6. 已知限制

1. **API 测试**：后端未启动，API 调用均失败。H5 仿真测试中受认证保护的页面（Dashboard、测试、统计、我的）会跳转登录页。需后端运行后做端到端集成测试。
2. **微信小程序真机测试**：miniprogram-automator 版本兼容性问题，改用 DevTools HTTP API + 文件结构验证。真机交互测试需手动扫码。
3. **离线同步**：需后端配合测试离线队列 flush 逻辑。

---

## 7. 测试命令速查

```bash
# 单元测试
cd wordmate-mini && pnpm test

# H5 手机仿真
cd wordmate-mini && npx playwright test

# 微信小程序结构
cd wordmate-mini && node e2e/wechat-test.js

# 全量构建验证
pnpm build:h5 && pnpm build:mp-weixin
```
