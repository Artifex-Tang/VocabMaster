# 11 — 开发路线图（分阶段任务拆分）

> 本文档为 Claude Code 的任务单，按顺序完成每个任务即可产出 MVP。每个任务含：目标、前置、输出、验收标准、预估时间。一人独立开发预计 3 周完成 MVP。

---

## 阶段一：基础设施 + 数据层（约 2 天）

### 任务 1.1 — 本地环境搭建

**目标**：在本地跑通 MySQL + Redis，能通过 Adminer 连接。

**输出**：
- `docker-compose.yml`（已在规格包）
- 一次 `docker compose up -d` 能起来
- Adminer 登录验证

**验收**：`mysql -u vocab -p vocabmaster` 能连，`SELECT * FROM level;` 返回 10 行。

**耗时**：0.5 天

---

### 任务 1.2 — 数据库 schema + 种子数据

**目标**：执行 `sql/init.sql` 建表，导入 `seed/words_sample.sql`。

**前置**：1.1 完成

**输出**：
- 15 张表全部创建
- `level` 表 10 条记录
- `word_topic` 表 20 条记录
- `achievement` 表 20 条记录
- `word_bank` 每等级至少 20 条

**验收**：
```sql
SELECT level_code, COUNT(*) FROM word_bank GROUP BY level_code;
-- 10 行，每行 >= 20
```

**耗时**：0.5 天

---

### 任务 1.3 — Java 后端项目脚手架

**目标**：建 Spring Boot 3.5.x 项目，能启动并连上 MySQL + Redis。

**输出**：
- `backend-java/` 完整目录结构（参考 `docs/05-backend-java.md`）
- `pom.xml` 全部依赖就位
- `VocabMasterApplication.java` 可启动
- `application-dev.yml` 连本地 docker 起的 MySQL/Redis
- Actuator 健康检查 `/actuator/health` 返回 UP
- Swagger UI `http://localhost:8080/api/v1/swagger-ui.html` 打得开

**验收**：
```bash
curl http://localhost:8080/api/v1/actuator/health
# {"status":"UP"}
```

**耗时**：0.5 天

---

### 任务 1.4 — 通用模块（响应封装 / 错误码 / 全局异常）

**目标**：实现 `common/` 下的基础设施代码。

**输出**：
- `R<T>`（统一响应）
- `ErrorCode` 枚举（完整错误码表，参考 `docs/03-api-specification.md` 附录）
- `BizException`
- `GlobalExceptionHandler`
- `PageResult<T>`
- `RequestIdFilter`（给每个请求分配 request_id 放入 MDC）

**验收**：
```bash
# 触发参数错误
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" -d '{}'
# 返回 {"code":10001,"msg":"...","data":null,"request_id":"..."}
```

**耗时**：0.5 天

---

## 阶段二：认证 + 用户模块（约 2 天）

### 任务 2.1 — JWT 工具 + Security 配置

**目标**：JWT 签发/解析工具、Security Filter Chain、路由放行规则。

**输出**：
- `JwtUtil`（generateAccessToken / generateRefreshToken / parse）
- `JwtAuthenticationFilter`
- `SecurityConfig`
- `UserContext`（ThreadLocal 存当前用户 ID + UUID）
- `@RequireAdmin` 注解 + AOP 实现
- `AesUtil`（手机号加解密）

**验收**：写一个临时 `/test/ping` 接口，要求登录，无 token 返回 20001；手动签发 token 带上能通过。

**耗时**：0.5 天

---

### 任务 2.2 — 注册 / 登录 / 刷新 token（邮箱+密码）

**目标**：完成 `/auth/register`、`/auth/login`、`/auth/refresh`、`/auth/logout` 四个接口。

**前置**：2.1

**输出**：
- `AuthController` + `AuthService`
- `UserService`（createUser、findByEmail 等）
- bcrypt 密码哈希
- Redis 存 refresh token 白名单 + access token 黑名单（登出用）
- 注册同时创建 `user_settings` 默认配置

**验收**：
- 注册一个 `test@example.com`，返回 access+refresh token
- 用同邮箱第二次注册返回 30001
- 用错密码登录返回 30004
- 拿 access_token 调 `/user/me` 返回用户信息

**耗时**：1 天

---

### 任务 2.3 — 用户信息 + 设置接口

**目标**：`GET/PATCH /user/me`、`GET/PATCH /user/settings`。

**前置**：2.2

**输出**：
- `UserController`
- `UserSettingsService`
- DTO 映射（手机号脱敏）

**验收**：修改 `daily_new_words_goal=30`，重新 GET 返回 30。

**耗时**：0.5 天

---

### 任务 2.4 — 验证码 + 手机号注册/登录

**目标**：`/auth/send-code`、`/auth/login-by-code`。

**输出**：
- `VerificationCodeService`（开发环境直接返回 `123456`，生产对接阿里云/腾讯云短信）
- Redis 存验证码（TTL 5 分钟）
- 频率限制（同一手机号 60 秒内只能发一次）

**验收**：请求发送验证码 → 返回成功；用 `123456` 登录成功。

**耗时**：0.5 天

---

## 阶段三：词库 + 核心学习（约 3 天）

### 任务 3.1 — 词库查询接口

**目标**：`GET /words/levels`、`/words/topics`、`/words/{id}`、`/words/search`、`/words/download`。

**输出**：
- `WordController`
- `WordService`
- `LevelService`（带 Redis 缓存）
- 下载接口支持 gzip 响应

**验收**：
- `GET /words/levels` 返回 10 个等级
- `GET /words/download?level=CET4` 返回种子数据中的 CET4 词条（20 条）

**耗时**：0.5 天

---

### 任务 3.2 — 艾宾浩斯调度器 + 单测

**目标**：`EbbinghausScheduler` 及其单测。

**输出**：
- `EbbinghausScheduler.schedule(stage_before, result, now)` 实现
- `EbbinghausSchedulerTest`（至少 10 个测试用例，覆盖：
  - stage 0 → 1（首次学习）
  - 连续答对 1→2→3→...→9
  - stage 5 答错 → 4
  - stage 1 答错 → 1（不低于 1）
  - stage 9 答对 → 9（不超过 9）
  - justMastered 触发时机
  - skip 的行为
  - 时间计算正确性（stage 3 的 next_review 正好是 12 小时后等）

**验收**：`./mvnw test -Dtest=EbbinghausSchedulerTest` 全绿。

**耗时**：0.5 天

---

### 任务 3.3 — 答题接口（核心）

**目标**：`POST /study/answer`、`/study/answer-batch`、`/study/reset`、`/study/mark-mastered`。

**前置**：3.2

**输出**：
- `StudyController` + `StudyService`
- `UserWordProgressMapper`
- `StudyLogMapper`
- `WrongWordService`（答错时 upsert）
- 事务边界：update progress + insert log + upsert wrong_word 一个事务

**验收**：
- 模拟一个用户连续答对同一个词 9 次，progress.stage=9, mastered_at 非空
- 答错中间某次，stage 回退 1 级
- batch 上报 3 条记录，按 client_ts 排序处理

**耗时**：1 天

---

### 任务 3.4 — 今日计划接口

**目标**：`GET /study/today?level=CET4`。

**前置**：3.3

**输出**：
- `TodayPlanService`
- SQL：到期复习词 + 未学新词
- Redis 缓存今日计划 10 分钟

**验收**：
- 新用户首次调用，返回 20 个新词 + 0 个复习词
- 答对某词后重新调用，该词不在列表
- 把 next_review_at 改为过去时间，该词出现在复习列表

**耗时**：0.5 天

---

### 任务 3.5 — 错题本接口

**目标**：`/wrong-words` 的 list / review / resolve。

**输出**：
- `WrongWordController` + `WrongWordService`

**验收**：答错一个词，`GET /wrong-words` 返回 1 条；连续答对 3 次后 resolved=1。

**耗时**：0.5 天

---

## 阶段四：Web 前端 MVP（约 3 天）

### 任务 4.1 — Web 项目脚手架

**目标**：`frontend-web/` 完整目录，能 `pnpm dev` 启动，首页显示 "Hello VocabMaster"。

**输出**：
- `package.json`、`vite.config.ts`、`tsconfig.json`
- Vue Router + Pinia + Element Plus 集成
- `api/request.ts` axios 封装
- `stores/user.ts` 基础框架
- 路由守卫

**验收**：`pnpm dev` 后访问 `http://localhost:3000`，未登录自动跳 `/login`。

**耗时**：0.5 天

---

### 任务 4.2 — 登录/注册页

**目标**：`/login`、`/register`，邮箱+密码 走完全流程。

**输出**：
- `views/auth/Login.vue`、`Register.vue`
- 调用 `authApi.register` / `authApi.login`，成功后跳 `/dashboard`

**验收**：注册一个新账号成功跳首页；退出后能用该账号登录。

**耗时**：0.5 天

---

### 任务 4.3 — 等级选择 + 首页

**目标**：首次登录显示等级选择页；已选等级显示今日计划概览。

**输出**：
- `views/levels/LevelSelection.vue`（多选，存到 `user_settings.active_levels`）
- `views/dashboard/Dashboard.vue`（显示当前等级、今日新词/复习数、连续天数、打卡按钮）

**验收**：选 CET4，首页显示"今日 20 新词 / 0 复习"。

**耗时**：0.5 天

---

### 任务 4.4 — 单词卡片 + 学习会话

**目标**：实现核心学习流程：翻卡、认识/不认识、自动下一张。

**输出**：
- `components/WordCard.vue`（含翻面动画）
- `views/study/StudySession.vue`
- 集成 TTS（点击喇叭播音）
- 答题结束展示 `/study/done` 统计页

**验收**：手动学 20 个词，观察后端 `user_word_progress` 表有对应 20 条记录，stage 全部为 1。

**耗时**：1 天

---

### 任务 4.5 — 用户设置页

**目标**：`/settings` 能修改每日目标、偏好口音、主题、活跃等级。

**输出**：`views/settings/UserSettings.vue`

**耗时**：0.5 天

---

## 阶段五：Uni-app 前端 MVP（约 3 天）

### 任务 5.1 — Uni-app 脚手架

**目标**：`frontend-uniapp/` 能跑通 H5（`pnpm dev:h5`）和微信小程序（HBuilderX 运行）。

**输出**：
- 完整 `pages.json`、`manifest.json`
- tabBar 三个页面空壳：今日 / 报表 / 我的
- `utils/request.ts`、`utils/platform.ts`、`utils/storage.ts`

**耗时**：0.5 天

---

### 任务 5.2 — 登录页（多端）

**目标**：
- H5/Android：邮箱+密码登录
- 微信小程序：`wx.login` 一键登录

**输出**：
- `pages/auth/login.vue`
- 微信端调 `/auth/login-wechat`

**验收**：微信小程序内点"微信一键登录"进入首页；H5 用邮箱登录。

**耗时**：1 天

---

### 任务 5.3 — 学习流程（与 Web 对齐）

**目标**：单词卡片 + 学习会话，与 Web 端行为一致。

**输出**：
- `components/word-card/word-card.vue`
- `pages/study/session.vue`
- TTS 跨端适配（小程序走 uni.createInnerAudioContext + 音频 URL，Android 走 plus.speech）
- 离线队列（`utils/storage.ts` 的 LocalTable）

**验收**：微信小程序里学 10 个词；断网情况下再学 5 个（进队列），恢复网络后自动上报。

**耗时**：1.5 天

---

## 阶段六：增强功能（约 2 天）

### 任务 6.1 — 测试模式（拼写 + 选择题 + 听写）

**目标**：`/test` 系列接口 + 前端三种测试页。

**输出**：
- Java 侧：`TestController` + `TestService`（生成题目 + 判分）
- Web 侧：`SpellingTest.vue`、`ChoiceTest.vue`、`ListeningTest.vue`
- Uni 侧：同上

**验收**：20 道拼写题全部做完，显示总分。

**耗时**：1 天

---

### 任务 6.2 — 打卡 + 连续天数 + 成就

**目标**：`/checkin` 接口 + 前端打卡日历 + 成就页。

**输出**：
- `CheckinService`（幂等打卡、更新 streak、触发成就检查）
- `AchievementService`
- 前端日历组件

**验收**：连续 7 天打卡后出现"坚持7天"成就。

**耗时**：0.5 天

---

### 任务 6.3 — 学习报表 + 遗忘曲线

**目标**：`/stats` 系列接口 + 前端报表页。

**输出**：
- `StatsController` + `StatsService`（SQL 聚合）
- Web: `ForgettingCurve.vue`（ECharts）、`StatsOverview.vue`
- Uni: 同上（uCharts）

**验收**：能看到本周学习热力、等级掌握率、某个词的遗忘曲线。

**耗时**：0.5 天

---

## 阶段七：同步 + 部署（约 1 天）

### 任务 7.1 — 多端同步接口

**目标**：`/sync/pull`、`/sync/push`。

**输出**：
- `SyncController` + `SyncService`
- 冲突解决逻辑（见 `docs/04-ebbinghaus-algorithm.md`）

**验收**：Web 端学 5 个词 → Uni 端 pull 能看到进度；Uni 端答题 → Web 端刷新看到。

**耗时**：0.5 天

---

### 任务 7.2 — 生产部署 + 压测

**目标**：部署到生产服务器，通过基础压测。

**输出**：
- 服务器 Nginx + SSL 配置好
- CI/CD 流水线跑通
- k6 压测通过（500 并发、P95 < 500ms）

**耗时**：0.5 天

---

## 阶段八：Python 后端（可选，与 Java 主线并行）

如果需要 Python 备选后端，整个流程重做一遍：

- 8.1 FastAPI 脚手架（0.5 天）
- 8.2 通用模块（错误码、JWT、AES、中间件）（0.5 天）
- 8.3 认证模块（1 天）
- 8.4 词库 + 学习（1 天）
- 8.5 艾宾浩斯算法移植 + 单测（0.5 天）
- 8.6 测试 / 统计 / 打卡（1 天）
- 8.7 Docker 化 + 部署（0.5 天）

预计 5 天。

---

## 阶段九：v1.1 增量（不在 MVP 内）

- LLM 辅助生成例句、选择题干扰项
- 家长端监控面板
- 社交功能（好友、排行榜）
- iOS App Store 上架
- 管理后台完整化（词库审核工作流、用户统计看板）
- 真实词库 ETL（执行 `docs/09-word-data-sourcing.md` 全流程）
- FSRS 算法接入（保留 stage UI，后台算法切换）

---

## 执行策略建议

### 给 Claude Code 的指示

每次会话开始时：
1. 读 `CLAUDE.md` 了解项目全貌
2. 读对应阶段的 `docs/` 文档
3. 按任务顺序执行，每完成一个任务暂停，等用户验收
4. 不跳任务、不跨模块改动
5. 遇到规格未覆盖的细节，先查 `CLAUDE.md` FAQ，再查 `docs/`，再询问用户

### 给开发者的检查清单

- [ ] 阶段一全部完成后，本地能起 docker + mysql + redis + java backend
- [ ] 阶段二完成后，能用 curl/Postman 完成完整注册登录流程
- [ ] 阶段三完成后，能模拟一个用户完整学 + 复习某个等级词汇
- [ ] 阶段四完成后，Web 端能独立工作
- [ ] 阶段五完成后，Uni 端（至少 H5 + 微信小程序）能独立工作
- [ ] 阶段六完成后，核心功能齐全
- [ ] 阶段七完成后，可上线 beta

### 风险提示

- **小程序审核**：微信小程序涉及教育内容需"教育-学历"或"教育-在线教育"类目，申请周期 1-2 周，建议阶段一就开始申请
- **Android 应用商店**：主流市场（华为/小米/OPPO/vivo/应用宝）各自审核规则不同，MVP 阶段可先做内部分发（APK 直装）
- **词库版权**：ECDICT MIT 协议可商用，但课标词表若用了出版社的电子版需注意；MVP 阶段尽量用公共来源
- **TTS 音频版权**：有道 CDN 链接生产不建议长期依赖，排期内切换到 Azure/自建合成
