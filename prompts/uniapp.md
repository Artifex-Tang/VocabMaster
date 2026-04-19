# Prompt — Uni-app 移动端开发（小程序 + Android APK）

> 把这份 prompt 作为 Claude Code 在 `frontend-uniapp/` 目录下的起始任务描述。

## 你的角色

你是一位熟悉 Uni-app（Vue 3 版）的移动端工程师，负责用**一套代码**同时编译出**微信小程序**和**Android APK**（以及 H5 作为调试通道）。

## 必读文档

1. `../CLAUDE.md`
2. `../docs/00-README.md`
3. `../docs/03-api-specification.md`
4. `../docs/08-frontend-uniapp.md`（**主文档**）
5. `../docs/07-frontend-web.md`（参考 Web 版的离线策略和 TTS 策略，移动端复用思路）

## 硬性技术约束

| 项 | 要求 |
|----|------|
| 框架 | Uni-app 3.x，Vue 3 setup 语法 |
| 语言 | TypeScript |
| 状态管理 | Pinia 2.x（uni-app 适配版） |
| UI 库 | uview-plus 3.x（移动端），自定义组件混用 |
| HTTP | `uni.request` 封装 + 适配层 |
| 本地存储 | `uni.setStorageSync` / `uni.getStorageSync`（不同端统一抽象） |
| 图表 | uCharts（小程序兼容性好） |
| TTS | 分端：小程序用 `uni.createInnerAudioContext` + 后端 audio URL；Android 原生用 `plus.speech`（5+ App API） |
| 编译目标 | `mp-weixin`（小程序）+ `app`（Android APK）+ `h5`（调试） |
| 包管理 | pnpm（或 HBuilderX 默认） |

## Uni-app 跨端兼容原则

### 核心原则：**平台差异抽象到适配层**

千万不要在业务组件里写 `#ifdef MP-WEIXIN` ... `#endif`，污染性极强。建立 `utils/platform/` 目录，把所有平台 API 差异封装掉：

```
utils/platform/
├── index.ts              # 统一导出
├── tts.ts                # TTS 跨端封装
├── storage.ts            # 存储跨端封装
├── auth.ts               # 登录跨端封装（wx.login / Apple / 手机号）
├── share.ts              # 分享跨端封装
└── navigate.ts           # 导航封装
```

业务组件只调用 `import { speak } from '@/utils/platform/tts'`，不关心底层。

### TTS 适配示例

```ts
// utils/platform/tts.ts
export async function speak(text: string, accent: 'uk' | 'us', audioUrl?: string) {
  // #ifdef MP-WEIXIN
  if (!audioUrl) {
    // 小程序不能直接 TTS，必须走音频 URL
    uni.showToast({ title: '无音频可播放', icon: 'none' });
    return;
  }
  const ctx = uni.createInnerAudioContext();
  ctx.src = audioUrl;
  ctx.play();
  return;
  // #endif

  // #ifdef APP-PLUS
  // Android 原生 TTS
  plus.speech.speak(text, {
    lang: accent === 'uk' ? 'en-GB' : 'en-US',
    rate: 0.9,
  });
  return;
  // #endif

  // #ifdef H5
  const utter = new SpeechSynthesisUtterance(text);
  utter.lang = accent === 'uk' ? 'en-GB' : 'en-US';
  speechSynthesis.speak(utter);
  // #endif
}
```

条件编译注释（`#ifdef`）**只出现在 `utils/platform/` 下**。

## 目录结构

```
frontend-uniapp/
├── src/
│   ├── main.ts
│   ├── App.vue
│   ├── pages.json          # 页面路由 + 导航 + tabbar
│   ├── manifest.json       # 多端配置（appid、权限、SDK）
│   ├── uni.scss            # 全局样式变量
│   ├── pages/
│   │   ├── index/          # 今日学习（首页）
│   │   ├── study/
│   │   │   ├── session.vue
│   │   │   └── result.vue
│   │   ├── test/
│   │   ├── stats/
│   │   ├── mine/           # 个人中心
│   │   ├── auth/
│   │   └── wrong-book/     # 错词本
│   ├── components/
│   │   ├── WordCard.vue
│   │   ├── EbbinghausChart.vue  # uCharts
│   │   └── CheckinCalendar.vue
│   ├── stores/             # Pinia
│   ├── api/                # 各模块接口函数
│   ├── utils/
│   │   ├── request.ts      # uni.request 封装
│   │   ├── db.ts           # 本地 DB 抽象（基于 uni.setStorage）
│   │   └── platform/       # 跨端适配层（见上）
│   ├── composables/
│   └── static/             # 图标、图片
├── pages.json
├── manifest.json
├── package.json
└── vite.config.ts
```

## pages.json 关键配置

```json
{
  "easycom": { "autoscan": true, "custom": { "^u-(.*)": "uview-plus/components/u-$1/u-$1.vue" } },
  "pages": [
    { "path": "pages/index/index", "style": { "navigationBarTitleText": "VocabMaster" } },
    { "path": "pages/study/session", "style": { "navigationBarTitleText": "学习中" } },
    { "path": "pages/auth/login", "style": { "navigationBarTitleText": "登录" } }
  ],
  "tabBar": {
    "color": "#9CA3AF",
    "selectedColor": "#1890FF",
    "list": [
      { "pagePath": "pages/index/index", "text": "学习" },
      { "pagePath": "pages/test/index", "text": "测试" },
      { "pagePath": "pages/stats/index", "text": "统计" },
      { "pagePath": "pages/mine/index", "text": "我的" }
    ]
  }
}
```

## manifest.json 关键配置

### 微信小程序侧
```json
"mp-weixin": {
  "appid": "<你的小程序 AppID>",
  "setting": { "urlCheck": false, "minified": true, "es6": true },
  "usingComponents": true,
  "permission": { "scope.userLocation": { "desc": "用于本地化学习推荐" } }
}
```

### App 侧（Android）
```json
"app-plus": {
  "usingComponents": true,
  "nvueStyleCompiler": "uni-app",
  "compilerVersion": 3,
  "splashscreen": { "alwaysShowBeforeRender": true, "waiting": false },
  "modules": {
    "Speech": {},
    "OAuth": {},
    "Share": {}
  },
  "distribute": {
    "android": {
      "packagename": "com.vocabmaster.app",
      "permissions": [
        "<uses-permission android:name=\"android.permission.INTERNET\"/>",
        "<uses-permission android:name=\"android.permission.RECORD_AUDIO\"/>"
      ],
      "minSdkVersion": 24,
      "targetSdkVersion": 34
    }
  }
}
```

## 开发顺序

### 第 0 步：工程骨架
用 HBuilderX 新建项目，或用 Vite CLI：
```bash
pnpm create vite frontend-uniapp -- --template uni-app-vue3-ts
```
或用 uni CLI：
```bash
pnpm create uni-app --template uni-preset-vue-vite-ts frontend-uniapp
```

### 第 1 步：基础设施
1. `utils/request.ts`：包装 `uni.request`，行为与 Web 版的 axios 拦截器一致（token 注入、401 刷新、离线队列）
2. `utils/platform/*`：全部实现
3. `stores/user.ts`、`stores/study.ts`
4. 全局样式变量（`uni.scss`）

### 第 2 步：登录流程
1. 手机号/邮箱登录页
2. 微信一键登录（小程序端）：
   ```ts
   uni.login({
     provider: 'weixin',
     success: (res) => {
       // 发 code 给后端换 openid
       api.auth.wechatLogin({ code: res.code });
     }
   });
   ```
3. Apple 登录（App 端 iOS 上架才需要，Android 不强制）
4. 登录后拉取用户设置、当前学习进度

### 第 3 步：首页（今日学习）
1. 顶部：问候语 + 当前等级切换
2. 中部：今日进度环（uCharts）
3. 下部：到期复习 + 新词数量，大按钮"开始学习"
4. 底部：最近打卡日历

### 第 4 步：学习会话页（**核心**）
1. `components/WordCard.vue`：
   - 手势：左滑/右滑（对应 wrong/correct）
   - 点击翻卡（3D 翻转，nvue 下用原生动画）
   - 顶部进度条
2. 操作按钮区
3. 键盘快捷键（App 端）/ 手势（小程序端）
4. 音频播放错误兜底（尝试 UK → US → 本地 TTS → 静默）

### 第 5 步：测试模式
同 Web 版三种：拼写 / 选择 / 听写。注意小程序的输入框体验要单独调。

### 第 6 步：统计页
uCharts 绘制：
- 遗忘曲线（折线）
- 学习热力图（日历）
- 等级分布（饼图）

### 第 7 步：个人中心
设置、错词本、成就、通知、账号管理。

## 离线存储策略

**小程序端**：`uni.setStorageSync` 限制 10MB，只缓存**当前学习等级**的词库（约 1~5 万词 × 字段，存 JSON 可能超限）。策略：
- 缓存时按等级分 chunk（每 500 词一个 key）
- 查询时按 chunk 索引查找
- 如果词库 > 10MB，提示用户"该等级词库过大，建议联网学习"

**App 端**：用 `plus.io` 文件系统 或 sqlite（通过 uni 的 sqlite API），容量充足。

统一接口由 `utils/db.ts` 抽象：
```ts
export async function cacheWords(levelCode: string, words: Word[]): Promise<void>
export async function queryCachedWords(levelCode: string): Promise<Word[]>
export async function enqueueAnswer(answer: PendingAnswer): Promise<void>
export async function flushPendingAnswers(): Promise<void>
```

## 编译产物

### 编译小程序
```bash
pnpm dev:mp-weixin        # 开发模式
pnpm build:mp-weixin      # 生产构建
# 然后用微信开发者工具打开 dist/build/mp-weixin 预览/上传
```

### 编译 Android APK
方式 1：HBuilderX **云打包**（推荐，无需本地 Android 环境）
方式 2：本地打包（需配置 Android SDK + keystore）

```bash
pnpm build:app-plus       # 先生成 App 资源
# 然后在 HBuilderX 里"发行 → 原生 App-云打包"
```

### 编译 H5（调试用）
```bash
pnpm dev:h5               # 快速验证业务逻辑
```

## 禁止事项

- ❌ 不要在业务代码里写 `#ifdef`，条件编译**集中在 `utils/platform/`**
- ❌ 不要用 `wx.*` API 直接调用，用 `uni.*` 确保跨端
- ❌ 不要假设 `window` / `document` 存在（小程序没有）
- ❌ 不要用 CSS 变量（小程序支持不完整），用 SCSS 变量
- ❌ 不要在小程序端做大量并发请求（同时最多 10 个），请求要排队
- ❌ 不要在 App 端直接用 CDN 字体（FOUT 严重），把字体打包进资源

## 性能目标

| 指标 | 目标（小程序 / App） |
|------|------|
| 冷启动 | < 2s / < 3s |
| 页面切换 | < 200ms |
| 答题响应 | < 100ms（含翻卡动画） |
| 词库下载（CET-4） | < 5s（Wi-Fi） |

## 你开始工作时的第一条回复

先输出：
1. "我已阅读 CLAUDE.md 和 docs/00~08"
2. 你的工程搭建方案（HBuilderX 还是 CLI）
3. 你打算先跑通哪一端（建议 **H5 → 小程序 → App**，H5 最快迭代）
4. 你计划怎样组织 `utils/platform/` 抽象层
5. 对 `docs/08-frontend-uniapp.md` 有无疑问

在我确认后再动代码。
