# VocabMaster — Java 后端

Spring Boot 3.5.x + MyBatis-Plus + MySQL 8 + Redis 7

---

## 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 21 LTS |
| Maven | 3.9+ |
| MySQL | 8.0+ |
| Redis | 7.x |
| Docker (可选) | 用于快速起依赖服务 |

---

## 快速启动

```bash
# 1. 启动 MySQL + Redis（项目根目录）
docker compose up -d mysql redis

# 2. 复制并填写环境变量
cp .env .env.local   # 按需修改密码

# 3. 启动后端（dev profile 会自动加载 .env）
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 4. 访问 Swagger UI
open http://localhost:8080/api/v1/swagger-ui.html
```

> 首次启动时，若 `word_bank` 表为空，服务会自动导入 `../seed/words_sample.csv` 种子数据。

---

## 已完成模块

### 步骤 0 — 工程骨架 ✅
- `pom.xml`：Spring Boot 3.5.4 / MyBatis-Plus 3.5.7 / jjwt 0.12.5 / Bouncy Castle 1.78.1
- `application.yml` / `application-dev.yml` / `application-prod.yml`
- `.env` 开发环境变量文件（已加入 .gitignore）
- Flyway 迁移脚本 `V1__init.sql`（15 张表 + 字典数据）

### 步骤 1 — 公共基建 ✅
- `common/result`：`R<T>`（统一响应，含 requestId）、`ErrorCode`（全量错误码）、`PageResult<T>`
- `common/exception`：`BizException`、`GlobalExceptionHandler`（7 种异常统一处理）
- `common/constant`：`RedisKey`（Key 前缀常量）、`AppConstants`（业务常量）
- `common/annotation`：`@RateLimit`、`@RequireAdmin`
- `common/aspect`：`RateLimitAspect`（Redis 计数器，按用户/IP 维度）
- `security`：`UserContext`（ThreadLocal）、`JwtUtil`（HS256 双 Token）、`JwtAuthenticationFilter`（黑名单检测）、`JwtAuthenticationEntryPoint`
- `config`：`SecurityConfig`（无状态链）、`RedisConfig`（双模板）、`JacksonConfig`（SNAKE_CASE + UTC）、`MybatisPlusConfig`（分页插件 + 自动填充）、`OpenAPIConfig`（Swagger Bearer Auth）、`WebMvcConfig`（CORS + RestTemplate）、`RequestIdFilter`
- `util`：`PhoneCryptoUtil`（AES-256-GCM + SHA-256）

### 步骤 2 — 数据层 ✅
- 15 个 Entity（全部对应 `V1__init.sql` 表结构）
- 15 个 Mapper（继承 `BaseMapper<T>`，关键方法含注解 SQL）
- 3 个 XML：`WordBankMapper.xml`（新词/干扰项/下载）、`StudyLogMapper.xml`（日统计/区间查询）、`WrongWordMapper.xml`

### 步骤 3 — 认证模块 ✅
- `AuthController`：9 个端点（注册/验证码/登录/微信/Apple/刷新/登出/重置密码）
- `AuthService`：密码强度校验、bcrypt 哈希、手机号 AES 加密存储、JWT 双 Token 签发/吊销
- `VerificationCodeService`：Redis 验证码（TTL 5 min）、分钟级 + 日级双重限流
- `OAuthService`：微信 jscode2session、Apple identity_token 解码（TODO: 生产验签）

### 步骤 4 — 词库模块 ✅
- `LevelService`：等级 / 主题列表（Redis 永久缓存，主动失效）
- `WordService`：按 ID / level+word 查询、前缀搜索、批量下载（支持 since 增量）
- `WordImportService`：启动自动检测空表并导入种子 CSV；管理员接口支持手动上传 CSV
- `WordController`：6 个端点（levels / topics 公开，其余需 JWT）

---

### 步骤 5 — 学习核心模块 ✅
- `EbbinghausScheduler`：九阶段固定间隔状态机（纯函数），`schedule()` / `nextStage()` / `computeNextReviewAt()`
- `TodayPlanService`：今日计划（到期复习 + 新词补充，Redis 缓存 10 分钟，答题后主动失效）
- `StudyService`：答题事务（progress upsert + study_log + 错题本 upsert + 缓存失效 + 冲突检测）；支持批量离线同步（按 client_ts 排序，冲突跳过）；reset / markMastered
- `StudyController`：`/study/today`、`/answer`（限流 60/min）、`/answer-batch`、`/reset`、`/mark-mastered`
- `EbbinghausSchedulerTest`：17 个单元测试（状态转移、时间计算、边界、文档示例对齐）

---

## 下一步

### 步骤 6 — 测试模块 ✅
- 测试会话存 Redis（TTL 1 小时），无需额外 DB 表
- `TestSession`（Redis 模型）：含正确答案，不下发客户端
- `TestService`：`generate()` 支持 source=due/all/wrong_words；spelling 显示中文释义+音频，choice 附带 3 个干扰项，listening 只给音频；`submit()` 评分后触发 `StudyService.answer()` 更新进度，提交后删除会话防重复
- `TestController`：`/test/generate`、`/test/submit`
- `RedisKey` 新增 `TEST_SESSION` 键

---

## 下一步

### 步骤 7 — 统计模块
- `StatsService`：今日统计、周/月报（按日聚合）、遗忘曲线数据、等级概览
- `StatsController`：`/stats/today`、`/stats/summary`、`/stats/forgetting-curve`、`/stats/level-overview`

### 后续步骤
- 步骤 8：打卡 / 错题本 / 多端同步
- 步骤 9：管理后台轻量版
