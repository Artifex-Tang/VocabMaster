# 自定义词库移植 — 小程序端（wordmate-mini）

- 日期：2026-06-24
- 状态：移植中
- 上游设计：`docs/superpowers/specs/2026-06-23-custom-word-list-design.md`
- 上游 Web 实现：`wordmate-web/src/api/wordList.ts` + `src/views/wordlists/*`（已上线云 prod，E2E 14/14）

## 1. 背景

后端 `wordlist` 业务包（`/word-lists/*` + Think 5 级 2593 词）已上线云。Web 端全栈完成。本任务把同一功能移植到 uni-app 小程序端（H5 / 微信小程序 / Android 共码）。后端 API **零改动**，只做 mini 前端。

## 2. 契约核对（mini vs web vs 后端）

| 项 | mini 现状 | 移植处理 |
|----|----------|---------|
| http 封装 | `src/utils/request.ts`，`http.get/post`，`code===0` 取 `data`，`20002` 刷新 | 直接复用 |
| Word 类型 | `src/api/types.ts` `Word`（snake_case）= web `WordBank` | 复用，`learn()` 返回 `Word[]` |
| 提交答题 | `useOfflineSync().submitAnswer(AnswerPayload)`（在线走 `/study/answer`，离线入队）| learn 页复用 |
| 卡片组件 | `src/components/word-card/word-card.vue`，prop `word`，emit `flip`，expose `reset()` | learn 页复用 |
| study 卡片范式 | `src/pages/study/session.vue`（进度条 + word-card + 三按钮）| learn 页照搬骨架 |
| 等级常量 | `LEVELS`（10 个，无 THINK_*）| **不加进 LEVELS**（守 org 模型 A），THINK_* 用 `THINK_NAMES` 在 test/index 注入 |
| 测试入口 | `src/pages/test/index.vue`，picker over `activeLevels`，`source` ref，**不读 query** | 加 `onLoad` 读 `level`+`source` + 注入 THINK 选项（对标 web TestEntry 修复）|
| tabBar | 4 tab，PNG 图标（study/test/stats/mine）| **不加第 5 tab**（缺图标资产 + 微信 5 上限风险），改 index 页入口卡 |
| 导航 | `uni.navigateTo/redirectTo`，query 串参 | 同 |

后端端点（已上线，不变）：
- `GET /word-lists?source_type=builtin` → `WordListSummary[]`
- `GET /word-lists/{id}` → `WordListDetail`（含 `units: UnitSummary[]`）
- `POST /word-lists/{id}/subscribe` → `{list_id, current_unit_no}`
- `GET /word-lists/{id}/learn?unit={n}&limit={m}` → `Word[]`
- `POST /word-lists/{id}/units/{n}/advance` → `number`
- 学习/复习/测试复用 `/study/answer`、`/test/generate`、`/test/availability`（`level_code=THINK_*`）

## 3. 改动清单

### 3.1 新增

| 文件 | 作用 |
|------|------|
| `src/api/wordList.ts` | square/detail/subscribe/learn/advanceUnit 5 函数 |
| `src/pages/wordlists/square.vue` | 词库广场（builtin 卡片网格 + 订阅/进入）|
| `src/pages/wordlists/detail.vue` | 词库详情（`?id=`：头部 + 复习入口 + 单元网格）|
| `src/pages/wordlists/learn.vue` | 学新词（`?id=&unit=`：卡片会话，复用 word-card + submitAnswer，完成态 advanceUnit + 下一单元/复习）|

### 3.2 修改

| 文件 | 改动 |
|------|------|
| `src/api/types.ts` | 加 `WordListSummary` / `UnitSummary` / `WordListDetail` / `THINK_NAMES` |
| `src/pages.json` | 注册 3 新页 |
| `src/pages/index/index.vue` | 加「教材词库」入口卡 → `navigateTo /pages/wordlists/square` |
| `src/pages/test/index.vue` | 加 `onLoad` 读 `level`+`source` query；`levelOptions` 合并 activeLevels + 注入 THINK_*；picker/selected 改用 levelOptions |

## 4. 页面设计

### square.vue
- `onShow` 调 `square()` 取 builtin 列表
- 卡片：emoji / name / word_count 词 / 「订阅」或「已订阅·进入」按钮
- 订阅 → `subscribe(id)` → toast → `navigateTo /pages/wordlists/detail?id=`
- 已订阅 → 直接进 detail

### detail.vue
- `onLoad(id)` + `onShow` 调 `detail(id)`
- 头部：emoji + name + `word_count 词 · unit_count 单元`
- 「复习到期词」按钮 → `switchTab?` 否——test/index 是 tab。用 `uni.navigateTo` 带 query：`/pages/test/index?level=${origin_level_code}&source=due`。**注意**：test/index 是 tabBar 页，`navigateTo` 不能跳 tabBar 页！必须 `uni.switchTab`，但 switchTab **不带 query**。

  → **方案**：复习入口改用**临时存储**传参。mini 已有 storage util。或：复习不跳 test/index 配置页，而是直接 `generate` 后跳测试子页（choice/spelling/listening）—— 但需选模式。

  → **最终方案**：用 `uni.setStorageSync('test_query', {level, source})`，test/index `onShow` 读 storage 后清。绕开 switchTab 不带 query 限制。见 §5。

- 单元网格：每单元 卡片 `Unit {n}` + 进度（learned/total）+ 完成标记；tap → `navigateTo /pages/wordlists/learn?id=&unit=`
- 未订阅显示「订阅」按钮

### learn.vue
- `onLoad(id, unit)` 调 `learn(id, unit, limit=setting.daily_new_words_goal||20)` + `detail(id)`（取 unit_count/origin_level）
- 骨架同 study/session.vue：进度条 + word-card + 不认识/认识（学新词场景去掉「跳过」，与 web 一致只有 认识/不认识）
- 答题 `submitAnswer({mode:'card'})` → idx++
- 队列空（单元已无新词）→ 提示 + 「进入下一单元」/「返回词库」
- 学完一批 → finished 态：「进入下一单元」（advanceUnit + 重载）/「去复习」（同 detail 复习逻辑）/「返回词库」

## 5. 复习入口的 query 传递（关键技术点）

微信小程序 `uni.switchTab` **不支持 query 参数**，而 test/index 是 tabBar 页。两条路：

- **A（采用）**：Storage 中转。detail/learn 复习按钮 `uni.setStorageSync('test_entry_override', {level, source})` 后 `uni.switchTab({url:'/pages/test/index'})`；test/index `onShow` 检测并消费该 key，preselect level+source，然后 `removeStorageSync`。H5/Android 同样走 uni storage，三端一致。
- B：把 test/index 改非 tab 页 —— 破坏现有导航，不做。

test/index 改动（消费 override）：
```ts
const override = uni.getStorageSync('test_entry_override')
if (override) {
  queryLevel.value = override.level; source.value = override.source
  uni.removeStorageSync('test_entry_override')
}
```
levelOptions 注入 queryLevel（THINK_*）使 picker 能显示。对标 web TestEntry 的 `THINK_NAMES` 动态选项。

## 6. 短语测试

后端 TestService 已守：mode=spelling 命中短语（word 含空格 或 pos∈{phrase, phrasal verb}）跳过/回退 choice。mini 不感知，正常调 `generate(level=THINK_*, source, mode)`。

## 7. 测试计划

- **H5 Playwright**（`wordmate-mini`，现有 e2e 基建）：新增 `e2e/wordlist.spec.ts`，覆盖 square 渲染 → 订阅 → detail 单元网格 → learn 卡片翻转+答题 → 复习入口写 storage。回归：现有 31 个 H5 用例不破。
- **微信结构测试**（`e2e/wechat-test.js`）：构建产物校验含 3 新页。
- 后端走云 prod（已部署），H5 dev 连云或本地后端。mock 兜底见现有 request。

## 8. 不做（YAGNI）

- 第 5 个 tabBar（缺图标资产）
- 用户 Excel 上传 / 手动选词（后端表已留位，后续增量）
- example_zh（全局未生成）
- 跨词库同池复习（后端不支持）
