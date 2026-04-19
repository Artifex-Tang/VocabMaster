# Prompt — Web 前端开发

> 把这份 prompt 作为 Claude Code 在 `frontend-web/` 目录下的起始任务描述。

## 你的角色

你是一位熟悉 Vue 3 组合式 API 的前端工程师，负责开发 VocabMaster 的 PC 端 Web 应用。

## 必读文档

1. `../CLAUDE.md`
2. `../docs/00-README.md`
3. `../docs/03-api-specification.md`（**所有接口都在这**）
4. `../docs/04-ebbinghaus-algorithm.md`（算法是后端的事，但前端需要理解 stage 含义）
5. `../docs/07-frontend-web.md`（**主文档**）

## 硬性技术约束

| 项 | 要求 |
|----|------|
| 框架 | Vue 3.5+ |
| 构建 | Vite 5.x |
| 语言 | TypeScript 5.x（strict 模式） |
| 状态管理 | Pinia 2.x |
| UI 库 | Element Plus 2.x |
| HTTP | axios 1.x（封装成 `utils/request.ts`） |
| 路由 | Vue Router 4.x |
| 图表 | ECharts 5.x（用于遗忘曲线、学习统计） |
| 本地存储 | Dexie.js（IndexedDB 封装，用于离线队列和词库缓存） |
| 语音 | Web Speech API 优先，降级到音频 CDN |
| 样式 | UnoCSS 或 原子 CSS + SCSS 模块化 |
| 包管理 | pnpm（**不用 npm/yarn**） |

## 设计规范

从 `docs/07-frontend-web.md` 第 8 节复制：
- 主色：`#1890FF`（蓝）+ `#4F46E5`（强调紫）
- 状态色：未学 `#9CA3AF` / 学习中 `#3B82F6` / 已掌握 `#10B981` / 错误 `#EF4444`
- 字体：中文 `PingFang SC / Noto Sans SC`，英文 `Inter`，IPA `Charis SIL`（需引入 @font-face）
- 间距尺度：4 / 8 / 12 / 16 / 24 / 32 / 48
- 圆角：8px（卡片） / 4px（按钮） / 16px（弹窗）
- 阴影：`0 2px 8px rgba(0,0,0,0.08)`

## 开发顺序

### 第 0 步：工程骨架
```bash
pnpm create vite frontend-web -- --template vue-ts
cd frontend-web
pnpm add vue-router pinia axios element-plus echarts dexie @vueuse/core
pnpm add -D unocss sass @types/node
```

目录：
```
frontend-web/
├── src/
│   ├── main.ts
│   ├── App.vue
│   ├── router/
│   ├── stores/           # Pinia
│   ├── api/              # axios 封装 + 各模块 API 函数
│   ├── utils/
│   │   ├── request.ts    # axios 实例 + 拦截器
│   │   ├── db.ts         # Dexie 本地 DB
│   │   ├── tts.ts        # 语音封装
│   │   └── time.ts       # 时区工具（dayjs）
│   ├── composables/      # useOfflineSync / useTts / useKeyboardShortcut
│   ├── components/       # 全局组件（WordCard, EbbinghausChart）
│   ├── views/            # 页面
│   │   ├── auth/         # Login, Register
│   │   ├── study/        # Today, Session, Result
│   │   ├── test/         # Spelling, Choice, Listening
│   │   ├── stats/        # Dashboard, ForgettingCurve
│   │   └── profile/      # Settings, Achievements
│   ├── assets/           # 字体、图标、音频
│   └── styles/
├── public/
└── vite.config.ts
```

### 第 1 步：基础设施
按 `docs/07-frontend-web.md` 第 4~6 节实现：

1. `utils/request.ts`
   - 请求拦截：注入 `Authorization: Bearer <token>`、`X-Device-Id`、`X-Device-Type: web`
   - 响应拦截：`code === 0` 放行，其他 code 走错误处理
   - 401 自动刷新 token（队列 + 只刷新一次）
   - network error 时把请求放入 `pendingQueue` 走离线
2. `utils/db.ts` — Dexie 定义 `cachedWords` 表 + `pendingAnswers` 表
3. `stores/user.ts` — 用户状态（token / profile / settings）
4. `stores/study.ts` — 当前学习会话状态
5. `composables/useOfflineSync.ts`
   - 用 `@vueuse/core` 的 `useOnline` 监听网络
   - 联网时自动 flush `pendingAnswers`
   - 用户点"立即同步"也触发

### 第 2 步：认证流程
1. 登录页：邮箱/手机号 + 密码，"记住我" 7 天
2. 注册页：邮箱/手机号 + 验证码（倒计时 60s）
3. 第三方登录按钮：微信（扫码）、Apple
4. 路由守卫：未登录访问受保护页面 → 跳登录 + 回跳

### 第 3 步：学习主流程（**核心体验**）
1. `views/study/Today.vue`
   - 顶部：今日目标 / 已完成 / 剩余（进度环）
   - 列表：到期复习单词 + 新词（分组展示）
   - 开始学习按钮
2. `views/study/Session.vue`
   - 大卡片展示单词
   - `components/WordCard.vue`：
     - 正面：单词 + 音标 + 发音按钮 + 🔊 自动发音开关
     - 背面：中文释义 + 例句 + 图片 + emoji
     - 3D 翻转动画（`transform: rotateY`）
   - 操作区：【简单】【一般】【困难】【再看看】（对应 correct/correct/wrong/skip）
   - 快捷键：`Space` 翻卡 / `1/2/3/4` 选操作 / `P` 发音
3. `views/study/Result.vue`
   - 展示：新学 X 个 / 复习 X 个 / 正确率 X% / 连续打卡 X 天
   - 分享按钮（生成截图 PNG，用 html2canvas）

### 第 4 步：测试模式
1. 拼写测试：展示释义+发音，输入单词
2. 选择题：4 选 1
3. 听写：只听发音拼写
4. 结果页展示错题 + 一键加入错词本

### 第 5 步：统计报表
1. `views/stats/Dashboard.vue`
   - 日历热力图（学习天数）
   - 折线图：最近 30 天学习量
   - 饼图：各等级学习分布
2. `views/stats/ForgettingCurve.vue`
   - ECharts 绘制理论曲线 + 个人实际点叠加
   - 按等级切换

### 第 6 步：个人中心
1. 设置：每日新词目标 / 时区 / 通知开关 / TTS 音色
2. 错词本：按错误次数排序，可以批量重学
3. 成就墙：已解锁成就 + 进度中成就
4. 账号：修改密码 / 绑定手机 / 导出数据 / 注销

## 离线模式规则

1. 用户点"下载等级 CET-4 到本地"后，调 `GET /words/bank?level=CET4&page=all`
2. 结果写入 Dexie `cachedWords` 表
3. 离线时：
   - 今日计划从本地 Dexie 算（复刻后端 `get_today_plan` 逻辑的简化版）
   - 答题结果写入 `pendingAnswers` 队列
4. 联网后：
   - 批量调 `POST /study/answer-batch` 上报
   - 后端返回冲突列表，前端按"服务端优先"更新本地

## TTS 策略

```ts
async function speak(word: string, accent: 'uk' | 'us' = 'uk') {
  // 1. 优先用 Web Speech API
  if ('speechSynthesis' in window) {
    const utter = new SpeechSynthesisUtterance(word);
    utter.lang = accent === 'uk' ? 'en-GB' : 'en-US';
    utter.rate = 0.9;
    speechSynthesis.speak(utter);
    return;
  }
  // 2. 降级：调后端提供的音频 URL（有道 / 剑桥 CDN）
  const audioUrl = accent === 'uk' ? word.audio_url_uk : word.audio_url_us;
  if (audioUrl) new Audio(audioUrl).play();
}
```

## 禁止事项

- ❌ 不要用 Options API，**统一 `<script setup>`**
- ❌ 不要 `any` 过度，关键 interface 必须定义
- ❌ 不要直接 `fetch`，必须走 `utils/request.ts`
- ❌ 不要在组件里直接操作 IndexedDB，走 `utils/db.ts` 封装
- ❌ 不要硬编码颜色/间距，用 UnoCSS 规则或 SCSS 变量
- ❌ 不要用 vuex，**只用 Pinia**
- ❌ 图标统一用 `@iconify/vue`，不要同时引多套图标库

## 性能目标

| 指标 | 目标 |
|------|------|
| 首屏加载（3G） | < 3s |
| 学习卡片切换 | < 50ms |
| 遗忘曲线渲染 | < 300ms |
| Lighthouse 性能 | > 90 |
| 打包体积（gzip） | < 500KB（首屏） |

## 环境配置

`.env.development`:
```
VITE_API_BASE=http://localhost:8080/api/v1
VITE_WS_BASE=ws://localhost:8080/ws
VITE_DEVICE_TYPE=web
```

`.env.production`:
```
VITE_API_BASE=/api/v1
VITE_WS_BASE=/ws
VITE_DEVICE_TYPE=web
```

## 你开始工作时的第一条回复

先输出：
1. "我已阅读 CLAUDE.md 和 docs/00~07"
2. 你的工程目录计划
3. 你打算先做哪个页面跑通全链路（建议先做 Login → Today → Session）
4. 对 `docs/07-frontend-web.md` 有无疑问

在我确认后再动代码。
