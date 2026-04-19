# 执行日志 — Uni-app 工程搭建

**日期**：2026-04-19  
**阶段**：阶段 0（工程骨架）+ 阶段 1（H5 环境搭建）  
**最终状态**：`pnpm dev:h5` 在 `http://localhost:3001/` 启动成功，TypeScript 零报错

---

## 阶段 0：工程骨架生成

### 背景

从零创建 `frontend-uniapp/` 目录，目标是一套代码同时编译微信小程序 + Android APK + H5。  
参考文档：`prompts/uniapp.md`、`docs/08-frontend-uniapp.md`、`docs/03-api-specification.md`。

### 关键决策

| 决策 | 选项 A | 选项 B（选择） | 理由 |
|------|--------|----------------|------|
| 工程创建方式 | HBuilderX GUI | Vite CLI | 便于版本控制，CI/CD 友好；HBuilderX 保留用于云打包 |
| 跨端适配层组织 | 嵌套 `utils/platform/` 子目录 | 平铺 `utils/` | 与 `docs/08` 一致，减少导入路径层级 |
| tabBar 数量 | docs/08 的 3-tab（今日/报表/我的） | prompts/uniapp.md 的 4-tab（学习/测试/统计/我的） | 用户明确指定 4-tab |
| PATCH 方法 | 原样传给 `uni.request` | POST + `X-HTTP-Method-Override` header | `uni.request` 不支持 PATCH，需绕过 |

### 生成文件清单

```
frontend-uniapp/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── .env.development          # VITE_API_BASE=http://localhost:8080/api/v1
├── .env.production           # VITE_API_BASE=https://api.vocabmaster.com/api/v1
├── .npmrc                    # registry=https://registry.npmmirror.com
├── src/
│   ├── main.ts               # createSSRApp + Pinia
│   ├── App.vue               # onLaunch 初始化 token；onShow 触发离线队列 flush
│   ├── pages.json            # 4-tab + 12 页面路由；easycom 自动扫描 uview-plus
│   ├── manifest.json         # 小程序/App/H5 多端配置（AppID 占位）
│   ├── uni.scss              # SCSS 变量：颜色/圆角/间距/字号
│   ├── utils/
│   │   ├── platform.ts       # getPlatform / isMiniProgram / isApp（含 #ifdef）
│   │   ├── tts.ts            # speak()：H5 WebSpeech / 小程序音频 URL / App plus.speech
│   │   ├── storage.ts        # storage KV + LocalTable<T> 离线队列抽象
│   │   ├── request.ts        # uni.request 封装：token/device-id 注入、401 刷新、PATCH 转换
│   │   ├── device-id.ts      # 设备 ID 生成与持久化
│   │   └── date.ts           # formatDate / fromNow / untilReview
│   ├── api/
│   │   ├── types.ts          # 全部 DTO 类型（AuthData / Word / TodayPlan 等）
│   │   ├── auth.ts           # login / register / refresh / wechatLoginMiniProgram
│   │   ├── study.ts          # getToday / answer / answerBatch
│   │   ├── word.ts           # getLevels / getWord / searchWords / downloadLevel
│   │   ├── stats.ts          # getTodayStats / getSummary / checkinToday / getCalendar
│   │   └── sync.ts           # push / pull
│   ├── stores/
│   │   ├── user.ts           # 登录态 + token 刷新 + 持久化
│   │   ├── settings.ts       # 用户设置（accent / autoPlay 等）
│   │   └── study.ts          # 学习会话状态（queue / currentIdx / correctCount）
│   ├── composables/
│   │   └── useOfflineSync.ts # 网络监听 + submitAnswer + flushQueue
│   ├── components/
│   │   ├── word-card/word-card.vue           # 翻卡组件（音频降级链）
│   │   └── ebbinghaus-chart/ebbinghaus-chart.vue  # uCharts mix 图
│   ├── pages/
│   │   ├── index/index.vue   # 学习 Tab（今日进度环 + 打卡日历）
│   │   ├── auth/login.vue    # 邮箱登录 + 微信一键（#ifdef MP-WEIXIN）
│   │   ├── auth/register.vue # 邮箱注册 + 验证码倒计时
│   │   ├── study/session.vue # 学习会话（WordCard + 答对/答错）
│   │   ├── study/done.vue    # 完成页（统计 + 返回/错词本）
│   │   ├── test/index.vue    # 测试 Tab（三模式选择 + 题量/来源配置）
│   │   ├── test/spelling.vue # 拼写测试
│   │   ├── test/choice.vue   # 四选一
│   │   ├── test/listening.vue# 听写（自动播放音频）
│   │   ├── stats/index.vue   # 统计 Tab（周/月报表 + 每日柱图）
│   │   ├── mine/index.vue    # 我的 Tab（用户信息 + 打卡 + 菜单）
│   │   └── wrong-book/list.vue # 错词本
│   └── static/tab-icons/     # 8 张 81×81 PNG 占位图
```

**共 35 个源文件**

---

## 阶段 1：H5 环境搭建

### Step 1 — 创建环境变量文件

```
frontend-uniapp/.env.development   → VITE_API_BASE=http://localhost:8080/api/v1
frontend-uniapp/.env.production    → VITE_API_BASE=https://api.vocabmaster.com/api/v1
```

### Step 2 — 生成 Tab 图标 PNG

uni-app tabBar 只接受 PNG，无法用 SVG。环境中 canvas 和 Pillow 均不可用，改用 Node.js 内置 `zlib` 生成最小合法 PNG：

```js
// 核心思路：PNG = sig + IHDR + IDAT(zlib deflate 的纯色像素) + IEND
// 每行：filter_byte(0x00) + [R G B] × width
const raw = Buffer.concat(Array.from({length:h}, () => row))
const idat = zlib.deflateSync(raw)
```

生成 8 张：`study/test/stats/mine` 各灰色（`#9CA3AF`）+ 蓝色（`#1890FF`）共 81×81px。

### Step 3 — 第一次 `pnpm install` → 失败

**错误**：
```
ERR_PNPM_NO_MATCHING_VERSION
No matching version found for @dcloudio/uni-components@3.0.0-4060920240930001
```

**根因**：`package.json` 中 `@dcloudio` 版本号 `3.0.0-4060920240930001` 是手写错误，该版本不存在。

**调查过程**：
```bash
pnpm view @dcloudio/uni-app dist-tags
# → vue3: '3.0.0-alpha-5000720260416001'
pnpm view @dcloudio/uni-components dist-tags
# → vue3: '3.0.0-alpha-5000720260416001'
```

所有 `@dcloudio/*` 包的 vue3 最新版均为 `3.0.0-alpha-5000720260416001`。

同时发现：原 `vite.config.ts` 中引用的 `@dcloudio/uni-vite` **不存在**，正确包名是 `@dcloudio/vite-plugin-uni`。

**修复**：
- 所有 `@dcloudio/*` 版本改为 `3.0.0-alpha-5000720260416001`
- `@dcloudio/uni-vite` → `@dcloudio/vite-plugin-uni`
- `vite.config.ts` import 同步修改

### Step 4 — 第二次 `pnpm install` → 失败

**错误**：
```
ERR_PNPM_NO_MATCHING_VERSION
No matching version found for @dcloudio/uni-app-vue@3.0.0-alpha-5000720260416001
(dependency of @dcloudio/uni-app-plus@3.0.0-alpha-5000720260416001)
```

**根因**：`uni-app-plus@416` 的依赖树中要求 `uni-app-vue@416`，但该包的 `vue3` tag 只发布到 `3.0.0-5000720260410001`（410，无 alpha 前缀）——发布日期差 6 天，疑似 dcloudio 发布流水线问题。

**修复**：在 `package.json` 中加 pnpm overrides 强制锁版本：
```json
"pnpm": {
  "overrides": {
    "@dcloudio/uni-app-vue": "3.0.0-5000720260410001"
  }
}
```

### Step 5 — 第三次 `pnpm install` → 成功，但 esbuild 未构建

**输出**：
```
Ignored build scripts: core-js-pure, core-js, esbuild, vue-demi
Run "pnpm approve-builds" to pick which dependencies should be allowed to run scripts.
```

**根因**：pnpm v10 默认禁止所有依赖的构建脚本（安全策略）。esbuild 未构建 = Vite 无法运行。

`pnpm approve-builds` 是交互式 TUI，无法在自动化环境中使用。

**修复**：在 `package.json` 中声明白名单：
```json
"pnpm": {
  "onlyBuiltDependencies": ["core-js", "core-js-pure", "esbuild", "vue-demi"]
}
```

再次 `pnpm install` — esbuild 及其他依赖正常构建完成。

**最终安装结果**：
```
Packages: +510
Done in 5.9s
WARN: peer deps (vue@3.5 vs expected 3.4, vite@5.4 vs expected 5.2) → 不影响运行
```

### Step 6 — `pnpm dev:h5` → 首次启动成功

```
编译器版本：5.07（vue3）
vite v5.4.21 dev server running at:
  ➜  Local:   http://localhost:3001/
ready in 3153ms.
```

### Step 7 — TypeScript 类型检查 → 5 处错误

```bash
pnpm type-check   # vue-tsc --noEmit
```

| # | 文件 | 错误 | 修复 |
|---|------|------|------|
| 1 | `pages/auth/register.vue:24` | 模板直接引用 `uni.navigateBack()` 不被 vue-tsc 识别 | 抽取为 `goBack()` 函数 |
| 2 | `pages/test/index.vue:44` | `source = src.key` 类型推断为 `string`，不兼容 `'due'\|'all'\|'wrong_words'` | 加 `as 'due' \| 'all' \| 'wrong_words'` 断言 |
| 3 | `utils/request.ts:4` | `import.meta.env` 无类型，`vite/client` 未引入 | `tsconfig.json` 的 `types` 加 `"vite/client"` |
| 4 | `utils/request.ts:65` | `uni.request` 不支持 `"PATCH"`（类型层面） | 引入 `toUniMethod()`：PATCH→POST，同时注入 `X-HTTP-Method-Override: PATCH` header |
| 5 | `utils/tts.ts:45` | `plus as Record<string,unknown>` 不被允许（类型不重叠） | 改为 `(plus as unknown) as Record<string,unknown>` 二步转型 |

修复后 `vue-tsc --noEmit` 零报错，`pnpm dev:h5` 再次干净启动（2972ms）。

---

## 遗留说明

### `PATCH` 方法的后端处理

`utils/request.ts` 发送 PATCH 请求时实际走 `POST`，并附加 header：
```
X-HTTP-Method-Override: PATCH
```
后端（Java Spring Boot）需开启 `HiddenHttpMethodFilter` 或在 `@RequestMapping` 中同时接受 `POST` 方法，才能正确路由到 `@PatchMapping` 接口。  
**后端开发时注意**：在 `application.yml` 加：
```yaml
spring:
  mvc:
    hiddenmethod:
      filter:
        enabled: true
```

### Tab 图标为占位图

`src/static/tab-icons/` 下 8 张 PNG 均为纯色方块，正式发版前需替换为设计稿图标（建议尺寸 81×81px，透明背景）。

### peer dependency 警告

以下 peer 警告不影响功能，等 `@dcloudio` 发布配套版本后自动消除：
- `vue@3.5` vs expected `3.4`
- `vite@5.4` vs expected `5.2`
- `@dcloudio/types@3.4.30` vs expected `3.4.28`

---

## 下一步

阶段 2（H5 冒烟测试）需要后端服务运行在 `localhost:8080`。  
如后端尚未就绪，可先做**阶段 3（手势滑动 + 离线缓存）**的纯前端逻辑，或 Mock API 数据走通主流程。
