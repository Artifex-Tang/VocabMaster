# VocabMaster Java 后端 — 开发日志

> 记录本次会话中后端从零到完成的完整执行过程，包括每步决策、遇到的问题和修复。

---

## 开发概况

- **开始时间**：2026-04-19
- **技术栈**：Java 21 / Spring Boot 3.5.4 / MyBatis-Plus 3.5.7 / MySQL 8 / Redis 7
- **最终文件数**：127 个 `.java` 文件
- **覆盖模块**：认证、用户、词库、学习、测试、统计、打卡、错题本、同步、管理后台

---

## 步骤 0 — 工程骨架

### 完成内容
- `pom.xml`：确定依赖版本
- `application.yml` / `application-dev.yml` / `application-prod.yml`
- `.env` 开发环境变量文件
- Flyway 迁移脚本 `V1__init.sql`（15 张表 + 字典数据）

### 关键决策

**spring-dotenv 而非硬编码**
- 使用 `me.paulschwarz:spring-dotenv:4.0.0` 加载 `.env`
- `application-dev.yml` 中：`spring.config.import: "optional:dotenv:./"`
- 初始写成 `optional:file:.env[.properties]`（错误），后修正

**Bouncy Castle 独立引入**
- 不依赖 Hutool 的 BC 包，单独引入 `bcprov-jdk18on:1.78.1`
- 原因：Hutool BC 包版本落后，AES-256-GCM 需要 JCE Provider 精确控制

**MyBatis-Plus Spring Boot 3 专用 Starter**
- 使用 `mybatis-plus-spring-boot3-starter`，不能用旧版 `mybatis-plus-boot-starter`
- 原因：Spring Boot 3 移除了 `javax.*`，旧 starter 不兼容

**软删除字段用 `@TableLogic(value="NULL", delval="now(3)")`**
- 字段级注解而非全局配置，避免 `NOW()` 函数调用被引号包裹导致 SQL 错误

---

## 步骤 1 — 公共基建

### 完成内容
- `R<T>` 统一响应（含 `requestId`）
- `ErrorCode` 枚举（全量错误码，按业务域分段：10xxx/20xxx/30xxx/40xxx/50xxx/90xxx）
- `BizException` + `GlobalExceptionHandler`（7 种异常）
- `@RateLimit` + `RateLimitAspect`（Redis INCR+EXPIRE 计数器）
- `JwtUtil`（HS256，access 2h + refresh 7d）
- `JwtAuthenticationFilter`（黑名单检测 + UserContext + SecurityContextHolder）
- `PhoneCryptoUtil`（AES-256-GCM，12 字节随机 IV，Base64(IV‖密文)；SHA-256 哈希用于唯一索引）

### 关键决策

**JWT 类型字段**
- payload 中加 `type: access / refresh`，filter 中只接受 `type=access`
- 防止用 refreshToken 直接访问业务接口

**手机号存储两列**
- `phone`：AES-256-GCM 加密存储（可解密用于展示脱敏）
- `phone_hash`：SHA-256 哈希（用于唯一索引和登录查询，不可逆）

**AES key 派生**
- `@PostConstruct` 中对配置字符串做 SHA-256，始终得到 32 字节，容忍任意长度配置值

**`spring-boot-starter-aop` 需显式引入**
- `RateLimitAspect` 使用 AspectJ，但父 POM 不自动传递 AOP 依赖，需手动加

---

## 步骤 2 — 数据层

### 完成内容
- 15 个 Entity（`user / user_auth / user_settings / word_bank / level / word_topic / user_word_progress / study_log / wrong_word / checkin / user_streak / achievement / user_achievement / sync_token / admin_log`）
- 15 个 Mapper（继承 `BaseMapper<T>`）
- 3 个 MyBatis XML：`WordBankMapper.xml`、`StudyLogMapper.xml`、`WrongWordMapper.xml`

### 关键决策

**`UserAchievement.achievedAt` 填充问题**
- 初始用 `@TableField(fill = FieldFill.INSERT)`，但 `MetaObjectHandler` 只填充名为 `createdAt`/`updatedAt` 的字段
- 修正为 `@TableField(insertStrategy = FieldStrategy.NOT_NULL)`，让 MySQL `DEFAULT CURRENT_TIMESTAMP` 兜底

**`word_bank` 唯一键为三列**
- `uk_level_word (level_code, word_lower, deleted_at)`
- 软删除后允许重新添加同一单词，导入时捕获 `Duplicate entry` 跳过而非失败

**`StudyLogMapper.xml` 时区转换**
- `dailySummary` 按 `Asia/Shanghai` 日期聚合，使用 `CONVERT_TZ(created_at, '+00:00', '+08:00')`
- 所有存储仍是 UTC，只在统计展示时转换

---

## 步骤 3 — 认证模块

### 完成内容
- `AuthController`：9 个端点（注册/验证码/登录/微信/Apple/刷新/登出/重置密码）
- `AuthService`：bcrypt cost=12，JWT 双 Token 签发/吊销，refreshToken 存 Redis
- `VerificationCodeService`：TTL 5 min，分钟级（1次/分钟）+ 日级（10次/天）双重限流
- `OAuthService`：微信 `jscode2session`，Apple `identity_token` JWT 解码

### 关键决策

**登出实现**
- `accessToken` 加入 Redis 黑名单，TTL = token 剩余有效期
- 同时删除 Redis 中的 `refreshToken`
- Filter 每次请求都检查黑名单，无需修改 JWT 本身

**刷新 Token 防重放**
- Redis 存储的 refreshToken value 必须与请求值完全匹配才换发
- 换发后旧 refreshToken 立即失效，新 refreshToken 覆盖写入

**Apple OAuth**
- 生产环境需用 Apple 公钥验签 `identity_token`，MVP 阶段仅做 JWT decode（TODO 标注）

---

## 步骤 4 — 词库模块

### 完成内容
- `LevelService`：等级/主题列表（Redis 永久缓存，主动失效）
- `WordService`：按 ID / level+word 查询、前缀搜索、批量下载（`since` 增量）
- `WordImportService`：启动时异步检测空表自动导入种子 CSV；支持管理员手动上传
- `WordController`：6 个端点

### 关键决策

**`since` 支持两种格式**
- `yyyyMMdd`（如 `20260401`）和 ISO 8601，两种都解析，适配移动端和 Web 端不同习惯

**CSV 导入频率归一化**
- 种子数据 `frequency` 字段使用 1-10 评分，导入时 `> 1` 则除以 10，统一到 0-1 区间

**`LevelService` 缓存策略**
- 永久缓存（不设 TTL），词库变更后调 `evictCache()` 主动删除
- 原因：等级/主题数据极少变化，永久缓存减少 DB 压力

---

## 步骤 5 — 学习核心模块

### 完成内容
- `EbbinghausScheduler`：九阶段固定间隔纯函数状态机
- `TodayPlanService`：今日计划（复习 + 新词），Redis 缓存 10 分钟
- `StudyService`：答题事务，支持单次和批量离线同步
- `StudyController`：5 个端点
- `EbbinghausSchedulerTest`：17 个单元测试

### 艾宾浩斯算法实现细节

```
INTERVALS_HOURS = [5/60, 0.5, 12, 24, 48, 96, 168, 360, 720]
索引               0     1    2   3   4   5   6    7    8
对应              stage 1→2  2→3  ...                9 保留期
```

**状态转移规则**
- `correct`：`stage_new = min(stage + 1, 9)`
- `wrong`：`stage_new = max(1, stage - 1)`（下限 1，避免回到"未学过"状态）
- `skip`：stage 不变（`max(1, stage)` 保证已学词留在复习循环），next_review_at 延后 10 分钟

**`mastered` 触发条件**
- `stageBefore < 9 && stageAfter == 9`（仅首次升到 9 时触发）
- `stage 9 答错` → 回退到 8，清除 `mastered_at`

**多端冲突检测**
- `client_ts <= progress.client_updated_at` → 拒绝（抛 `CLIENT_TS_TOO_OLD`）
- 批量接口：冲突跳过，不中断整批

**TodayPlanService 新词查询**
- 先查 `findLearnedWordIds()`（所有 stage > 0 的词 ID）
- 传给 `WordBankMapper.findNewWords()` 作为 `excludeIds`，应用层差集，避免全表 LEFT JOIN

---

## 步骤 6 — 测试模块

### 完成内容
- `TestSession`（Redis 模型，TTL 1 小时）
- `TestService`：`generate()` + `submit()`
- `TestController`：2 个端点

### 关键决策

**会话存 Redis 而非 DB**
- 测试会话是临时数据，TTL 1 小时后自动清理，无需 Flyway 迁移
- 提交后立即删除会话，防止重复提交

**正确答案不下发客户端**
- `TestSession`（服务端内部模型）含 `correctAnswer`
- `TestQuestion`（下发给客户端）只含 `prompt` 和 `choices`

**choice 模式干扰项**
- 调用 `WordBankMapper.pickDistractors(levelCode, topicCode, excludeId, 3)`
- 同等级同主题随机取 3 个，保证干扰项语义相关
- 干扰项不足 3 个时仍正常返回，不报错

**spelling/listening 评分**
- `equalsIgnoreCase()`，不区分大小写
- 前后空格 `trim()` 后比较

---

## 步骤 7 — 统计模块

### 完成内容
- `StatsService`：4 个统计接口
- `StatsController`：4 个端点
- `UserWordProgressMapper.levelSummary()` 新增

### 实现细节

**今日统计**
- 利用 `StudyLogMapper.dailySummary()` XML（`CONVERT_TZ` 按上海时区聚合）
- `goal_progress` 字段格式 `"18/20"` 直接字符串拼接，前端无需二次计算

**周报/月报**
- `week`：锚定日期所在周的周一（`TemporalAdjusters.MONDAY`）
- `month`：首日/末日用 `TemporalAdjusters.firstDayOfMonth/lastDayOfMonth`
- `level_breakdown` 走新增的 `levelSummary()` 一次聚合，不多次查询

**遗忘曲线**
- 返回该单词全部 `study_log`（时间戳 + result + stage_after）
- 同时返回理论曲线参数 `INTERVALS_HOURS`，前端用 ECharts 叠加绘制

---

## 步骤 8 — 打卡 / 错题本 / 用户模块

### 完成内容
- `CheckinService` + `CheckinController`
- `WrongWordService` + `WrongWordController`
- `UserService` + `UserController`（`/user/me`、`/user/settings`）

### 关键决策

**打卡幂等**
- 先查今日是否已有记录，有则直接返回当前状态
- 成就发放也做幂等（查 `user_achievement` 已有记录则跳过）

**streak 计算**
- 昨天有记录 → `currentStreak + 1`；否则重置为 1
- `longestStreak = max(longestStreak, currentStreak)`

**成就触发解析**
- `trigger_rule` 存 JSON 字符串（如 `{"streak":7}`）
- 用简单字符串解析替代 Jackson 反序列化，避免引入额外依赖

**手机号脱敏**
- AES 解密后取前 3 位 + `****` + 后 4 位：`138****0000`
- 解密失败时静默返回 null（不暴露加密错误）

---

## 步骤 9 — 管理后台

### 完成内容
- `AdminService` + `AdminController`

### 关键决策

**DAU 计算**
- 初始在 `AdminService.dashboard()` 里复用了 `dailySummary(userId=null)` —— 错误（XML 查询含 `user_id = #{userId}` 条件，传 null 会查不到数据）
- 修正：新增 `StudyLogMapper.countDau()` 查 `COUNT(DISTINCT user_id)`

**SecurityConfig 路径保护**
- `/admin/**` 已在 `SecurityConfig` 中配置 `hasRole("ADMIN")`
- `AdminController` 不需要额外 `@PreAuthorize`

---

## 补全项（发现并修复的遗漏）

### 问题 1：错题本永远不会自动 resolved

**根因**：`AppConstants.WRONG_WORD_RESOLVE_THRESHOLD = 3` 定义了但从未使用。

**修复**：`StudyService.answer()` 答对后调 `tryAutoResolveWrongWord()`：
```java
// 查最近 THRESHOLD 条 study_log，全为 correct 则置 resolved=1
List<StudyLog> recent = studyLogMapper.findLastN(userId, wordId, threshold);
if (recent.size() >= threshold && recent.stream().allMatch(l -> "correct".equals(l.getResult()))) {
    ww.setResolved(1);
}
```
新增 `StudyLogMapper.findLastN()` 注解 SQL。

### 问题 2：打卡统计字段永远是 0

**根因**：`checkin.words_learned / words_reviewed / correct_count / duration_seconds` 字段存在，但 `StudyService` 答题后从未更新。

**修复**：新增 `CheckinMapper.upsertStats()` 使用 `INSERT … ON DUPLICATE KEY UPDATE`，`StudyService.answer()` 每次答题后调用，依赖 `uk_user_date (user_id, checkin_date)` 唯一键。

### 问题 3：Sync 模块缺失 Service / Controller

**根因**：`sync/entity/SyncToken.java` 和 `sync/mapper/SyncTokenMapper.java` 存在，但没有业务层。

**修复**：新增完整 Sync 模块：
- `SyncService.pull()`：查 `user_word_progress.updatedAt > since`、`checkin.createdAt > since`、`user_settings.updatedAt > since`
- `SyncService.push()`：调 `answerBatch()` 处理答题队列，`upsertStats()` 处理离线打卡
- `SyncController`：从 `X-Device-Id` 请求头读设备 ID

### 问题 4：UserController 缺少 export / delete-account

**修复**：
- `GET /user/export`：查全量 `study_log` 生成 CSV，`Content-Disposition: attachment` 直接下载
- `POST /user/delete-account`：调 `VerificationCodeService.verifyAndConsume()` 校验验证码，通过后触发逻辑删除

---

## 文件结构总览

```
src/main/java/com/vocabmaster/
├── VocabMasterApplication.java
├── admin/
│   ├── controller/AdminController.java
│   ├── dto/AdminUserDto.java
│   ├── dto/DashboardResponse.java
│   ├── entity/AdminLog.java
│   ├── mapper/AdminLogMapper.java
│   └── service/AdminService.java
├── auth/
│   ├── controller/AuthController.java
│   ├── dto/{RegisterRequest, LoginRequest, LoginByCodeRequest, ...}.java
│   └── service/{AuthService, OAuthService, VerificationCodeService}.java
├── checkin/
│   ├── controller/CheckinController.java
│   ├── dto/{CheckinResponse, CalendarResponse, AchievementsResponse}.java
│   ├── entity/{Checkin, UserStreak, Achievement, UserAchievement}.java
│   ├── mapper/{CheckinMapper, UserStreakMapper, AchievementMapper, UserAchievementMapper}.java
│   └── service/CheckinService.java
├── common/
│   ├── annotation/{RateLimit, RequireAdmin}.java
│   ├── aspect/RateLimitAspect.java
│   ├── constant/{AppConstants, RedisKey}.java
│   ├── exception/{BizException, GlobalExceptionHandler}.java
│   └── result/{R, ErrorCode, PageResult}.java
├── config/
│   ├── JacksonConfig.java
│   ├── MybatisPlusConfig.java
│   ├── OpenAPIConfig.java
│   ├── RedisConfig.java
│   ├── RequestIdFilter.java
│   ├── SecurityConfig.java
│   └── WebMvcConfig.java
├── security/
│   ├── JwtAuthenticationEntryPoint.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtUtil.java
│   └── UserContext.java
├── stats/
│   ├── controller/StatsController.java
│   ├── dto/{TodayStatsResponse, SummaryResponse, DailyBreakdown, LevelBreakdown,
│   │        ForgettingCurveResponse, ReviewPoint, LevelOverviewResponse, StageDistribution}.java
│   └── service/StatsService.java
├── study/
│   ├── controller/StudyController.java
│   ├── dto/{AnswerRequest, AnswerResponse, AnswerBatchRequest, TodayPlanResponse}.java
│   ├── entity/{UserWordProgress, StudyLog, WrongWord}.java
│   ├── mapper/{UserWordProgressMapper, StudyLogMapper, WrongWordMapper}.java
│   └── service/{EbbinghausScheduler, TodayPlanService, StudyService}.java
├── sync/
│   ├── controller/SyncController.java
│   ├── dto/{SyncPullResponse, SyncPushRequest, SyncPushResponse, SyncCheckinItem}.java
│   ├── entity/SyncToken.java
│   ├── mapper/SyncTokenMapper.java
│   └── service/SyncService.java
├── test/
│   ├── controller/TestController.java
│   ├── dto/{GenerateTestRequest, GenerateTestResponse, TestQuestion, QuestionPrompt,
│   │        TestAnswerItem, SubmitTestRequest, SubmitTestResponse, QuestionResult}.java
│   ├── model/TestSession.java
│   └── service/TestService.java
├── user/
│   ├── controller/UserController.java
│   ├── dto/{UserProfileDto, UpdateProfileRequest, UpdateSettingsRequest}.java
│   ├── entity/{User, UserAuth, UserSettings}.java
│   ├── mapper/{UserMapper, UserAuthMapper, UserSettingsMapper}.java
│   └── service/UserService.java
├── util/PhoneCryptoUtil.java
├── word/
│   ├── controller/WordController.java
│   ├── dto/{WordDetailDto, WordSummaryDto, WordDownloadResponse}.java
│   ├── entity/{WordBank, Level, WordTopic}.java
│   ├── mapper/{WordBankMapper, LevelMapper, WordTopicMapper}.java
│   └── service/{WordService, LevelService, WordImportService}.java
└── wrongword/
    ├── controller/WrongWordController.java
    ├── dto/WrongWordDto.java
    └── service/WrongWordService.java

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-prod.yml
├── db/migration/V1__init.sql
└── mapper/
    ├── StudyLogMapper.xml
    ├── WordBankMapper.xml
    └── WrongWordMapper.xml

src/test/java/com/vocabmaster/
└── study/service/EbbinghausSchedulerTest.java   (17 个测试用例)
```

---

## Redis Key 汇总

| Key 模板 | TTL | 用途 |
|---------|-----|-----|
| `token:blacklist:{token}` | token 剩余有效期 | 登出 token 黑名单 |
| `token:refresh:{uuid}` | 7 天 | refreshToken 存储 |
| `code:{scene}:{type}:{identifier}` | 5 分钟 | 验证码 |
| `code:day_count:{type}:{identifier}` | 次日 0 点 | 验证码日发送计数 |
| `study:plan:{userId}:{levelCode}:{date}` | 10 分钟 | 今日学习计划缓存 |
| `test:session:{testId}` | 1 小时 | 测试会话（含正确答案） |
| `user:info:{userId}` | 30 分钟 | 用户信息缓存 |
| `user:settings:{userId}` | 30 分钟 | 用户设置缓存 |
| `word:levels` | 永久（手动失效） | 等级列表缓存 |
| `word:topics` | 永久（手动失效） | 主题列表缓存 |
| `checkin:streak:{userId}` | 永久 | 连续打卡天数 |
| `rate:{apiKey}:{identifier}` | window 秒 | 接口限流计数 |

---

## 已知 TODO（生产前需处理）

1. **Apple OAuth 生产验签**：`OAuthService` 中 Apple `identity_token` 仅做 JWT decode，生产环境需用 Apple JWK 公钥验签
2. **用户导出异步化**：当前 `GET /user/export` 同步生成 CSV，数据量大时会阻塞线程，生产应改为异步生成 + 预签名 URL 下载
3. **`dailySummary` 时区硬编码**：`CONVERT_TZ` 中时区写死为 `+08:00`，应读用户 `timezone` 字段动态计算
4. **测试覆盖率**：目前只有 `EbbinghausSchedulerTest`（17 用例），`StudyService`、`AuthService` 等核心逻辑缺少集成测试
5. **`/sync/pull` 数据量限制**：未对返回的 `progress` 变更列表做分页，大量离线操作后可能返回超大响应
