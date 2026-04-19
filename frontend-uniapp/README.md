# VocabMaster — Uni-app 移动端

> 一套代码编译微信小程序 + Android APK + H5。

## 技术栈

| 项 | 版本 |
|----|------|
| Uni-app | 3.x（Vue 3） |
| TypeScript | 5.x |
| Pinia | 2.x |
| uview-plus | 3.x |
| uCharts | 2.x |

---

## 开发进度

### ✅ 阶段 0：工程骨架（2026-04-19 完成）

#### 已完成模块

| 模块 | 文件 | 说明 |
|------|------|------|
| 项目配置 | `package.json` / `vite.config.ts` / `tsconfig.json` | 依赖声明、路径别名 `@/` |
| 多端配置 | `src/manifest.json` | 小程序 AppID 占位、Android 权限、H5 代理 |
| 路由配置 | `src/pages.json` | 4-tab（学习/测试/统计/我的）+ 12 个页面路由 |
| 全局样式 | `src/uni.scss` | SCSS 变量：颜色、圆角、间距、字号 |
| 应用入口 | `src/main.ts` / `src/App.vue` | Pinia 初始化、离线队列 flush |
| **跨端适配层** | `src/utils/platform.ts` | `getPlatform()` / `isMiniProgram()` / `isApp()` |
| | `src/utils/tts.ts` | TTS 跨端：H5 WebSpeech / 小程序音频 URL / App plus.speech |
| | `src/utils/storage.ts` | `storage` KV + `LocalTable<T>` 离线队列抽象 |
| | `src/utils/request.ts` | `uni.request` 封装：token 注入、401 自动刷新、离线静默降级 |
| | `src/utils/device-id.ts` | 设备 ID 生成与持久化 |
| | `src/utils/date.ts` | 日期格式化、fromNow、untilReview |
| **API 层** | `src/api/types.ts` | 全部 DTO 类型定义 |
| | `src/api/auth.ts` | 登录/注册/刷新/微信一键登录 |
| | `src/api/study.ts` | 今日计划/答题/批量上报 |
| | `src/api/word.ts` | 词库查询/下载/搜索/错词 |
| | `src/api/stats.ts` | 统计/打卡/日历/成就 |
| | `src/api/sync.ts` | 离线推送/增量拉取 |
| **Pinia Stores** | `src/stores/user.ts` | 登录态、token 刷新、持久化 |
| | `src/stores/settings.ts` | 用户设置、本地缓存 |
| | `src/stores/study.ts` | 当前学习会话状态 |
| **Composable** | `src/composables/useOfflineSync.ts` | 网络监听 + 答题提交 + 队列 flush |
| **组件** | `src/components/word-card/word-card.vue` | 单词卡（点击翻面、音频降级、emoji/图片） |
| | `src/components/ebbinghaus-chart/ebbinghaus-chart.vue` | 遗忘曲线图（uCharts mix） |
| **页面** | `src/pages/auth/login.vue` | 邮箱密码登录 + 微信一键登录（小程序端） |
| | `src/pages/auth/register.vue` | 邮箱注册 + 验证码倒计时 |
| | `src/pages/index/index.vue` | 学习 Tab：今日进度、学习计划、打卡日历 |
| | `src/pages/study/session.vue` | 学习会话：进度条、WordCard、答对/答错 |
| | `src/pages/study/done.vue` | 完成页：统计数字、返回/错词本 |
| | `src/pages/test/index.vue` | 测试 Tab：三种模式选择 + 题量/来源配置 |
| | `src/pages/test/spelling.vue` | 拼写测试 |
| | `src/pages/test/choice.vue` | 四选一 |
| | `src/pages/test/listening.vue` | 听写（自动播放音频） |
| | `src/pages/stats/index.vue` | 统计 Tab：周/月报表、每日柱图、等级进度 |
| | `src/pages/mine/index.vue` | 我的 Tab：用户信息、打卡连续天、菜单、退出 |
| | `src/pages/wrong-book/list.vue` | 错词本列表 + 开始错词复习 |

#### 约束遵守情况

- ✅ `#ifdef` 条件编译仅出现在 `utils/platform.ts` 和 `utils/tts.ts`
- ✅ 业务组件/页面全部使用 `uni.*` API，无 `wx.*` 直调
- ✅ 无 `window` / `document` 直接引用（H5 分支除外，已用 `typeof window` 守卫）
- ✅ 布局全部使用 `rpx` + SCSS 变量，无硬编码 px
- ✅ 离线队列通过 `useOfflineSync` 统一管理

---

### ⬜ 阶段 1：H5 端联调（下一步）

**目标**：`pnpm dev:h5` 跑通完整主流程

**待完成**：

- [ ] 补充 `.env.development`（`VITE_API_BASE=http://localhost:8080/api/v1`）
- [ ] 准备 tab 图标 PNG（8 张放 `static/tab-icons/`，否则小程序报错）
- [ ] 执行 `pnpm install` 安装依赖
- [ ] H5 冒烟测试：注册 → 登录 → 今日学习 → 答题 → 完成页
- [ ] 修复 H5 联调中发现的问题

### ⬜ 阶段 2：学习核心功能完善

- [ ] `WordCard` 手势滑动（左滑=不认识、右滑=认识）
- [ ] 学习会话支持离线模式（答题结果进本地队列）
- [ ] 词库下载到本地（`/words/download` + `LocalTable` 分 chunk 缓存）
- [ ] TTS 音频实际播放验证（需后端返回真实 audio URL）

### ⬜ 阶段 3：统计图表接入

- [ ] 安装 `@qiun/ucharts` 插件
- [ ] `EbbinghausChart` 接入真实数据
- [ ] 统计页每日柱图改为 uCharts 柱状图
- [ ] 遗忘曲线独立页（`stats/forgetting-curve.vue`）

### ⬜ 阶段 4：小程序端适配

- [ ] 配置微信小程序 AppID
- [ ] 配置 request 合法域名（后端 API + 有道 CDN）
- [ ] 微信一键登录真机验证
- [ ] 包体积检查（主包 ≤ 2MB，超出做分包）

### ⬜ 阶段 5：Android App 打包

- [ ] HBuilderX 云打包测试证书版本
- [ ] 正式 keystore 签名（务必备份）
- [ ] `plus.speech` TTS 真机验证
- [ ] 版本更新检查接口对接

---

## 快速启动

```bash
# 安装依赖
pnpm install

# H5 开发模式（最快迭代）
pnpm dev:h5

# 小程序（需要微信开发者工具）
pnpm dev:mp-weixin
# 然后用微信开发者工具导入 dist/dev/mp-weixin

# Android（生成资源后用 HBuilderX 云打包）
pnpm build:app-plus
```

## 环境变量

新建 `.env.development`：

```
VITE_API_BASE=http://localhost:8080/api/v1
```

生产环境新建 `.env.production`：

```
VITE_API_BASE=https://api.vocabmaster.com/api/v1
```
