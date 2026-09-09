# 游客浏览模式（小程序审核整改）— 设计文档

- 日期：2026-09-09
- 状态：已确认（用户批准方案 B）
- 背景拒审原文：「小程序打开一进入【首页】页面，未浏览体验功能服务，即要求授权手机号码、头像、昵称进行授权登录，请在用户体验浏览功能服务后，再自行选择授权登录。」

## 目标

微信小程序首次打开不强制登录；审核员/新用户可先体验**词库浏览与查词**（含发音、配图），遇到需个人数据的功能（学习/测试/统计/我的）时再以 modal 引导自主登录。

## 非目标（YAGNI）

- 不做游客学习进度 / 试学
- 不做游客打卡、统计空态图表
- 不改 Web/H5 端（审核对象仅小程序）
- 不新增任何用户状态字段（游客 = 未登录，纯推导）

## 架构

### 后端（SecurityConfig 白名单，最小改动）

`permitAll` 新增 3 条只读内容端点：

| 端点 | 用途 |
|------|------|
| `/words/search` | 搜词（分页） |
| `/words/by-word` | 按词+等级查详情 |
| `/words/{id:[0-9]+}` | 按 id 查详情（正则限数字，不吞 `/words/download`） |

明确不动：`/words/download`（离线包，用户功能）、学习/打卡/测试/统计全部接口。
滥用兜底：现有 nginx 限流 30r/s。
数据安全：3 端点均纯词库内容，无用户数据。

### 前端（wordmate-mini）

1. **首页** `src/pages/index/index.vue`：
   - 删除 `loadData()` 中 `if (!userStore.isLoggedIn) reLaunch('/pages/auth/login')` 强制跳转
   - 游客渲染专属视图：问候语 + 等级选择器（`/words/levels` 公开）+「搜词试试」入口卡 + 登录引导卡（按钮 → login 页）
   - 登录用户视图不变
2. **搜词页** `src/pages/word/search.vue`：游客直接可用（查词/音标/发音/配图）
3. **统一登录引导** `src/utils/requireLogin.ts`（新）：
   - `requireLogin(): boolean` — 未登录弹 modal「登录后即可使用」，确认 → `uni.reLaunch('/pages/auth/login')`，返回 false；已登录返回 true
   - 挂载点（非 tab 页）：设置、错词本、词库学习、词详情「开始学习」类按钮——进入前/点击时 modal 拦截
   - **tab 页（统计/我的/测试）不用 modal**：onShow 检测未登录 → 渲染居中空态（图标 + 一句话说明 + 「去登录」按钮），避免「modal 取消后白屏」。空态即页面本身内容
   - 首页（也是 tab）例外：游客态有自己的内容视图（见上），不拦
4. **user store**：不改。游客判定用现有 `isLoggedIn` 取反
5. **登录回流**：login 成功 → 现有 `switchTab` 首页，不变

### 数据流

游客 → 首页（无请求或仅 levels/topics）→ 搜词页 → `GET /words/search`（无 Authorization 头，200）→ 详情 → `GET /words/{id}`（200）。
点学习/测试 → `requireLogin()` 拦截（请求根本不发）→ 登录 → 回首页正常。

## 错误处理

- 游客误触受限功能：`requireLogin()` 在前端拦截，不发无效请求
- 已登录 token 过期：现有 401 → refresh → 失败踢登录逻辑不动
- 限流 429：现有 toast 文案

## 测试计划

| 层 | 用例 |
|----|------|
| 后端 | 无 token `GET /words/search` → 200；无 token `GET /words/download` → 401/403；带 token 全部照旧 |
| mini vitest | `requireLogin` 未登录弹窗+跳转、已登录直接放行；首页游客态不触发 reLaunch |
| DevTools 冒烟 | 清 storage → 首页停留 → 搜词出结果 → 点「学习」出登录引导 → UI 登录 → 功能正常 |

## 验收标准（对审核）

1. 首次打开（未登录）停留在首页，可见产品内容与功能入口
2. 能实际体验查词（含发音、配图）
3. 需要个人数据的功能点击时才出现登录引导，且可拒绝继续逛
4. 登录行为全部用户主动触发
