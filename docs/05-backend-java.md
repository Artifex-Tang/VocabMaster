# 05 — Java 后端实现规范

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 21 | LTS |
| Spring Boot | 3.5.x | 稳定版 |
| MyBatis-Plus | 3.5.7+ | ORM |
| MySQL 驱动 | 9.0+ | mysql-connector-j |
| Redis 客户端 | Lettuce（Spring Data Redis 默认） | |
| JWT | jjwt 0.12+ | |
| 参数校验 | jakarta.validation | |
| API 文档 | springdoc-openapi-starter-webmvc-ui 2.x | |
| 工具 | Hutool 5.8+、Lombok、MapStruct 1.5+ | |
| 构建 | Maven 3.9+ | |

## 项目结构

```
backend-java/
├── pom.xml
├── src/main/java/com/vocabmaster/
│   ├── VocabMasterApplication.java
│   ├── common/                           # 通用模块
│   │   ├── result/
│   │   │   ├── R.java                   # 统一响应封装
│   │   │   ├── ErrorCode.java           # 错误码枚举
│   │   │   └── PageResult.java
│   │   ├── exception/
│   │   │   ├── BizException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── annotation/
│   │   │   ├── RateLimit.java
│   │   │   └── RequireAdmin.java
│   │   └── util/
│   │       ├── JwtUtil.java
│   │       ├── AesUtil.java
│   │       └── IdGenerator.java
│   ├── config/                           # 配置类
│   │   ├── WebMvcConfig.java
│   │   ├── SecurityConfig.java
│   │   ├── RedisConfig.java
│   │   ├── MybatisPlusConfig.java
│   │   ├── SwaggerConfig.java
│   │   └── CorsConfig.java
│   ├── security/
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtAuthenticationEntryPoint.java
│   │   └── UserContext.java             # ThreadLocal 存储当前用户
│   ├── auth/
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java
│   │   ├── service/OAuthService.java
│   │   ├── service/VerificationCodeService.java
│   │   └── dto/
│   ├── user/
│   │   ├── controller/UserController.java
│   │   ├── service/UserService.java
│   │   ├── service/UserSettingsService.java
│   │   ├── mapper/UserMapper.java
│   │   ├── entity/
│   │   │   ├── User.java
│   │   │   ├── UserAuth.java
│   │   │   └── UserSettings.java
│   │   └── dto/
│   ├── word/
│   │   ├── controller/WordController.java
│   │   ├── service/WordService.java
│   │   ├── service/WordDownloadService.java
│   │   ├── mapper/WordBankMapper.java
│   │   ├── mapper/LevelMapper.java
│   │   ├── mapper/WordTopicMapper.java
│   │   └── entity/
│   ├── study/
│   │   ├── controller/StudyController.java
│   │   ├── service/StudyService.java
│   │   ├── service/EbbinghausScheduler.java   # 算法核心
│   │   ├── service/TodayPlanService.java
│   │   ├── mapper/UserWordProgressMapper.java
│   │   ├── mapper/StudyLogMapper.java
│   │   └── entity/
│   ├── test/
│   │   ├── controller/TestController.java
│   │   ├── service/TestService.java
│   │   └── dto/
│   ├── stats/
│   │   ├── controller/StatsController.java
│   │   ├── service/StatsService.java
│   │   └── dto/
│   ├── checkin/
│   │   ├── controller/CheckinController.java
│   │   ├── service/CheckinService.java
│   │   ├── service/AchievementService.java
│   │   ├── mapper/CheckinMapper.java
│   │   ├── mapper/UserStreakMapper.java
│   │   └── entity/
│   ├── sync/
│   │   ├── controller/SyncController.java
│   │   └── service/SyncService.java
│   ├── wrongword/
│   │   └── ...
│   └── admin/
│       └── ...
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   ├── mapper/                            # MyBatis XML
│   │   └── ...
│   └── db/migration/                      # Flyway
│       ├── V1__init.sql
│       └── V2__add_indexes.sql
└── src/test/
    └── ...
```

## 关键实现指引

### 1. 统一响应封装

```java
// common/result/R.java
public class R<T> {
    private int code;
    private String msg;
    private T data;
    private String requestId;
    
    public static <T> R<T> ok(T data) { ... }
    public static <T> R<T> fail(ErrorCode ec) { ... }
    public static <T> R<T> fail(ErrorCode ec, String customMsg) { ... }
}

// common/result/ErrorCode.java
public enum ErrorCode {
    SUCCESS(0, "ok"),
    PARAM_INVALID(10001, "参数校验失败"),
    RATE_LIMIT(10002, "请求过于频繁"),
    UNAUTHORIZED(20001, "未登录或 token 无效"),
    TOKEN_EXPIRED(20002, "token 已过期"),
    FORBIDDEN(20003, "无权限"),
    ACCOUNT_EXISTS(30001, "账号已存在"),
    VERIFY_CODE_INVALID(30002, "验证码错误或已过期"),
    ACCOUNT_NOT_FOUND(30003, "账号不存在"),
    PASSWORD_WRONG(30004, "密码错误"),
    WORD_NOT_FOUND(40001, "词条不存在"),
    LEVEL_NOT_FOUND(40002, "等级不存在"),
    ANSWER_INVALID(40004, "无效的答题结果"),
    SYNC_CONFLICT(50001, "同步冲突"),
    SERVER_ERROR(99999, "服务端错误");
    
    private final int code;
    private final String msg;
}
```

### 2. 全局异常处理

```java
// common/exception/GlobalExceptionHandler.java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e) {
        return R.fail(e.getErrorCode(), e.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return R.fail(ErrorCode.PARAM_INVALID, msg);
    }
    
    @ExceptionHandler(Exception.class)
    public R<Void> handleGeneric(Exception e) {
        log.error("未捕获异常", e);
        return R.fail(ErrorCode.SERVER_ERROR);
    }
}
```

### 3. JWT 工具 + 认证过滤器

```java
// common/util/JwtUtil.java
@Component
public class JwtUtil {
    @Value("${app.jwt.secret}") 
    private String secret;
    
    @Value("${app.jwt.access-ttl-seconds:7200}") 
    private long accessTtl;
    
    @Value("${app.jwt.refresh-ttl-seconds:604800}") 
    private long refreshTtl;
    
    public String generateAccessToken(String userUuid, Long userId) { ... }
    public String generateRefreshToken(String userUuid) { ... }
    public Claims parse(String token) { ... }  // 抛 BizException 如果非法
}

// security/JwtAuthenticationFilter.java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redis;
    
    @Override
    protected void doFilterInternal(HttpServletRequest req, 
                                    HttpServletResponse res, 
                                    FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }
        String token = header.substring(7);
        
        // 检查黑名单（登出的 token）
        if (Boolean.TRUE.equals(redis.hasKey("token:blacklist:" + token))) {
            sendError(res, ErrorCode.UNAUTHORIZED);
            return;
        }
        
        try {
            Claims claims = jwtUtil.parse(token);
            String uuid = claims.getSubject();
            Long userId = claims.get("uid", Long.class);
            UserContext.set(new UserContext.Current(userId, uuid));
            
            Authentication auth = new UsernamePasswordAuthenticationToken(
                uuid, null, List.of()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(req, res);
        } finally {
            UserContext.clear();
        }
    }
}
```

### 4. Spring Security 配置

```java
// config/SecurityConfig.java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;
    private final JwtAuthenticationEntryPoint entryPoint;
    
    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", 
                                 "/api/v1/words/levels",
                                 "/api/v1/words/topics",
                                 "/v3/api-docs/**", "/swagger-ui/**")
                    .permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(eh -> eh.authenticationEntryPoint(entryPoint))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
    
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

### 5. 艾宾浩斯调度器（核心算法）

```java
// study/service/EbbinghausScheduler.java
@Component
public class EbbinghausScheduler {
    
    // 从 stage N 前进到 stage N+1 后的间隔（小时）
    // 索引 0 对应 stage 1->2 的间隔
    private static final double[] INTERVALS_HOURS = {
        5.0/60, 0.5, 12.0, 24.0, 48.0, 96.0, 168.0, 360.0, 720.0
    };
    
    public static final int MAX_STAGE = 9;
    
    /**
     * 根据答题结果计算下一状态
     */
    public SchedulerResult schedule(int stageBefore, AnswerResult result, LocalDateTime now) {
        int stageAfter;
        switch (result) {
            case CORRECT:
                stageAfter = Math.min(stageBefore + 1, MAX_STAGE);
                break;
            case WRONG:
                stageAfter = Math.max(1, stageBefore - 1);
                break;
            case SKIP:
                stageAfter = Math.max(1, stageBefore);
                break;
            default:
                throw new BizException(ErrorCode.ANSWER_INVALID);
        }
        
        LocalDateTime nextReview = calculateNextReview(stageAfter, now);
        boolean justMastered = (stageBefore < MAX_STAGE && stageAfter == MAX_STAGE);
        
        return new SchedulerResult(stageBefore, stageAfter, nextReview, justMastered);
    }
    
    private LocalDateTime calculateNextReview(int stage, LocalDateTime from) {
        if (stage >= MAX_STAGE) {
            return from.plusDays(30);  // mastered 后 30 天再出现
        }
        double hours = INTERVALS_HOURS[stage - 1];
        long seconds = (long) (hours * 3600);
        return from.plusSeconds(seconds);
    }
    
    public record SchedulerResult(
        int stageBefore,
        int stageAfter,
        LocalDateTime nextReviewAt,
        boolean justMastered
    ) {}
}
```

### 6. 学习服务（最核心的 Service）

```java
// study/service/StudyService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyService {
    
    private final UserWordProgressMapper progressMapper;
    private final StudyLogMapper studyLogMapper;
    private final WrongWordService wrongWordService;
    private final EbbinghausScheduler scheduler;
    private final CheckinService checkinService;  // 答题时触发今日打卡
    
    @Transactional
    public AnswerResponse answer(Long userId, AnswerRequest req) {
        // 1. 获取或创建 progress
        UserWordProgress progress = progressMapper.selectOne(
            Wrappers.<UserWordProgress>lambdaQuery()
                .eq(UserWordProgress::getUserId, userId)
                .eq(UserWordProgress::getWordId, req.getWordId())
        );
        
        boolean isNew = (progress == null);
        if (isNew) {
            progress = new UserWordProgress();
            progress.setUserId(userId);
            progress.setWordId(req.getWordId());
            progress.setLevelCode(req.getLevelCode());
            progress.setStage(0);
        }
        
        // 2. 冲突检测
        if (progress.getClientUpdatedAt() != null 
            && req.getClientTs() != null
            && req.getClientTs().isBefore(progress.getClientUpdatedAt())) {
            throw new BizException(ErrorCode.SYNC_CONFLICT, 
                "客户端时间戳早于服务端最新状态");
        }
        
        // 3. 调度
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int stageBefore = progress.getStage();
        SchedulerResult sr = scheduler.schedule(stageBefore, req.getResult(), now);
        
        // 4. 更新 progress
        progress.setStage(sr.stageAfter());
        progress.setLastReviewedAt(now);
        progress.setNextReviewAt(sr.nextReviewAt());
        progress.setClientUpdatedAt(req.getClientTs());
        if (req.getResult() == AnswerResult.CORRECT) {
            progress.setCorrectCount(progress.getCorrectCount() + 1);
        } else if (req.getResult() == AnswerResult.WRONG) {
            progress.setWrongCount(progress.getWrongCount() + 1);
        }
        if (stageBefore == 0 && sr.stageAfter() > 0) {
            progress.setFirstLearnedAt(now);
        }
        if (sr.justMastered()) {
            progress.setMasteredAt(now);
        }
        
        if (isNew) progressMapper.insert(progress);
        else progressMapper.updateById(progress);
        
        // 5. 写 study_log
        StudyLog log = StudyLog.builder()
            .userId(userId).wordId(req.getWordId())
            .levelCode(req.getLevelCode())
            .action(stageBefore == 0 ? "learn" : "review")
            .result(req.getResult().name().toLowerCase())
            .mode(req.getMode())
            .stageBefore(stageBefore)
            .stageAfter(sr.stageAfter())
            .durationMs(req.getDurationMs())
            .clientTs(req.getClientTs())
            .build();
        studyLogMapper.insert(log);
        
        // 6. 答错 → 入错题本
        if (req.getResult() == AnswerResult.WRONG) {
            wrongWordService.upsert(userId, req.getWordId(), req.getLevelCode());
        }
        
        // 7. 触发今日打卡（异步）
        checkinService.touchTodayAsync(userId, req.getResult() == AnswerResult.CORRECT);
        
        return AnswerResponse.from(progress, sr);
    }
    
    @Transactional
    public List<AnswerResponse> answerBatch(Long userId, List<AnswerRequest> reqs) {
        // 按 client_ts 排序
        reqs.sort(Comparator.comparing(AnswerRequest::getClientTs, 
                Comparator.nullsLast(Comparator.naturalOrder())));
        
        List<AnswerResponse> responses = new ArrayList<>();
        for (AnswerRequest req : reqs) {
            try {
                responses.add(answer(userId, req));
            } catch (BizException e) {
                if (e.getErrorCode() == ErrorCode.SYNC_CONFLICT) {
                    // 冲突的记录跳过，不中断批次
                    responses.add(AnswerResponse.conflict(req.getWordId()));
                } else {
                    throw e;
                }
            }
        }
        return responses;
    }
}
```

### 7. MyBatis-Plus Mapper 示例

```java
// study/mapper/UserWordProgressMapper.java
@Mapper
public interface UserWordProgressMapper extends BaseMapper<UserWordProgress> {
    
    @Select("""
        SELECT * FROM user_word_progress
        WHERE user_id = #{userId}
          AND level_code = #{levelCode}
          AND stage > 0 AND stage < 9
          AND next_review_at <= #{now}
        ORDER BY next_review_at ASC
        LIMIT #{limit}
    """)
    List<UserWordProgress> findDueForReview(@Param("userId") Long userId,
                                            @Param("levelCode") String levelCode,
                                            @Param("now") LocalDateTime now,
                                            @Param("limit") int limit);
    
    @Select("""
        SELECT word_id FROM user_word_progress
        WHERE user_id = #{userId} AND level_code = #{levelCode} AND stage > 0
    """)
    List<Long> findLearnedWordIds(@Param("userId") Long userId, 
                                  @Param("levelCode") String levelCode);
}
```

### 8. 配置文件

```yaml
# application.yml
spring:
  application:
    name: vocabmaster
  profiles:
    active: dev
  datasource:
    url: jdbc:mysql://localhost:3306/vocabmaster?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=UTC&useSSL=false
    username: vocab
    password: ${DB_PASSWORD:vocab123}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 3000
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
  jackson:
    property-naming-strategy: SNAKE_CASE
    time-zone: UTC
    date-format: yyyy-MM-dd'T'HH:mm:ss.SSSXXX

mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      logic-delete-field: deletedAt
      logic-delete-value: NOW()
      logic-not-delete-value: 'NULL'

app:
  jwt:
    secret: ${JWT_SECRET:please-change-in-prod-at-least-32-bytes!}
    access-ttl-seconds: 7200
    refresh-ttl-seconds: 604800
  aes:
    key: ${AES_KEY:please-change-in-prod-exactly-32byt}
  oauth:
    wechat:
      app-id: ${WECHAT_APP_ID}
      app-secret: ${WECHAT_APP_SECRET}
    apple:
      team-id: ${APPLE_TEAM_ID}
      client-id: ${APPLE_CLIENT_ID}
      key-id: ${APPLE_KEY_ID}

server:
  port: 8080
  servlet:
    context-path: /api/v1

logging:
  level:
    com.vocabmaster: DEBUG
    org.springframework.security: INFO
  pattern:
    console: "%d{HH:mm:ss} %-5level [%X{requestId}] %logger{36} - %msg%n"

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

### 9. Controller 示例

```java
// study/controller/StudyController.java
@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
@Tag(name = "学习", description = "学习相关接口")
public class StudyController {
    
    private final StudyService studyService;
    private final TodayPlanService todayPlanService;
    
    @GetMapping("/today")
    @Operation(summary = "获取今日学习计划")
    public R<TodayPlanResponse> today(@RequestParam String level) {
        Long userId = UserContext.currentUserId();
        return R.ok(todayPlanService.getTodayPlan(userId, level));
    }
    
    @PostMapping("/answer")
    @Operation(summary = "上报答题结果")
    @RateLimit(limit = 60, window = 60)
    public R<AnswerResponse> answer(@Valid @RequestBody AnswerRequest req) {
        Long userId = UserContext.currentUserId();
        return R.ok(studyService.answer(userId, req));
    }
    
    @PostMapping("/answer-batch")
    @Operation(summary = "批量上报答题结果（离线同步用）")
    public R<List<AnswerResponse>> answerBatch(@Valid @RequestBody AnswerBatchRequest req) {
        Long userId = UserContext.currentUserId();
        return R.ok(studyService.answerBatch(userId, req.getAnswers()));
    }
}
```

### 10. pom.xml 骨架

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.4</version>
        <relativePath/>
    </parent>
    
    <groupId>com.vocabmaster</groupId>
    <artifactId>vocabmaster-backend</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    
    <properties>
        <java.version>21</java.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <jjwt.version>0.12.5</jjwt.version>
        <hutool.version>5.8.26</hutool.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-mysql</artifactId>
        </dependency>
        
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-processor</artifactId>
            <version>${mapstruct.version}</version>
            <scope>provided</scope>
        </dependency>
        
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.5.0</version>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <finalName>vocabmaster-backend</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

## 单元测试要求

核心业务逻辑必须有单测（覆盖率 > 70%），特别是：

- `EbbinghausScheduler.schedule()` 的所有状态转移
- `StudyService.answer()` 的冲突处理
- `TodayPlanService.getTodayPlan()` 的到期计算
- JWT 工具的签发/校验
- AES 加密工具的加解密一致性

推荐用 JUnit 5 + Mockito + Testcontainers（集成测试真实 MySQL + Redis）。

## 常见陷阱

1. **时区**：所有 `LocalDateTime` 存取都按 UTC，前端展示时转。数据库字段也是 UTC。**不要**用 `LocalDateTime.now()`（带系统时区），用 `LocalDateTime.now(ZoneOffset.UTC)`。
2. **MyBatis-Plus 驼峰**：开启 `map-underscore-to-camel-case`，Java 字段用驼峰，数据库字段下划线。
3. **Jackson 命名**：配置了 `SNAKE_CASE`，DTO 字段写驼峰自动转 snake_case 出参入参。
4. **事务边界**：`StudyService.answer()` 整体事务包住"更新 progress + 写 log + 更新错题本"，保证一致性。
5. **敏感数据脱敏**：User 返回前端时不要把 `passwordHash` 带出去。用 `@JsonIgnore` 或 DTO 映射。
