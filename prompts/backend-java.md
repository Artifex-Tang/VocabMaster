# Prompt — Java 后端开发

> 把这份 prompt 作为 Claude Code 在 `backend-java/` 目录下的起始任务描述。

## 你的角色

你是一位资深 Java 后端工程师，负责从零搭建 VocabMaster 项目的 Spring Boot 3.5.x 后端服务。

## 必读文档（先读再写代码）

按顺序阅读下列文档，读完再动键盘：

1. `../CLAUDE.md` — 项目总览与硬约束
2. `../docs/00-README.md` — 产品形态
3. `../docs/01-architecture.md` — 模块划分和系统架构
4. `../docs/02-database-design.md` — 15 张表的 DDL（**与 `../sql/init.sql` 保持一致**）
5. `../docs/03-api-specification.md` — RESTful API 全量接口
6. `../docs/04-ebbinghaus-algorithm.md` — 复习算法状态机
7. `../docs/05-backend-java.md` — Java 实现细节（包结构、关键类代码样例、pom.xml 骨架）

读完后，如果对 `docs/05-backend-java.md` 里的任何代码示例有疑问，**停下来问我**，不要自由发挥。

## 硬性技术约束

| 项 | 要求 |
|----|------|
| JDK | 21 LTS（**必须**，不用 17 也不用 23） |
| 构建 | Maven 3.9+ |
| Spring Boot | 3.5.x（**不升级到 4.x**，当前生产级稳定分支，4.x 存在破坏性变更） |
| ORM | MyBatis-Plus 3.5.7+ |
| 数据库 | MySQL 8.x，字符集 `utf8mb4_0900_ai_ci` |
| 缓存 | Redis 7.x |
| JWT | jjwt 0.12.5，算法 HS256，双 token：access 2h + refresh 7d |
| 密码哈希 | BCrypt cost=12 |
| 手机号存储 | AES-256-GCM 加密 + SHA-256 哈希（用 phone_hash 建唯一索引） |
| OpenAPI | springdoc-openapi 2.5.0 |
| 代码风格 | Google Java Style，Lombok 允许 `@Data @Slf4j @RequiredArgsConstructor` |
| 时间字段 | 数据库 UTC，响应 ISO 8601 带时区 |

## 开发顺序（严格按此）

### 第 0 步：工程骨架
1. 在 `backend-java/` 下生成 Spring Boot 工程（`pom.xml` 完全复制 `docs/05-backend-java.md` 的骨架）
2. 建立 `com.vocabmaster` 根包，按 `docs/05-backend-java.md` 第 3 节"目录结构"分包
3. 建 `application.yml`、`application-dev.yml`、`application-prod.yml`
4. 集成 Druid 连接池、MyBatis-Plus、Redis Template、Jackson（LocalDateTime 支持）
5. 跑起 `VocabmasterApplication.main()`，确保能启动（此时没有业务代码也要能启）

### 第 1 步：公共基建
按这个顺序实现：

1. `common/R.java`（统一响应）
2. `common/ErrorCode.java`（全量错误码枚举，与 Python 版对齐）
3. `common/GlobalExceptionHandler.java`
4. `common/constant/`（JWT key、Redis key 前缀、业务常量）
5. `config/MybatisPlusConfig`、`config/RedisConfig`、`config/JacksonConfig`、`config/OpenAPIConfig`
6. `security/JwtUtil`、`security/JwtAuthenticationFilter`、`security/SecurityConfig`
7. `util/PhoneCryptoUtil`（AES 加密 + SHA256 哈希）

### 第 2 步：数据层
1. 按 `sql/init.sql` 创建所有 Entity 类（`@TableName` 注解）
2. 每个 Entity 建 Mapper 接口（继承 `BaseMapper<T>`）
3. 复杂查询写在 XML（`resources/mapper/`）：
   - `UserWordProgressMapper.findDueForReview`
   - `StudyLogMapper.findByDateRange`
   - `WordBankMapper.pickRandomForTest`
4. 运行 MyBatis-Plus 生成器（可选）或手写，保持命名统一

### 第 3 步：认证模块（最先跑通）
1. 注册：邮箱/手机号注册，校验格式，bcrypt 哈希密码
2. 登录：邮箱/手机号 + 密码，发 access + refresh token
3. 刷新 token：refresh token 换 access token
4. 微信小程序登录：接收 `code`，调微信 API 换 openid，不存在则自动注册
5. Apple 登录：接收 `identityToken`，验签，取 sub 作为 apple_id
6. 登出：把当前 access token 加入 Redis 黑名单（剩余 TTL）

**联调测试**：用 Postman 或单元测试跑通「注册→登录→带 token 访问受保护接口→登出」全链路。

### 第 4 步：词库模块
1. `WordService`：按等级分页查词、关键词搜索、批量导入（管理员）
2. `WordImportService`：读 `seed/words_sample.csv` 写入 `word_bank` 表（**工程启动时自动检测，若表空则导入**）
3. `LevelService`：返回十个等级列表（`/api/v1/words/levels`）

### 第 5 步：学习模块（核心）
严格按 `docs/04-ebbinghaus-algorithm.md`：

1. `EbbinghausScheduler`：静态工具类，实现 `schedule(currentStage, answerResult)` 纯函数
2. `StudyService.getTodayPlan(userId, levelCode)`：
   - 查 `user_word_progress` 中 `next_review_at <= now()` 的记录
   - 再从 `word_bank` 里补足"新词"到 `daily_new_target`
   - Redis 缓存 5 分钟，key: `study:plan:{userId}:{levelCode}:{date}`
3. `StudyService.answer(...)`：
   - 事务内：查 progress → 调 Scheduler → 更新 progress → 写 study_log
   - 处理首次学习（progress 不存在→INSERT）
   - 处理冲突：`client_ts < db.updated_at` 时拒绝并返回服务端最新状态
4. `StudyController`：实现 `/api/v1/study/today`、`/answer`、`/answer-batch`

**单元测试必写**：`EbbinghausSchedulerTest` 覆盖所有 9 个阶段 + 答错回退 + 上限。

### 第 6 步：测试模块
1. `TestService.generate(...)`：按 mode (spelling/choice/listening) 和 count 生成题目
   - 选择题的干扰项从同等级同主题词中取 3 个
2. `TestService.submit(...)`：批量判定，写 wrong_word 表

### 第 7 步：统计模块
1. `StatsService.today(userId)`：当日新学 / 复习数 / 正确率
2. `StatsService.forgettingCurve(userId, levelCode)`：
   - 理论曲线：直接按艾宾浩斯公式返回 9 个点
   - 实际点：从 `user_word_progress` 按 stage 分组返回掌握数
3. Redis 缓存 10 分钟

### 第 8 步：其他模块
打卡、连续天数（用 Redis BITMAP 优化）、错词本、多端同步。

### 第 9 步：管理后台接口
只实现**轻量版**：词库 CRUD + 用户列表查询 + 封号。不做角色权限系统（管理员由 `user.is_admin` 字段控制）。

## 禁止事项

- ❌ **不要**自己发明 API 接口，所有接口以 `docs/03-api-specification.md` 为准
- ❌ **不要**改艾宾浩斯间隔数组，以 `docs/04-ebbinghaus-algorithm.md` 第 2 节为准
- ❌ **不要**用 `@DateTimeFormat` 处理时间，统一用 Jackson 的 `JavaTimeModule`
- ❌ **不要**在 Controller 里写业务逻辑，薄 Controller + 厚 Service
- ❌ **不要**直接 `e.printStackTrace()`，用 `log.error("...", e)`
- ❌ **不要**在事务方法里调用远程 API（短信、微信 API），把它们拆出去

## 交付标准

每完成一个模块后：
1. 跑通该模块的所有 Controller 接口（手动或 MockMvc）
2. 核心 Service 类写单元测试（覆盖率目标 60%+）
3. 更新 `backend-java/README.md` 记录启动方式和已完成模块
4. `git commit` 时用约定式提交：`feat(auth): 实现邮箱注册登录`

## 开发环境启动

```bash
# 1. 起 MySQL + Redis（用项目根 docker-compose.yml）
docker compose up -d mysql redis

# 2. 导入 schema
mysql -uroot -proot vocabmaster < sql/init.sql

# 3. 启动后端
cd backend-java
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 4. 访问 Swagger
open http://localhost:8080/swagger-ui/index.html
```

## 你开始工作时的第一条回复

先输出：
1. "我已阅读 CLAUDE.md 和 docs/00~05 与 docs/07"
2. 你理解的开发顺序
3. 你计划的分支策略（建议 `main` + `dev` + `feat/*`）
4. 对 `docs/05-backend-java.md` 有无疑问

**在我确认后再动代码。**
