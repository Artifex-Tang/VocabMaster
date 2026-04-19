# Web 前端开发日志

> 记录 `frontend-web/` 从零搭建到 mock 全链路跑通的完整过程。

---

## 环境信息

| 项目 | 值 |
|------|----|
| 操作系统 | Windows 11 Pro 10.0.22621 |
| Node.js | v22.14.0 |
| npm | 10.9.2 |
| pnpm | 10.33.0（执行前通过 `npm install -g pnpm` 安装） |
| 执行日期 | 2026-04-19 |

---

## 阶段一：工程骨架

### 1.1 创建 Vite 脚手架

```bash
pnpm create vite frontend-web -- --template vue-ts
```

Vite 默认拉到了 8.x / TS 6.x，但规格要求 Vite 5.x / TS 5.x，手动锁版本写入 `package.json`。

### 1.2 切换镜像源

npmjs.org 多次 `ECONNRESET`，切淘宝镜像解决：

```bash
pnpm config set registry https://registry.npmmirror.com
```

### 1.3 写入完整 package.json

```json
{
  "dependencies": {
    "@iconify/vue": "^4.3.0",
    "@vueuse/core": "^13.0.0",
    "axios": "^1.9.0",
    "dayjs": "^1.11.13",
    "dexie": "^4.0.10",
    "echarts": "^5.6.0",
    "element-plus": "^2.9.0",
    "pinia": "^2.3.1",
    "vue": "^3.5.13",
    "vue-router": "^4.5.1"
  },
  "devDependencies": {
    "@types/node": "^22.0.0",
    "@vitejs/plugin-vue": "^5.2.3",
    "sass": "^1.89.0",
    "typescript": "~5.8.3",
    "unplugin-auto-import": "^19.1.2",
    "unplugin-vue-components": "^28.5.0",
    "vite": "^5.4.19"
  }
}
```

pnpm 首次安装时报 `esbuild / @parcel/watcher / vue-demi` 未批准构建脚本，通过 `pnpm.onlyBuiltDependencies` 配置解决：

```bash
pnpm pkg set pnpm.onlyBuiltDependencies[]="esbuild" ...
pnpm install
```

最终安装耗时约 18s，共 145 个包。

---

## 阶段二：配置文件

### 2.1 tsconfig.json

将脚手架默认的 `es2023 / verbatimModuleSyntax` 替换为 strict 模式配置，增加 `baseUrl` 和 `paths`（`@/*` → `src/*`）。

同时新增 `src/shims-vue.d.ts`，解决 TypeScript 找不到 `.vue` 模块声明的问题。

### 2.2 vite.config.ts

关键决策：
- 用 `readFileSync('./package.json')` 替代 `require()` 读取版本号（ESM 不支持 `require`）
- 接入 `unplugin-auto-import` + `unplugin-vue-components`（ElementPlusResolver），实现 Element Plus 按需自动导入
- 增加 `manualChunks` 拆包（见阶段五）
- `css.preprocessorOptions.scss.api: 'modern-compiler'` 消除 Sass 废弃警告

### 2.3 环境变量

`.env.development`：

```
VITE_API_BASE=/api/v1
VITE_WS_BASE=/ws
VITE_DEVICE_TYPE=web
VITE_USE_MOCK=true   # 开发阶段启用 mock
```

`.env.production`：

```
VITE_API_BASE=/api/v1
VITE_WS_BASE=/ws
VITE_DEVICE_TYPE=web
```

---

## 阶段三：源码结构（46 个文件）

### 目录树

```
src/
├── api/             # axios 封装 + 各模块接口函数
│   ├── request.ts   # axios 实例 + 拦截器 + registerAuthHooks
│   ├── types.ts     # 全量 TypeScript 接口定义
│   └── auth / user / word / study / stats / checkin / sync / wrong-word / test / admin
├── stores/          # Pinia stores
│   ├── user.ts      # token + 用户信息 + 注册 auth 钩子
│   ├── settings.ts  # 用户设置
│   ├── study.ts     # 今日计划 + 当前学习会话
│   └── offline.ts   # 离线队列状态
├── router/
│   ├── index.ts     # 全量路由（公开 + 布局嵌套 + admin）
│   └── guards.ts    # 登录守卫 + 管理员守卫
├── composables/
│   ├── useTts.ts          # Web Speech API 封装，降级到音频 CDN
│   ├── useOfflineSync.ts  # 离线队列 + 联网自动 flush
│   └── useCountdown.ts    # 验证码倒计时
├── utils/
│   ├── storage.ts    # Dexie IndexedDB（pendingAnswers + cachedWords）
│   ├── device-id.ts  # UUID 生成与持久化
│   ├── date.ts       # dayjs + utc/timezone/relativeTime
│   └── constants.ts  # 十个等级列表 + 艾宾浩斯间隔数组 + 错误码
├── components/
│   ├── WordCard.vue          # 3D 翻转卡片（正面/背面）
│   ├── EbbinghausChart.vue   # ECharts 遗忘曲线
│   └── layout/
│       ├── AppLayout.vue     # el-aside + el-header + el-main 三栏
│       ├── AppSidebar.vue    # 导航菜单 + 离线徽章
│       └── AppHeader.vue     # 面包屑 + 云同步按钮 + 用户头像下拉
├── views/
│   ├── auth/          Login / Register / ResetPassword
│   ├── dashboard/     Dashboard（今日计划）+ WordListPreview
│   ├── study/         StudySession（含键盘快捷键）+ StudyDone
│   ├── test/          TestEntry / SpellingTest / ChoiceTest / ListeningTest
│   ├── stats/         StatsOverview / ForgettingCurve
│   ├── wrong-word/    WrongWordList
│   ├── settings/      UserSettings
│   ├── levels/        LevelSelection
│   ├── admin/         Dashboard / WordManage / UserManage（骨架）
│   └── errors/        NotFound / Forbidden
├── styles/
│   ├── variables.scss  # 颜色、间距、字体、圆角、阴影变量
│   └── main.scss       # 全局样式 + 3D 翻卡动画 + 工具类
└── main.ts
```

---

## 阶段四：关键架构决策

### 4.1 循环依赖：request.ts ↔ stores/user.ts

**问题**：`request.ts` 需要读 token（来自 `user store`），`user store` 又调用 `api/auth.ts`（依赖 `request.ts`），形成循环。

**解决方案**：`registerAuthHooks` 回调模式

```
request.ts 导出:
  export function registerAuthHooks(refresh, logout) { ... }

stores/user.ts 在 store 初始化末尾调用:
  registerAuthHooks(refreshAccessToken, logout)

request.ts 拦截器中:
  - 读 token → 直接 localStorage.getItem('vm_access_token')（不依赖 store）
  - 401/refresh → 调用 _refresh() / _logout()（通过回调，不 import store）
```

### 4.2 useOfflineSync 初始化方式

`useOnline` watcher 不放在模块顶层（多次 import 会注册多个监听），也不放在每个组件内（组件卸载后失效）。改为：

```ts
// composables/useOfflineSync.ts
export function initOfflineSync() {   // 全局单次调用
  const online = useOnline()
  watch(online, isOnline => { if (isOnline) flushPendingQueue() })
}
```

```ts
// main.ts
initOfflineSync()  // app 启动时调用一次
```

### 4.3 Element Plus 按需导入

不使用 `app.use(ElementPlus)`（全量 ~280 KB gzip）。

`unplugin-vue-components` 会在编译阶段分析 SFC 模板中用到的 `<el-*>` 组件，自动生成按需导入。`ElMessage` / `ElMessageBox` 等函数式 API 在 `.ts` 文件中直接 `import { ElMessage } from 'element-plus'` 即可，无需注册。

---

## 阶段五：构建优化

### 5.1 问题

初始构建 `index.js` 达 391 KB gzip，超出首屏 <500 KB 目标。

### 5.2 manualChunks 拆包

```ts
manualChunks: (id) => {
  if (id.includes('echarts') || id.includes('zrender'))  return 'echarts'
  if (id.includes('element-plus'))                        return 'element-plus'
  if (id.includes('/vue') || id.includes('pinia') || id.includes('vue-router')) return 'vue-vendor'
  if (id.includes('dexie'))   return 'dexie'
  if (id.includes('axios'))   return 'axios'
  if (id.includes('dayjs'))   return 'dayjs'
  if (id.includes('@iconify')) return 'iconify'
}
```

### 5.3 结果

| chunk | 大小（gzip） | 加载时机 |
|-------|-------------|---------|
| `index.js`（主入口） | **4.48 KB** | 首屏必须 |
| `vue-vendor.js` | 48.38 KB | 首屏必须 |
| `element-plus.js` | 281.33 KB | 首屏（Login 用到表单组件） |
| `echarts.js` | 343.41 KB | 仅 stats 页面加载 |
| `dexie.js` | 32.41 KB | 首次使用离线功能时 |
| `iconify.js` | 7.16 KB | 首屏（侧边栏图标） |

首屏总计约 **345 KB gzip**，符合 <500 KB 目标。

---

## 阶段六：Mock 服务

安装 `vite-plugin-mock@3 + mockjs`，通过 `VITE_USE_MOCK=true` 环境变量控制开关。

### Mock 文件

| 文件 | 覆盖接口 |
|------|---------|
| `mock/auth.ts` | login / register / send-code / refresh / logout / me |
| `mock/words.ts` | levels / topics / search / download（含 20 条 CET4 种子词） |
| `mock/study.ts` | today / answer / answer-batch / reset / mark-mastered |
| `mock/stats.ts` | today / summary / forgetting-curve / level-overview |
| `mock/checkin.ts` | today / calendar / achievements |
| `mock/user.ts` | settings GET/PATCH / me PATCH / sync push / wrong-words |

### 端到端验证结果

```
POST /api/v1/auth/login       → code=0, token 正常返回
GET  /api/v1/study/today      → review_count=5, new_count=5
POST /api/v1/study/answer     → stage_after=3, mastered=false
POST /api/v1/checkin/today    → current_streak=7
GET  /api/v1/stats/today      → accuracy=0.85, words_learned=8
```

---

## 已知问题与后续事项

| # | 问题 | 状态 |
|---|------|------|
| 1 | Sass `legacy-js-api` 废弃警告 | 已修复（`api: 'modern-compiler'`） |
| 2 | `element-plus` 全量 CSS（355 KB）在首屏加载 | 可接受，后续可拆分成按需 CSS |
| 3 | Admin 页面均为骨架占位 | 待实现 |
| 4 | `ForgettingCurve.vue` 的 `first_learned_at` 字段未在 `ForgettingCurveData` 类型中声明 | 类型定义待补充 |
| 5 | 测试模式（SpellingTest/ChoiceTest/ListeningTest）结果页复用 TestEntry 路由，尚未实现独立结果展示 | 待实现 |
| 6 | Service Worker（PWA 离线打开）未接入 | 规划在部署阶段用 vite-plugin-pwa 实现 |

---

## 启动方式

```bash
cd frontend-web

# 开发（启用 mock，无需后端）
pnpm dev          # http://localhost:3100

# 类型检查
pnpm type-check

# 生产构建
pnpm build
```

测试账号（mock）：任意邮箱 + 8 位以上密码，例如 `test@example.com` / `P@ssw0rd!`
