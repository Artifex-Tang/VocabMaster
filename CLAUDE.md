# CLAUDE.md — 项目主指令

> 本文件是 Claude Code 进入本项目时读取的**主指令**。每次新会话开始先读此文件，再按需读取 `docs/` 下的专题文档。

## 项目代号

**VocabMaster** — 基于艾宾浩斯遗忘曲线的多端背单词应用。

## 核心信息

- **产品形态**：Web（PC 浏览器） + 微信小程序 + Android APP，三端共用一套后端
- **后端主选**：Java 21 + Spring Boot 3.5.x + MyBatis-Plus + MySQL 8.x + Redis 7.x
- **后端备选**：Python 3.12 + FastAPI + SQLAlchemy 2.x + MySQL 8.x + Redis 7.x
- **前端 Web**：Vue 3 + Vite + Pinia + Element Plus
- **前端移动**：Uni-app + Vue 3（一套代码编译出小程序 + Android APK + H5）
- **部署**：Docker Compose（开发）+ Nginx + 后端容器 + MySQL + Redis
- **用户规模目标**：DAU 万级，MySQL 单实例 + Redis 缓存足够

## 关键业务规则（最容易出错，优先记住）

1. **艾宾浩斯九阶段复习**：5 分钟 → 30 分钟 → 12 小时 → 1 天 → 2 天 → 4 天 → 7 天 → 15 天 → 30 天。单词答对 stage+1，答错 stage 回退至 max(1, stage-1)，走完 9 阶视为"已掌握"。详见 `docs/04-ebbinghaus-algorithm.md`。
2. **词库按等级隔离**：十个等级（KET/PET/FCE/CAE + 小学/初中/高中/大学四级/大学六级/专业八级），**同一单词在不同等级是不同记录**（释义深度不同），不做去重合并。
3. **学习进度按「用户 + 单词」维度存储**，不以等级为主键。一个用户可同时学多个等级。
4. **多端进度同步**：所有学习操作都实时同步到服务端，本地只做缓存和离线队列。
5. **离线缓存**：下载一个等级的词库到本地后即可离线学习，复习结果进本地队列，联网后批量上报。
6. **时区**：所有时间字段存 UTC，展示时按用户时区（默认 `Asia/Shanghai`）换算。

## 目录结构（规划）

```
vocabmaster/
├── CLAUDE.md                    # 本文件
├── docs/                        # 规格文档（12 份）
│   ├── 00-README.md             # 项目总览
│   ├── 01-architecture.md       # 架构 + 模块划分
│   ├── 02-database-design.md    # 数据库设计
│   ├── 03-api-specification.md  # API 规范
│   ├── 04-ebbinghaus-algorithm.md
│   ├── 05-backend-java.md       # Java 后端实现
│   ├── 06-backend-python.md     # Python 后端实现（备选）
│   ├── 07-wordmate-web.md       # Web 端
│   ├── 08-wordmate-mini.md    # 小程序 + Android
│   ├── 09-word-data-sourcing.md # 词库采集
│   ├── 10-deployment.md         # 部署
│   └── 11-roadmap.md            # 开发路线图
├── sql/
│   └── init.sql                 # MySQL 初始化脚本
├── seed/
│   └── words_sample.csv         # 种子词库（每级 20 词）
├── prompts/                     # 针对性开发 prompt
│   ├── backend-java.md
│   ├── backend-python.md
│   ├── web.md
│   └── uniapp.md
├── backend-java/                # Java 后端代码（待生成）
├── backend-python/              # Python 后端代码（待生成）
├── wordmate-web/                # Web 前端代码（待生成）
├── wordmate-mini/             # Uni-app 代码（待生成）
└── docker-compose.yml           # 本地开发环境
```

## Claude Code 工作原则

1. **先读文档再动手**：接到任务先找对应的 `docs/` 文件，严格按规格实现，不要自由发挥。
2. **小步提交**：每完成一个模块（比如一个 Controller + Service + Mapper）就停下来让用户确认。
3. **一致性高于优雅**：API 命名、错误码、字段命名三端必须完全一致，参考 `docs/03-api-specification.md`。
4. **遵循标准 Spring Boot 分层规范**：Controller → Service → Mapper/Repository 三层清晰分离，包名按业务模块（`auth/user/word/study/...`）而非技术（`controller/service/...`）组织。详见 `docs/05-backend-java.md`。
5. **遇到不确定的需求**：先查 `docs/`，再查本文件末尾的"FAQ"，都没有的询问用户，不要猜。

## 推荐开发顺序

参考 `docs/11-roadmap.md`。粗略是：

1. 搭数据库 + 种子数据（半天）
2. Java 后端骨架 + 认证模块（1 天）
3. 词库 + 学习进度 API（2 天）
4. Web 端 MVP（2 天）
5. Uni-app 端 MVP（3 天）
6. TTS/听写/拼写测试（2 天）
7. 报表/可视化（2 天）
8. 离线缓存 + 同步（2 天）
9. 部署 + 压测（1 天）

总计约 3 周一人独立完成 MVP。

## 历史会话沉淀（来自 2026-05-30 ~ 2026-06-02 的 9 个会话）

> 以下内容从项目历史会话中归纳，作为跨会话的持久知识。避免重复踩坑。

### 已确定的业务调整

1. **词库数据源**：主用 ECDICT（skywind3000/ECDICT，MIT 协议）SQLite 提取，辅以 CMU（音标）、Wiktionary（例句）、WordNet（同义词）、COCA/BNC 词频。已产出 42,531 条词汇覆盖 10 个等级。
2. **等级分布**：PRIMARY 800 / KET 1,445 / JUNIOR 1,597 / PET 2,904 / SENIOR 3,674 / CET4 3,837 / FCE 5,000 / CET6 5,396 / CAE 7,500 / TEM8 10,378。
3. **主题分类**：从 20 类调整为 **31 类**（参考 Cambridge Vocabulary in Use + Oxford Word Skills），用 DeepSeek-V3 API 完成 42K 词分类。旧方案（GLM-4-Flash）受速率限制不可用。
4. **音频方案**：开发用有道 CDN（`dict.youdao.com/dictvoice?audio={word}&type=1/2`），生产用 Azure TTS 批量生成存自有 CDN。前端兜底用 Web Speech API。
5. **配图方案**：有 `image_url` 的优先显示图片，无则 emoji 兜底。9,765 词有 Wikimedia 图片，已下载到本地 `backend-java/static/images/words/`，Nginx 挂载 serve。抽象词（action/description/abstract 等主题）无合适图片，emoji 兜底合理。
6. **Python 后端**：确认不纳入 MVP，标记为 Phase 8 可选项。
7. **微信小程序**：已开发完成并通过全量测试（2026-06-02）。上线进度（2026-07-23）：域名 `vocab-master.cn` + HTTPS + ICP 备案✓（`京ICP备2026044898号-1`），公安联网备案已提交待审，小程序备案 + 提审待办。**个人主体须选"工具"类目**（教育类要资质）。
8. **审核状态**：`audit_status` 状态机 0=待审核 / 1=审核通过 / -1=已下架，MVP 跳过审核直接设 1。
9. **测试 UX 增强**：默认数据源从"待复习"改为"全部"；选择题可改答案再提交；拼写测试支持中/英文释义切换；所有测试支持前后导航 + 跳过 + 随时交卷。
10. **产品免费**：MVP 无付费功能。

### 已确定的架构决策

1. **后端技术栈（已落地）**：Spring Boot 3.5.4 + Java 21 + MyBatis-Plus 3.5.7 + MySQL 8.x + Valkey 8（替代 Redis）。依赖：jjwt 0.12.5、Bouncy Castle 1.78.1（AES-256-GCM）、Hutool 5.8.26、MapStruct 1.5.5.Final、Flyway（数据库迁移）、springdoc-openapi 2.5.0。
2. **前端 Web（已落地）**：Vue 3 + Vite 5.4 + Pinia + Element Plus + ECharts + Dexie（IndexedDB 离线存储）+ MockJS + unplugin-auto-import/components。Dev 端口 3100。
3. **前端 Uni-app（已落地）**：@dcloudio/uni-app 3.0.0-alpha + Vue 3 + Vite 5.1 + Pinia，支持 H5/微信小程序/Android。需 `index.html` 入口模板 + `sass-embedded` devDependency。
4. **Docker 多阶段构建**：后端 maven:3.9-eclipse-temurin-21 → eclipse-temurin:21-jre-alpine；前端 node:20-alpine + pnpm → nginx:1.27-alpine。非 root 用户运行，G1GC + MaxRAMPercentage=75%。
5. **Nginx 双层架构**：`deploy/nginx.conf`（主网关，限流 30r/s 普通/5r/s 认证 + gzip + 安全头）；`deploy/web-nginx.conf`（前端容器内，SPA fallback + 静态资源 30 天缓存）。
6. **Docker Compose profiles**：默认只起 MySQL + Redis，`backend`/`frontend`/`full`/`production` profile 按需启用。
7. **CI/CD**：GitHub Actions（`.github/workflows/ci.yml`），4 并行任务（backend / web / uniapp / integration），MySQL 8 + Valkey 8 作为 CI 服务。
8. **ETL 工具链**：`tools/word-etl/`，4 步流水线：01 下载 ECDICT → 02 按等级提取 → 03 转换 → 04 导入数据库。
9. **离线同步**：`useOfflineSync` composable，在线走 API，离线入 IndexedDB（Dexie），重连后批量 flush。
10. **JWT 双 token**：access token + refresh token，Redis 存 refresh 白名单 + access 黑名单（登出用）。
11. **安全设计**：手机号 AES-256-GCM 加密存储 + SHA-256 hash 做唯一索引；JWT 只暴露 uuid 不暴露自增 id；无数据库外键，应用层保证完整性。
12. **2C2G 部署方案**：Java 堆 -Xmx384m，MySQL innodb_buffer_pool=256M + maxmemory 128MB Redis，总计约 850MB。本地编译 JAR → SCP 上传 → Docker 运行，服务器不需要 Maven/Node。
13. **代理方案**：用户用 Clash TUN 模式全局透明代理，Dockerfile/docker-compose 不硬编码 proxy 环境变量。
14. **图片本地化**：`scripts/download_images.py` 下载 Wikimedia 图片到 `backend-java/static/images/words/{word}.jpg`，DB `image_url` 改为 `/images/words/{word}.jpg`。Nginx volume 挂载 + `location /images/` alias。前端展示优先级：`image_url` → `emoji` 兜底。
15. **例句生成**：`scripts/generate_examples.py` 用 DeepSeek-V4-Flash 批量生成英文例句（36K 词），写入 `example_en` 字段。成本约 ¥5。

### 代码规范与踩坑记录

1. **"手机仿真测试"= 微信开发者工具模拟器**：用户说"手机仿真测试"/"端到端手机模拟仿真测试"时，指的是微信开发者工具里的模拟器（miniprogram-automator），**不是** Playwright headless 浏览器。两者必须区分清楚。H5 Playwright 测试≠微信模拟器测试。
2. **API 参数命名**：前端发 `snake_case`（`level_code`, `word_id`），后端 `@RequestParam("snake_case") Type camelCaseName` 显式映射。所有 GET 查询参数必须加别名。
3. **统一响应格式**：`R<T>` 封装（code / msg / data / request_id），分页用 `PageResult<T>`（items / total / page / page_size）。
4. **Lombok `@Getter` 命名**：字段名 `errorCode` 对应 `getErrorCode()`，不是 `getCode()`。
5. **MyBatis-Plus 约定**：`@TableLogic(value = "NULL", delval = "now(3)")` 软删除；`@TableField(fill = FieldFill.INSERT/INSERT_UPDATE)` 自动填时间；`@TableName` 显式表名。
6. **Lombok + MapStruct 联合**：maven-compiler-plugin 需配置 `lombok-mapstruct-binding 0.2.0` 解决注解处理器冲突。
7. **Redis 反序列化**：`RedisTemplate` JSON 序列化读回来是 `LinkedHashMap` 而非领域对象，需 `ObjectMapper.convertValue(raw, TargetClass.class)` 兼容。
7. **`@RateLimit` 注解**：必须有 `key()` 属性支持自定义限流 key 前缀。
8. **`EbbinghausScheduler.INTERVALS_HOURS`**：必须 `public static final`，跨包访问。
9. **前端日期格式**：后端 `LocalDateTime` Jackson 格式 `yyyy-MM-dd'T'HH:mm:ss`，前端 `nowIso()` 必须去掉毫秒和 `Z` 后缀匹配。
10. **`crypto.randomUUID`**：HTTP 环境不可用（需 HTTPS 或 localhost），前端需 `Math.random()` 降级。
11. **生产环境 Swagger**：`application-prod.yml` 中 `springdoc.swagger-ui.enabled` 和 `api-docs.enabled` 设 false。
12. **Maven/npm 镜像**：后端 `settings.xml` 阿里云 Maven 镜像，前端 `.pnpmrc` 国内 npm 镜像源。
13. **Git 忽略**：ECDICT 原始数据（大文件 zip）、ETL 输出的 SQL 导入文件不提交；CSV 输出保留提交。
14. **JVM 编码**：服务器默认 GBK，必须 `-Dfile.encoding=UTF-8`。
15. **Docker Compose `JAVA_OPTS`**：不要用 `${JAVA_OPTS:--Xmx384m}` 嵌套变量语法，会泄漏 `}` 字符，直接硬编码值。
16. **选择题 UI**：用原生 `<div>` 而非 `el-button` 做选项，后者内部 DOM 干扰文本对齐。
17. **`related_words` JSON 字段**：已扩展包含 `synonyms`、`antonyms`、`derived` 三个数组。TypeScript 接口和后端 DTO 须同步。
18. **音标字体**：引入 Gentium Plus / Charis SIL / DejaVu Sans web font 替代系统默认。
19. **Web 端 dev server**：端口 3100（非 3000），通过 `VITE_USE_MOCK` 切换 mock/真实后端。
20. **login.vue 错误处理**：`handleLogin` 的 `try/finally` 缺少 `catch` 块，API 失败时静默吞错无反馈。必须加 `catch` 显示 `uni.showToast({ title: msg, icon: 'none' })`。
21. **manifest.json permission**：`scope.userInfo` 不是微信小程序 `app.json` 合法的 permission 键，会报 "无效的 app.json permission" 警告。改为 `scope.userLocation` 或移除。
22. **miniprogram-automator 中文路径**：Windows bash 环境下 `child_process.spawn` 调用含中文的 cliPath 会乱码。解决：用 `e2e/launch-devtools.js` 通过 HTTP API（`/v2/auto`）启用自动化，绕过 CLI spawn。
23. **miniprogram-automator `element.input()`**：只对 `<input>` 和 `<textarea>` 标签有效，参数是字符串 value。按钮点击用 `element.tap()`，非 input 元素调 `input()` 会报 "not a function"。
24. **miniprogram-automator `page.callMethod()`**：仅对原生 Page 的 `Page({methods: {}})` 有效。uni-app `<script setup>` 编译后方法不暴露。改用 `element.tap()` 触发按钮。
25. **Vue `new Map()` 非响应式**：`const answerMap = new Map()` 不触发 computed 更新。必须 `const answerMap = reactive(new Map())`。同理 `questionStart = Date.now()` 需改成 `ref(Date.now())`。
26. **Web 路由守卫测试 URL**：测试未登录跳转时，`/study` 不匹配 `study/:level` 路由，会走 catch-all（public=true）导致不跳转。应使用 `/dashboard` 等真实受保护路由。
27. **Wikimedia 429 限流**：批量下载图片需 1.0s 延迟 + 429 重试指数退避。0.5s 不够。
28. **Python 脚本 Windows GBK**：print 语句含 Unicode 特殊字符（✓✗⚠）在 GBK 终端报错。用 ASCII 替代（[OK]/[FAIL]/[WARN]）。
29. **微信结构测试端口自动发现**：`wechat-test.js` 原硬编码端口 20288，实际 DevTools HTTP 端口不固定（如 12995）。改为调用 `CLI islogin` 自动发现端口，与 `wechat-user-journey.js` 一致。
30. **mp-weixin 不支持 ES 动态 `import()`**：`request.ts` 曾用 `await import('@/stores/user')` 解循环引用，mp-weixin build 把它编成 `await "字符串"` → `useUserStore` 为 undefined → **所有 API 请求静默失败**（登录/学习/测试全挂，H5 不受影响故难发现）。改静态 `import` + 运行时调用 useUserStore（ES 活绑定照样解循环引用 request↔user↔auth）。**任何 mp-weixin 代码禁用动态 import()。**
31. **storage.get 对非 JSON 字符串别 `catch{return null}`**：JWT `access_token` 按原始字符串存，`get` 对字符串走 `JSON.parse` 必失败，原 catch 返 null → token 读回 null → `isLoggedIn` 永远 false → **重启丢登录态**。catch 应返回原始值 `v`（把 `let v` 提到 try 外才够得着）。device_id 等字符串字段同理。

### 自动化测试（2026-06-02 完成全量测试，全端 100% 通过）

1. **单元测试**：Vitest 3.x，48 个测试覆盖 utils/stores/api/composables。命令 `pnpm test`。
2. **H5 手机仿真**：Playwright + Chromium，iPhone 15（393×852）+ Pixel 7（412×915）双视口。`e2e/mobile.spec.ts`（22 个）+ `e2e/login-flow.spec.ts`（9 个）= **31 个**覆盖全部 16 页面 + 登录表单填充验证 + tab 切换 + 无 JS 错误。命令 `npx playwright test`。
3. **Web E2E 测试**（2026-06-02 新增）：`wordmate-web/e2e/full-flow.spec.ts`，Playwright Desktop Chrome（1280×800），10 个用例覆盖登录页渲染、登录表单、首页、学习页、测试页、单词搜索、遗忘曲线、设置页、路由守卫（未登录跳转）、后端 API 连通。命令 `npx playwright test`（在 wordmate-web 目录）。Docker 全栈环境跑（localhost:3001）。
4. **微信小程序结构测试**：Node.js 脚本，19 个测试验证构建产物完整性。端口自动发现（通过 `CLI islogin` 命令解析，不再硬编码 20288）。命令 `node e2e/wechat-test.js`。
5. **微信小程序用户旅程测试**：`e2e/wechat-user-journey.js`，T1-T20 覆盖完整用户操作流程。**2026-06-25 升级为真鉴权**（provision 真账号→UI 登录→真 JWT，打本地 docker 后端），**39 通过 / 0 失败 / 1 info**。之前是假 token 结构覆盖（36/0/3）。靠 Pinia 内存 token（不 reLaunch 读 storage，规避坑 #31）。命令 `WX_AUTO_PORT=60640 node e2e/wechat-user-journey.js`（端口每跑+1 避死监听）。

**全端测试成绩（2026-06-02）**：
| 端 | 结果 | 命令 |
|----|------|------|
| Web Chrome | **10/10** | `cd wordmate-web && npx playwright test` |
| H5 仿真 | **40/40** | `cd wordmate-mini && npx playwright test` |
| 微信结构 | **19/19** | `cd wordmate-mini && node e2e/wechat-test.js` |
| 微信旅程（真鉴权） | **39/0/1** | `cd wordmate-mini && WX_AUTO_PORT=60640 node e2e/wechat-user-journey.js` |
5. **测试报告**：`docs/test-report-2026-06-02.md`。
6. **测试依赖**：vitest@3.2.6、@vue/test-utils、happy-dom、playwright@1.60、jest、ts-jest、miniprogram-automator@0.12.1、ws@8.21。
7. **uni-app H5 input DOM 结构**：uni-app H5 将 `<input>` 包裹在 `<uni-input>` 中，placeholder 在兄弟 `<div class="uni-input-placeholder">` 而非 `<input>` 上。Playwright 测试需通过 `uni-input` wrapper 定位。`fillUniInput()` 工具函数按 wrapper 内的 placeholder div 找到对应 `.uni-input-input` 后用 `nativeInputValueSetter` + `input`/`change` 事件触发 Vue 响应式。
8. **uni-app `<script setup>` 编译**：变量名被压缩（如 `tab`→`a`），`page.data('tab')` 不可用；方法不暴露为 `page.callMethod()`。微信小程序自动化中：用 `element.input()` 填写、`element.tap()` 点击按钮、`page.setData()` 修改状态、`page.data()` 取全部 data。
9. **微信小程序自动化启动器**：`e2e/launch-devtools.js`（移植自 FocusLab）。通过 `CLI islogin` 发现 DevTools HTTP 端口 → `/v2/open` 打开项目 → `/v2/auto` 启用自动化 WebSocket → `automator.connect()` 连接。解决 CLI 中文路径 spawn 编码问题。端口可通过 `WX_AUTO_PORT` 环境变量配置，默认 60616。
10. **uni-app H5 路由**：Playwright `getByText` 无法匹配 uni-app 自定义元素（`<uni-text>`），需用 `page.evaluate(() => document.body.innerText)` 或 CSS 选择器替代。
11. **微信开发者工具 CLI 端口**：默认非 9420，需在"设置→安全设置"查看实际端口。
12. **uni-app `uni.showToast` H5 DOM**：渲染在 `.uni-toast` 覆盖层中，不在 `document.body.innerText` 内。Playwright 检测 toast 需查询 `.uni-toast` 元素的 display/visibility/opacity。
13. **微信小程序 reLaunch vs switchTab**：`reLaunch` 不重新触发 `App.onLaunch`，Pinia store 的 `initFromStorage()` 不会再次执行。测试中注入 wx storage 后 `reLaunch` 到首页，路由守卫可能仍重定向到 login。这是"无后端"场景的已知限制。

### 已解决的问题（避免重复踩坑）

| 问题 | 根因 | 解决 |
|------|------|------|
| uni-app 构建 "missing entry module index.html" | 缺少 `index.html` 入口模板 | 创建标准 uni-app Vite 入口 HTML |
| uni-app "sass-embedded not found" | 缺少预处理器依赖 | `pnpm add -D sass-embedded` |
| Docker MySQL `word_bank` 为空 | 数据未导入 | `docker cp` + `LOAD DATA LOCAL INFILE` |
| MySQL `LOAD DATA LOCAL INFILE` 权限 | 默认关闭 | root 用户 `SET GLOBAL local_infile = ON` |
| 登录 400（`client_ts` 格式） | 前端 ISO 带毫秒+Z，后端 `LocalDateTime` 不匹配 | 前端 `nowIso()` 去毫秒+Z |
| 测试会话无效 ("测试 ID 无效或已过期") | Redis 反序列化返回 `LinkedHashMap` | `ObjectMapper.convertValue()` 兼容 |
| 前后端 502 Bad Gateway | Docker 网络不一致（vocab-network vs vocab-net） | 统一网络 + proxy_pass 目标 |
| `crypto.randomUUID` HTTP 不支持 | 需安全上下文 | `Math.random()` 降级 |
| 选择题无选项 | 后端 choices 在 `TestQuestion` 级而非 `prompt.options` | `QuestionPrompt` 增加 `options` 字段 |
| 听写测试无声音 | `speakOrFallback('')` 传空字符串 | 从 `prompt.word` 取单词文本 |
| 拼写测试无 TTS 兜底 | 只有 `new Audio().play()` 无 URL 时静音 | 引入 `useTts` composable |
| `test/availability` 400 | 后端 `levelCode` vs 前端 `level_code` | `@RequestParam("level_code")` |
| MySQL OOM Killer | 256MB 内存限制不够 | 增至 512MB |
| MySQL `sync-binlogs` 启动失败 | `--skip-log-bin` 与 `sync-binlogs=0` 冲突 | 删除 `sync-binlogs=0` |
| Java Dockerfile `}` 字符泄漏 | `JAVA_OPTS` 嵌套变量语法 | 硬编码值 |
| Docker Hub 镜像源 403（daocloud） | 镜像源失效 | 移除，直连 |
| 暗色主题不生效 | Element Plus 暗色模式初始化时序问题 | 修复初始化顺序 |
| LLM 分类 GLM-4-Flash 429 | 免费模型速率限制 | 切换 DeepSeek-V3 |
| Git push 缺 `workflow` scope | PAT 无 workflow 权限 | 重新生成 PAT |
| 登录页 API 失败无反馈 | `handleLogin` try/finally 无 catch | 加 catch + `uni.showToast` |
| `manifest.json` permission 警告 | `scope.userInfo` 非合法键 | 改为 `scope.userLocation` |
| automator spawn 中文乱码 | bash 环境 child_process 编码问题 | HTTP API `/v2/auto` 绕过 CLI spawn |
| uni-app input Playwright fill 无效 | uni-app 包裹 input，placeholder 不在 input 上 | 通过 `uni-input` wrapper 定位 + nativeInputValueSetter |
| `page.data('tab')` undefined | uni-app `<script setup>` 变量名压缩 | 取 `page.data()` 全量或跳过命名 key 断言 |
| `page.callMethod('handleLogin')` not exists | `<script setup>` 方法不暴露为 page method | 用 `element.tap()` 点击按钮 |
| 选择题第20题选不上、进度0/20 | `new Map()` 不触发 Vue computed 响应式 | 改 `reactive(new Map())` + `ref(Date.now())` |
| Web 路由守卫测试失败 | `/study` 不匹配 `study/:level`，走 public catch-all | 测试改用 `/dashboard` |
| 微信结构测试 2/19 失败 | 端口硬编码 20288，实际 DevTools 端口不固定 | `CLI islogin` 自动发现端口 |
| Wikimedia 批量下载 429 | 0.5s 延迟不够 | 延迟改 1.0s + 429 指数退避 |
| Python 脚本 GBK 编码错误 | Unicode 特殊字符（✓✗⚠）在 Windows GBK 终端报错 | 用 ASCII 替代 |

### 功能规划（待用户确认后再实施）

#### 自定义词库（Custom Word List）

用户可创建自定义词库，学习/测试时选为数据源。

**导入方式**：
1. **Excel/CSV 导入**：列 `word, zh_definition(可选), en_definition(可选)`，后端解析匹配 `word_bank` 已有词，匹配不到的入 `custom_words` 表
2. **手动选词**：从已有等级词库勾选加入自定义列表
3. **智能推荐**：根据当前等级 + 薄弱词自动组一批

**数据模型**：
```sql
custom_word_list (id, user_id, name, description, word_count, created_at)
custom_word_list_item (list_id, word_id)  -- 关联 word_bank.id
```

#### 单词小短文（Word Story）

从当前学习批次选 8-15 个词，LLM 生成小短文，单词在文中加粗高亮。附带中文翻译 + 阅读理解题。

**体裁**：文学故事 / 科普文章 / 新闻报道 / 日常对话 / 实用文体（邮件、日记、演讲）

**数据模型**：
```sql
word_story (id, title, content_en, content_zh, genre, level_code, word_ids JSON, created_at)
word_story_question (story_id, question, options JSON, answer)
```

**成本**：DeepSeek-V4-Flash ~¥0.0004/篇，1 万篇 ≈ ¥4。

**生成策略**：按等级×体裁预生成 + 同批词缓存复用（所有用户共享）。

**建议实施顺序**：
1. Phase 1：小短文预生成 + 阅读（1 天）
2. Phase 2：小短文阅读理解题（0.5 天）
3. Phase 3：自定义词库 Excel 导入（1 天）
4. Phase 4：自定义词库手动选词 + 智能推荐（1 天）

### 待办事项（当前未完成）

**高优先级：**
- [x] 全栈 Docker Compose 集成测试已通（CI integration job，run 28215677126，2026-06-26）
- [ ] `docs/11-roadmap.md` 进度未更新
- [ ] commit `2d0eb21`（OOM 修复）未 push
- [ ] WIP 未提交：`WordTopic.java`、`wordmate-web/src/api/word.ts`、`.env.development`、`V2__add_word_topic_image_type.sql`、`scripts/` 4 个 py 脚本、`backup/`
- [ ] V2 迁移 `V2__add_word_topic_image_type.sql` 未上云（云只 V1 baseline，云上无 `image_type` 列）——下次后端 redeploy 走 Flyway 自动应用，勿手动 ALTER
- [ ] 图片质量验收 + 同步 `image_url` 列到云 + sftp 13068 张图到云 `/images` 卷（本地 38213 已填，云 0，质量暂缓）
- [ ] `example_zh` 中文例句未生成（本地+云都 0）

**中优先级：**
- [ ] "待复习"/"错词本" 选项在用户无数据时应禁用
- [ ] 18 个残留旧字母分类词需重新分类
- [ ] 后端构建验证（修 3 个编译错误后未重新 `mvn package`）
- [ ] k6 压测脚本未实际运行（`scripts/k6-load-test.js`）
- [x] CI/CD 流水线已触发且全绿（backend/web/mini/integration 四 job，2026-06-26）
- [x] SSL 证书已上线（阿里云 DV，vocab-master.cn，2026-07-23；到期 2026-10-19 续签跑 `deploy/install_ssl.py`）
- [ ] API key 管理生产化（当前明文）
- [ ] 服务器升 4G（OOM 根治：1.6G 超售，autoheal+调内存只是抠门）

> 数据现状（2026-06-23 本地 `vocabmaster` 实测，42531 词）：`audio_url_uk`/`audio_url_us`/`emoji` 全满，`example_en` 42489，`image_url` 38213，`example_zh` 0。云上待同步项见 [[project_session_20260621]]。

**低优先级（后续阶段）：**
- [ ] Python 备选后端（Phase 8 可选）
- [ ] 报表/可视化完善
- [ ] 离线缓存 + 多端同步端到端验证
- [ ] uni-app 依赖版本统一（vue 3.4.21 vs 3.5.32 等）
- [ ] Dart Sass 弃用警告（legacy JS API，Dart Sass 2.0 移除）
- [ ] `docker-compose.yml` 的 `version: '3.8'` 已废弃需移除
- [ ] 词汇爬取需求（剑桥官网 + 国内课标词汇）
- [ ] 微信小程序真机交互测试（需手机扫码）
- [ ] `wordmate-mini/src/manifest.json` 未提交修改（appid + permission）

## FAQ

**Q: 用户注册用手机号还是邮箱？**
A: 都支持。Web 端默认邮箱，小程序端默认微信 OAuth，Android 端默认手机号。见 `docs/03-api-specification.md#认证`。

**Q: TTS 用哪家？**
A: 前端优先用浏览器 / 小程序 / Android 原生 TTS（Web Speech API / uni.createInnerAudioContext + 有道 API / TextToSpeech）。服务端不做 TTS，只返回单词音频 URL（从有道或 Cambridge 词典 CDN）。

**Q: 配图怎么来？**
A: 种子数据用 emoji 兜底。正式词库从 Unsplash API 按词搜图，CDN 缓存 URL 存库。版权敏感场景可换成 Pixabay / Pexels。

**Q: 遗忘曲线可视化用什么图？**
A: 指数衰减曲线 + 用户实际复习点叠加。推荐 ECharts（Web）+ uCharts（Uni-app）。

**Q: 管理后台做吗？**
A: MVP 阶段做一个简版，就用 Web 前端同一个项目加 `/admin` 路由和角色守卫，不单独开项目。

---

下一步：读 `docs/00-README.md` 了解完整产品设计，再按任务需要读专题文档。
