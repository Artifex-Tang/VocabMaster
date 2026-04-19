# 06 — Python 后端实现规范（备选）

> 备选方案。如果未来需要接入 Claude API、NLP 处理（自动生成例句、词形变化分析、难度评估）等 AI 场景，Python 侧更顺手。业务 API 与 Java 侧**完全一致**（同一个 `03-api-specification.md`），两端可互为备用。

## 技术栈

| 组件 | 版本 |
|------|------|
| Python | 3.12 |
| FastAPI | 0.115+ |
| SQLAlchemy | 2.0+（async） |
| Alembic | 最新 |
| Pydantic | 2.x |
| uvicorn + gunicorn | 生产部署 |
| redis-py | 5.x（async） |
| aiomysql | MySQL 异步驱动 |
| passlib[bcrypt] | 密码哈希 |
| python-jose[cryptography] | JWT |
| cryptography | AES |
| httpx | 异步 HTTP 客户端（微信/苹果 OAuth） |
| pytest + pytest-asyncio | 测试 |

## 项目结构

```
backend-python/
├── pyproject.toml                      # 依赖管理（用 uv 或 poetry）
├── alembic.ini
├── alembic/
│   ├── env.py
│   └── versions/
│       └── 0001_init.py
├── app/
│   ├── __init__.py
│   ├── main.py                         # FastAPI 入口
│   ├── config.py                       # 设置（Pydantic Settings）
│   ├── db.py                           # 数据库引擎 + session
│   ├── redis_client.py
│   ├── deps.py                         # 通用依赖（get_current_user 等）
│   ├── models/                         # SQLAlchemy ORM
│   │   ├── __init__.py
│   │   ├── user.py
│   │   ├── word.py
│   │   ├── progress.py
│   │   ├── checkin.py
│   │   └── log.py
│   ├── schemas/                        # Pydantic DTO
│   │   ├── auth.py
│   │   ├── user.py
│   │   ├── word.py
│   │   ├── study.py
│   │   └── common.py
│   ├── core/                           # 业务核心
│   │   ├── ebbinghaus.py               # 算法
│   │   ├── security.py                 # JWT/密码/AES
│   │   ├── rate_limit.py
│   │   └── errors.py                   # 错误码
│   ├── services/                       # 业务服务层
│   │   ├── auth_service.py
│   │   ├── user_service.py
│   │   ├── word_service.py
│   │   ├── study_service.py
│   │   ├── test_service.py
│   │   ├── stats_service.py
│   │   ├── checkin_service.py
│   │   ├── sync_service.py
│   │   └── admin_service.py
│   ├── api/                            # 路由
│   │   ├── __init__.py
│   │   └── v1/
│   │       ├── __init__.py
│   │       ├── auth.py
│   │       ├── user.py
│   │       ├── word.py
│   │       ├── study.py
│   │       ├── test.py
│   │       ├── stats.py
│   │       ├── checkin.py
│   │       ├── sync.py
│   │       ├── wrong_word.py
│   │       └── admin.py
│   ├── middleware/
│   │   ├── request_id.py
│   │   ├── error_handler.py
│   │   └── rate_limit.py
│   └── utils/
│       ├── time_util.py
│       └── id_gen.py
├── tests/
│   ├── conftest.py
│   ├── test_ebbinghaus.py
│   ├── test_study_service.py
│   └── ...
└── Dockerfile
```

## 关键实现指引

### 1. 配置（Pydantic Settings）

```python
# app/config.py
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    APP_NAME: str = "vocabmaster"
    ENV: str = "dev"
    
    MYSQL_URL: str = "mysql+aiomysql://vocab:vocab123@localhost:3306/vocabmaster"
    REDIS_URL: str = "redis://localhost:6379/0"
    
    JWT_SECRET: str
    JWT_ACCESS_TTL: int = 7200
    JWT_REFRESH_TTL: int = 604800
    AES_KEY: str  # 32 bytes
    
    WECHAT_APP_ID: str | None = None
    WECHAT_APP_SECRET: str | None = None
    
    class Config:
        env_file = ".env"

settings = Settings()
```

### 2. 统一响应

```python
# app/schemas/common.py
from typing import Generic, TypeVar
from pydantic import BaseModel

T = TypeVar("T")

class R(BaseModel, Generic[T]):
    code: int = 0
    msg: str = "ok"
    data: T | None = None
    request_id: str | None = None

    @classmethod
    def ok(cls, data: T | None = None) -> "R[T]":
        return cls(code=0, msg="ok", data=data)

    @classmethod
    def fail(cls, code: int, msg: str) -> "R[T]":
        return cls(code=code, msg=msg, data=None)
```

### 3. 错误码

```python
# app/core/errors.py
from enum import Enum

class ErrorCode(Enum):
    SUCCESS = (0, "ok")
    PARAM_INVALID = (10001, "参数校验失败")
    RATE_LIMIT = (10002, "请求过于频繁")
    UNAUTHORIZED = (20001, "未登录或 token 无效")
    # ... 与 Java 侧完全一致
    SYNC_CONFLICT = (50001, "同步冲突")
    SERVER_ERROR = (99999, "服务端错误")
    
    def __init__(self, code: int, msg: str):
        self.code_value = code
        self.msg = msg

class BizException(Exception):
    def __init__(self, ec: ErrorCode, msg: str | None = None):
        self.ec = ec
        self.msg = msg or ec.msg
        super().__init__(self.msg)
```

### 4. 艾宾浩斯调度器

```python
# app/core/ebbinghaus.py
from datetime import datetime, timedelta, timezone
from dataclasses import dataclass
from enum import Enum

class AnswerResult(str, Enum):
    CORRECT = "correct"
    WRONG = "wrong"
    SKIP = "skip"

INTERVALS_HOURS = [5/60, 0.5, 12, 24, 48, 96, 168, 360, 720]
MAX_STAGE = 9

@dataclass
class SchedulerResult:
    stage_before: int
    stage_after: int
    next_review_at: datetime
    just_mastered: bool

def schedule(stage_before: int, result: AnswerResult, now: datetime) -> SchedulerResult:
    if result == AnswerResult.CORRECT:
        stage_after = min(stage_before + 1, MAX_STAGE)
    elif result == AnswerResult.WRONG:
        stage_after = max(1, stage_before - 1)
    elif result == AnswerResult.SKIP:
        stage_after = max(1, stage_before)
    else:
        raise ValueError(f"invalid result: {result}")
    
    if stage_after >= MAX_STAGE:
        next_review = now + timedelta(days=30)
    else:
        hours = INTERVALS_HOURS[stage_after - 1]
        next_review = now + timedelta(seconds=int(hours * 3600))
    
    just_mastered = stage_before < MAX_STAGE and stage_after == MAX_STAGE
    return SchedulerResult(stage_before, stage_after, next_review, just_mastered)
```

### 5. 学习服务

```python
# app/services/study_service.py
from datetime import datetime, timezone
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.models.progress import UserWordProgress
from app.models.log import StudyLog
from app.core.ebbinghaus import schedule, AnswerResult
from app.core.errors import BizException, ErrorCode
from app.schemas.study import AnswerRequest, AnswerResponse

async def answer(db: AsyncSession, user_id: int, req: AnswerRequest) -> AnswerResponse:
    # 1. 获取或创建 progress
    q = select(UserWordProgress).where(
        UserWordProgress.user_id == user_id,
        UserWordProgress.word_id == req.word_id
    )
    result = await db.execute(q)
    progress = result.scalar_one_or_none()
    is_new = progress is None
    
    if is_new:
        progress = UserWordProgress(
            user_id=user_id,
            word_id=req.word_id,
            level_code=req.level_code,
            stage=0,
            correct_count=0,
            wrong_count=0,
        )
        db.add(progress)
    
    # 2. 冲突检测
    if (progress.client_updated_at and req.client_ts 
        and req.client_ts < progress.client_updated_at):
        raise BizException(ErrorCode.SYNC_CONFLICT)
    
    # 3. 调度
    now = datetime.now(timezone.utc)
    stage_before = progress.stage
    sr = schedule(stage_before, req.result, now)
    
    # 4. 更新 progress
    progress.stage = sr.stage_after
    progress.last_reviewed_at = now
    progress.next_review_at = sr.next_review_at
    progress.client_updated_at = req.client_ts
    if req.result == AnswerResult.CORRECT:
        progress.correct_count += 1
    elif req.result == AnswerResult.WRONG:
        progress.wrong_count += 1
    if stage_before == 0 and sr.stage_after > 0:
        progress.first_learned_at = now
    if sr.just_mastered:
        progress.mastered_at = now
    
    # 5. 写日志
    db.add(StudyLog(
        user_id=user_id, word_id=req.word_id,
        level_code=req.level_code,
        action="learn" if stage_before == 0 else "review",
        result=req.result.value,
        mode=req.mode,
        stage_before=stage_before,
        stage_after=sr.stage_after,
        duration_ms=req.duration_ms,
        client_ts=req.client_ts,
    ))
    
    # 6. 答错入错题本
    if req.result == AnswerResult.WRONG:
        from app.services.wrong_word_service import upsert_wrong_word
        await upsert_wrong_word(db, user_id, req.word_id, req.level_code)
    
    await db.commit()
    await db.refresh(progress)
    
    return AnswerResponse(
        word_id=req.word_id,
        stage_before=stage_before,
        stage_after=sr.stage_after,
        next_review_at=sr.next_review_at,
        mastered=progress.mastered_at is not None,
    )
```

### 6. 认证依赖

```python
# app/deps.py
from fastapi import Depends, Header, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from app.db import get_db
from app.core.security import decode_token
from app.models.user import User
from sqlalchemy import select

async def get_current_user(
    authorization: str = Header(...),
    db: AsyncSession = Depends(get_db),
) -> User:
    if not authorization.startswith("Bearer "):
        raise HTTPException(401, "invalid auth header")
    token = authorization[7:]
    payload = decode_token(token)
    user_id = payload.get("uid")
    q = select(User).where(User.id == user_id, User.deleted_at.is_(None))
    result = await db.execute(q)
    user = result.scalar_one_or_none()
    if not user:
        raise HTTPException(401, "user not found")
    return user
```

### 7. 路由

```python
# app/api/v1/study.py
from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession
from app.db import get_db
from app.deps import get_current_user
from app.models.user import User
from app.schemas.common import R
from app.schemas.study import AnswerRequest, AnswerResponse, TodayPlanResponse
from app.services import study_service, today_plan_service

router = APIRouter(prefix="/study", tags=["学习"])

@router.get("/today", response_model=R[TodayPlanResponse])
async def get_today(
    level: str = Query(...),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    plan = await today_plan_service.get_today_plan(db, user.id, level)
    return R.ok(plan)

@router.post("/answer", response_model=R[AnswerResponse])
async def answer(
    req: AnswerRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    resp = await study_service.answer(db, user.id, req)
    return R.ok(resp)
```

### 8. 入口 main.py

```python
# app/main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.v1 import auth, user, word, study, test, stats, checkin, sync, wrong_word, admin
from app.middleware.error_handler import register_handlers
from app.middleware.request_id import RequestIdMiddleware

app = FastAPI(
    title="VocabMaster API",
    version="1.0.0",
    openapi_url="/api/v1/openapi.json",
    docs_url="/api/v1/docs",
)

app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])
app.add_middleware(RequestIdMiddleware)

register_handlers(app)

for r in (auth, user, word, study, test, stats, checkin, sync, wrong_word, admin):
    app.include_router(r.router, prefix="/api/v1")

@app.get("/health")
async def health():
    return {"status": "ok"}
```

### 9. pyproject.toml

```toml
[project]
name = "vocabmaster-backend"
version = "1.0.0"
requires-python = ">=3.12"
dependencies = [
    "fastapi>=0.115",
    "uvicorn[standard]>=0.30",
    "gunicorn>=22",
    "sqlalchemy[asyncio]>=2.0",
    "alembic>=1.13",
    "aiomysql>=0.2",
    "redis[hiredis]>=5.0",
    "pydantic>=2.5",
    "pydantic-settings>=2.1",
    "passlib[bcrypt]>=1.7",
    "python-jose[cryptography]>=3.3",
    "cryptography>=42",
    "httpx>=0.27",
    "python-multipart>=0.0.9",
]

[project.optional-dependencies]
dev = [
    "pytest>=8.0",
    "pytest-asyncio>=0.23",
    "pytest-cov>=5.0",
    "ruff>=0.3",
    "mypy>=1.8",
    "testcontainers[mysql,redis]>=4.0",
]
```

### 10. 与 Java 版本的一致性保证

- **同一份 OpenAPI**：两边都生成 openapi.json，手动比对或用 schemadiff 工具
- **同一份错误码**：`errors.py` 和 `ErrorCode.java` 必须保持映射一致
- **同一份数据库 schema**：Alembic 和 Flyway 的初始版本从同一份 `sql/init.sql` 派生
- **同一份艾宾浩斯测试用例**：`test_ebbinghaus.py` 和 Java 侧的 `EbbinghausSchedulerTest` 用相同输入输出断言
- **同一个 JWT 格式**：两边都用 HS256，secret 配成同一个，可互相解析对方签发的 token（便于灰度切换）

## 部署建议

```bash
# 生产启动
gunicorn app.main:app \
    --worker-class uvicorn.workers.UvicornWorker \
    --workers 4 \
    --bind 0.0.0.0:8080 \
    --access-logfile - \
    --error-logfile -
```

Dockerfile：

```dockerfile
FROM python:3.12-slim AS base
WORKDIR /app
RUN pip install --no-cache-dir uv
COPY pyproject.toml .
RUN uv pip install --system .
COPY app/ ./app/
COPY alembic/ ./alembic/
COPY alembic.ini .
EXPOSE 8080
CMD ["gunicorn", "app.main:app", "--worker-class", "uvicorn.workers.UvicornWorker", "--workers", "4", "--bind", "0.0.0.0:8080"]
```
