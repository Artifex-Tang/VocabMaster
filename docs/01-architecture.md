# 01 — 系统架构

## 总体架构

```
┌─────────────────────────────────────────────────────────────┐
│                         客户端层                              │
├─────────────────┬─────────────────┬──────────────────────────┤
│   Web (Vue3)    │  小程序 (Uni)   │   Android (Uni 编译)     │
└────────┬────────┴────────┬────────┴────────┬─────────────────┘
         │                 │                 │
         │      HTTPS + JWT Bearer Token     │
         └─────────────────┼─────────────────┘
                           │
                  ┌────────▼────────┐
                  │   Nginx 网关    │  反向代理 + 静态资源
                  └────────┬────────┘
                           │
                  ┌────────▼────────┐
                  │  Spring Boot    │  业务 API
                  │   (Java 21)     │
                  └────────┬────────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
    ┌────▼────┐      ┌─────▼────┐      ┌────▼─────┐
    │  MySQL  │      │  Redis   │      │  对象存储 │
    │  (主)   │      │ (缓存/session) │ (词库音频/图) │
    └─────────┘      └──────────┘      └──────────┘
```

## 模块划分（后端）

### 核心业务模块

```
com.vocabmaster
├── auth/              # 认证授权（注册/登录/刷新/OAuth）
├── user/              # 用户信息 + 配置
├── word/              # 词库（分级 + 查询 + 批量下载）
├── study/             # 学习核心（卡片调度 + 评分记录）
├── review/            # 艾宾浩斯复习调度器
├── test/              # 测试模式（拼写/选择/听写）
├── stats/             # 统计报表
├── checkin/           # 打卡 + 连续天数
├── sync/              # 多端同步 + 离线队列
├── admin/             # 管理后台（词库维护）
└── common/            # 通用（异常 / 响应封装 / 工具）
```

### 基础设施模块

```
├── config/            # Spring 配置（Security/Redis/Swagger）
├── security/          # JWT + 权限注解
├── cache/             # Redis 缓存抽象
├── mybatis/           # MyBatis-Plus 通用配置
└── exception/         # 全局异常处理
```

## 模块划分（前端）

### Web 端（frontend-web）

```
src/
├── api/                 # 接口封装（axios）
│   ├── auth.ts
│   ├── word.ts
│   ├── study.ts
│   ├── stats.ts
│   └── ...
├── stores/              # Pinia
│   ├── user.ts
│   ├── study.ts
│   └── settings.ts
├── views/
│   ├── auth/            # 登录/注册
│   ├── dashboard/       # 首页 / 今日计划
│   ├── study/           # 学习卡片
│   ├── review/          # 复习
│   ├── test/            # 测试模式
│   ├── stats/           # 报表
│   ├── settings/        # 个人设置
│   └── admin/           # 管理后台
├── components/          # 通用组件
│   ├── WordCard.vue
│   ├── EbbinghausChart.vue
│   └── ...
├── router/
├── utils/
│   ├── request.ts       # axios 拦截器
│   ├── tts.ts           # Web Speech API 封装
│   └── storage.ts       # IndexedDB 封装（离线缓存）
└── App.vue
```

### Uni-app 端（frontend-uniapp）

```
src/
├── pages/
│   ├── auth/
│   ├── dashboard/
│   ├── study/
│   ├── review/
│   ├── test/
│   ├── stats/
│   └── profile/
├── components/          # 跨端组件
├── api/                 # 同 Web
├── stores/              # Pinia（同 Web，尽量共享）
├── utils/
│   ├── request.ts       # uni.request 封装
│   ├── tts.ts           # 多端 TTS 适配层
│   ├── storage.ts       # 本地存储适配
│   └── platform.ts      # 平台差异处理
├── pages.json
├── manifest.json
└── App.vue
```

## 数据流

### 学习一个单词的完整流程

```
1. 用户进入"今日计划"
   └─> GET /api/study/today
       └─> 后端查询：待复习词（按艾宾浩斯到期时间）+ 新学词（按配额）
       └─> 返回词卡列表（含释义 + 音频 URL + 图 URL）

2. 前端缓存词卡到本地（IndexedDB / uni storage）

3. 用户逐个翻卡 → 点"认识/不认识"
   └─> 前端先更新本地状态
   └─> POST /api/study/answer（单条上报，也支持批量）
       └─> 后端更新 user_word_progress 表的 stage/lastReviewed
       └─> 计算下次复习时间
       └─> 返回更新后的记录

4. 如果离线
   └─> 上报操作进入本地队列（localStorage / IndexedDB）
   └─> 联网后 POST /api/sync/pull + POST /api/sync/push 做双向合并

5. 完成后：
   └─> POST /api/checkin/today（幂等）
   └─> 前端刷新今日报表
```

### 多端同步冲突解决

所有学习操作带客户端时间戳 `clientTs`。同一个用户同一个单词的两条操作：

- **取 stage 较高的那条**（因为学习进度不应倒退，除非是"答错回退"，见下）
- **"答错回退"操作仍按时间戳取最新**（lastAnswer = 'wrong' 时强制覆盖）
- 服务端时间戳为准，客户端时间戳仅用于排序和冲突解决

详见 `docs/03-api-specification.md#同步`。

## 缓存策略

| 数据 | 位置 | TTL | 失效时机 |
|------|------|-----|---------|
| 词库词条 | Redis | 7 天 | 词条更新时主动失效 |
| 用户信息 | Redis | 30 分钟 | 用户修改信息时失效 |
| 今日计划 | Redis | 10 分钟 | 答题后增量更新 |
| 连续打卡天数 | Redis | 永久 | 每日 00:00 定时任务重算 |
| 等级元数据 | Redis | 永久（预热） | 启动时加载，手动失效 |
| 用户 session | Redis | 7 天 | JWT refresh |

## 安全架构

- **传输**：全站 HTTPS，HSTS 强制
- **认证**：JWT 双 Token 机制（accessToken 2h + refreshToken 7d）
- **密码**：bcrypt cost=12
- **敏感字段**：手机号 AES-256 加密入库，查询时脱敏
- **接口限流**：Redis + 漏桶（登录 5 次/分钟，注册 3 次/分钟，其他 60 次/分钟）
- **CSRF**：API 纯 JWT 无状态，天然免疫
- **XSS**：后端所有输出默认转义，前端用 Vue 的 `{{ }}` 插值（不用 `v-html`）
- **SQL 注入**：MyBatis-Plus 全参数化查询
- **第三方登录**：微信 / 苹果的 OAuth code 后端换 token，前端绝不持有 secret

## 部署拓扑

**开发环境（本地）**：
```yaml
docker-compose:
  - mysql:8.0        # :3306
  - redis:7-alpine   # :6379
  - backend          # :8080（Java 或 Python 二选一）
  - frontend-web     # :3000（Vite dev server）
```

**生产环境**：
```
Nginx (443)
  ├─ / → frontend-web 静态文件
  ├─ /api/ → backend (Spring Boot) :8080
  └─ /admin/ → frontend-web (同源，路由区分)

backend × 2 实例（负载均衡） → MySQL 主 + 从（读写分离可选）
                             → Redis（主 + 哨兵）
                             → 对象存储（音频/图片 CDN）
```

## 监控与日志

- **日志**：Logback / loguru，按天分割，WARN 及以上推 Sentry
- **指标**：Spring Boot Actuator + Prometheus + Grafana（或 SkyWalking）
- **接口埋点**：每个 API 记录耗时、状态码、用户 ID
- **业务埋点**：注册/登录/答题/打卡 写入单独的 events 表，供后续分析
