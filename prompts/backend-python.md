# Prompt — Python 后端开发（备选方案）

> 把这份 prompt 作为 Claude Code 在 `backend-python/` 目录下的起始任务描述。
>
> **注意**：Python 版是**备选/灰度**后端。API、错误码、DB schema、JWT 格式都**必须与 Java 版完全一致**，这样同一个前端可以切换后端而无感知。

## 你的角色

你是一位资深 Python 后端工程师，负责搭建 VocabMaster 的 FastAPI 异步后端。

## 必读文档

1. `../CLAUDE.md`
2. `../docs/00-README.md` → `docs/03-api-specification.md`
3. `../docs/04-ebbinghaus-algorithm.md`
4. `../docs/06-backend-python.md`（本端实现细则）
5. `../docs/05-backend-java.md`（**参照**对照 Java 实现，保证字段一致）

读完后问自己：「我写出来的 JSON 响应，Java 版的 Postman 请求能 100% 正确解析吗？」

## 硬性技术约束

| 项 | 要求 |
|----|------|
| Python | 3.12+ |
| 包管理 | uv（快）或 pip + venv |
| 框架 | FastAPI 0.115+ |
| ORM | SQLAlchemy 2.0+ **async** |
| 驱动 | aiomysql |
| Redis | redis[hiredis] ≥ 5.0，**async 客户端** |
| JWT | pyjwt 2.9+，算法 **HS256**，secret **与 Java 版同一个**（便于灰度） |
| 密码哈希 | passlib[bcrypt] cost=12 |
| 手机号加密 | pycryptodome AES-256-GCM（与 Java 版同 key） |
| 类型检查 | mypy strict（至少对 service 层） |
| 配置 | pydantic-settings |
| 启动 | uvicorn，prod 用 gunicorn + uvicorn worker |

## 与 Java 版的强约定

这些点如果不一致会导致前端切换后端时出 bug：

### 1. JSON 响应结构
```json
{
  "code": 0,
  "msg": "ok",
  "data": { ... },
  "request_id": "uuid"
}
```
时间字段统一 ISO 8601 UTC，例：`"2026-04-18T07:12:34.567Z"`。

### 2. 错误码
`common/error_code.py` 里的 `ErrorCode` 枚举**值必须与 Java 版的 `com.vocabmaster.common.ErrorCode` 一一对应**。每次加错误码两边同时加。

### 3. JWT payload
```python
{
    "sub": str(user_id),   # 用户 ID，字符串
    "jti": "<uuid>",       # token 唯一 ID，放 Redis 黑名单用
    "typ": "access",       # 或 "refresh"
    "iat": 1713..., 
    "exp": 1713..., 
    "iss": "vocabmaster"
}
```
**secret 通过环境变量 `VM_JWT_SECRET` 注入，Java 和 Python 读同一个值**。

### 4. 艾宾浩斯间隔
`INTERVALS_HOURS = [5/60, 0.5, 12, 24, 48, 96, 168, 360, 720]` —— **不许改**。

### 5. 数据库
两边**同一个 MySQL 实例、同一张 schema**。不要在 Python 侧建"专属字段"。

## 开发顺序

### 第 0 步：工程骨架
```
backend-python/
├── pyproject.toml          # uv / pip 用
├── .env.example
├── app/
│   ├── main.py             # FastAPI 入口
│   ├── core/
│   │   ├── config.py       # pydantic-settings
│   │   ├── security.py     # JWT / bcrypt / AES
│   │   ├── redis_client.py
│   │   ├── db.py           # async engine + session
│   │   └── exceptions.py
│   ├── common/
│   │   ├── response.py     # R 统一响应
│   │   ├── error_code.py
│   │   └── deps.py         # get_current_user 依赖
│   ├── models/             # SQLAlchemy ORM
│   ├── schemas/            # Pydantic DTO
│   ├── services/
│   ├── api/
│   │   └── v1/             # routers/auth.py, words.py, study.py ...
│   └── utils/
│       └── ebbinghaus.py   # 算法纯函数
└── tests/
```

### 第 1 步：基建
按 `docs/06-backend-python.md` 第 3~6 节抄代码：
1. `core/config.py` — 所有配置从环境变量
2. `common/response.py` — R 类 + `ok()` / `fail()` 辅助函数
3. `common/error_code.py` — 错误码枚举
4. `core/security.py` — `create_access_token` / `create_refresh_token` / `verify_token` / `hash_password` / `verify_password` / `encrypt_phone` / `hash_phone`
5. `core/db.py` — `async_engine` + `async_sessionmaker` + `get_db()` 依赖
6. 全局异常处理：`BusinessException` 捕获 + `RequestValidationError` 友好化
7. FastAPI 中间件：`X-Request-Id` 注入 + 响应头

### 第 2 步：ORM 模型
按 `sql/init.sql` 一对一映射所有表为 SQLAlchemy 模型。字段命名蛇形（与 DB 一致）。

### 第 3 步：认证模块
```
api/v1/auth.py
├── POST /auth/register/email
├── POST /auth/register/phone
├── POST /auth/login
├── POST /auth/refresh
├── POST /auth/logout
├── POST /auth/wechat/login
└── POST /auth/apple/login
```

对接微信用 `httpx.AsyncClient`，Apple 用 `python-jose` 验签 identityToken。

### 第 4 步：词库 + 学习 + 测试模块
重点：**`utils/ebbinghaus.py` 必须是纯函数**，方便写单元测试：

```python
def schedule(current_stage: int, result: Literal["correct", "wrong", "skip"]) -> ScheduleResult:
    """无任何 I/O、无副作用，与 Java EbbinghausScheduler.schedule 行为完全一致"""
    ...
```

单元测试必须对照 Java 版的 `EbbinghausSchedulerTest` 用例逐条复现。

### 第 5 步：其他模块
按 `docs/03-api-specification.md` 的 stats / checkin / sync / admin 模块挨个实现。

## 禁止事项

- ❌ 不要用 Flask / Django，**只用 FastAPI**
- ❌ 不要同步 DB 客户端（PyMySQL 直连），必须 async
- ❌ 不要把算法逻辑塞进 router，router 只做参数校验 + 调 service
- ❌ 不要用 Django-style 的"全局 settings 单例"，配置通过依赖注入
- ❌ 不要把 Pydantic schema 当 ORM 模型，两者分离
- ❌ 不要在 router 函数里 `try/except Exception` —— 让全局异常处理去拦

## 性能目标

| 指标 | 目标 |
|------|------|
| GET /words/levels | P99 < 50ms |
| POST /study/answer | P99 < 100ms |
| POST /auth/login | P99 < 200ms |
| 并发连接 | 单机 2000+（uvicorn 4 worker） |

## 开发环境启动

```bash
cd backend-python
uv venv && source .venv/bin/activate
uv pip install -e ".[dev]"

# 配置 .env（复制 .env.example）
# 确保 VM_JWT_SECRET 与 backend-java 一致

# 起服务
uvicorn app.main:app --reload --port 8081

# 访问 Swagger
open http://localhost:8081/docs
```

**Java 版用 8080 端口，Python 版用 8081 端口**，避免冲突。生产环境通过 Nginx upstream 切换。

## 你开始工作时的第一条回复

先输出：
1. "我已阅读 CLAUDE.md 和 docs/00~06"
2. 你对「Python 版必须与 Java 版字段级一致」的理解
3. 你的 pyproject.toml 依赖清单
4. 对哪些接口你需要 Java 版先实现好再对照（如 JWT payload 细节）

在我确认后再动代码。
