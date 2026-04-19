# 03 — API 规范

## 通用约定

### 基础

- **Base URL**：`https://api.vocabmaster.com/api/v1`（开发：`http://localhost:8080/api/v1`）
- **协议**：HTTPS + JSON
- **编码**：UTF-8
- **时间格式**：ISO 8601 带时区，如 `2026-04-17T10:30:00+08:00`；也接受 Unix 毫秒时间戳整数
- **命名**：URL 用短横线（`user-settings`），JSON 字段用下划线（`user_id`）。这里要留意：保持和 Java/Python 后端的 DTO 字段名一致，前端接收后转 camelCase 由前端拦截器完成。

### 认证

除少数公开接口（登录、注册、验证码、词库预览）外，全部需要在请求头携带：

```
Authorization: Bearer {accessToken}
X-Device-Id: {deviceUuid}
X-Device-Type: web | miniprogram | android
X-Client-Version: 1.0.0
```

`X-Device-Id` 前端生成一次并持久化，用于多端同步和限流。

### 统一响应格式

**成功**：

```json
{
  "code": 0,
  "msg": "ok",
  "data": { ... },
  "request_id": "7f3c..."
}
```

**失败**：

```json
{
  "code": 40001,
  "msg": "用户名或密码错误",
  "data": null,
  "request_id": "7f3c..."
}
```

HTTP 状态码：2xx = 业务成功、4xx = 客户端错误、5xx = 服务端错误；业务细分靠 `code` 字段。

### 错误码规范

- `0` — 成功
- `10xxx` — 通用错误（参数校验、限流）
- `20xxx` — 认证授权
- `30xxx` — 用户相关
- `40xxx` — 词库/学习相关
- `50xxx` — 同步相关
- `90xxx` — 管理后台
- `99999` — 未分类服务端错误

完整错误码表见本文末尾附录。

### 分页

```
GET /api/v1/xxx?page=1&page_size=20
```

响应：

```json
{
  "code": 0,
  "data": {
    "items": [...],
    "total": 123,
    "page": 1,
    "page_size": 20
  }
}
```

### 限流

| 接口 | 限制 |
|------|------|
| `/auth/register` | 3 次/分钟/IP |
| `/auth/login` | 5 次/分钟/IP |
| `/auth/send-code` | 1 次/分钟/手机号，10 次/天 |
| `/study/answer` | 60 次/分钟/用户 |
| 其他 | 120 次/分钟/用户 |

超限返回 `429 Too Many Requests` + `code: 10002`。

---

## 1. 认证模块 `/auth`

### 1.1 注册（邮箱/手机号）

`POST /auth/register`

```json
{
  "type": "email",            // email / phone
  "identifier": "abc@x.com",
  "password": "P@ssw0rd!",
  "code": "123456",           // 验证码（手机号必填）
  "nickname": "城外人"         // 可选
}
```

**响应**：

```json
{
  "code": 0,
  "data": {
    "user": { "uuid": "...", "nickname": "...", "email": "..." },
    "access_token": "eyJ...",
    "refresh_token": "eyJ...",
    "expires_in": 7200
  }
}
```

**错误**：
- `30001` — 邮箱/手机号已注册
- `30002` — 验证码错误
- `10001` — 密码强度不足（至少 8 位，含字母+数字）

---

### 1.2 发送验证码

`POST /auth/send-code`

```json
{
  "type": "phone",           // phone / email
  "identifier": "13800000000",
  "scene": "register"        // register / login / reset_password / bind
}
```

**响应**：`{ "code": 0, "data": { "expires_in": 300 } }`

**错误**：
- `10002` — 频率超限
- `30003` — 场景与已注册状态不符（如 register 但号码已存在）

---

### 1.3 登录（密码）

`POST /auth/login`

```json
{
  "type": "email",
  "identifier": "abc@x.com",
  "password": "P@ssw0rd!"
}
```

响应同注册。

---

### 1.4 登录（验证码）

`POST /auth/login-by-code`

```json
{
  "type": "phone",
  "identifier": "13800000000",
  "code": "123456"
}
```

如果号码未注册，自动完成注册并登录。

---

### 1.5 登录（微信小程序）

`POST /auth/login-wechat`

```json
{
  "code": "wx-login-code",      // wx.login() 拿到的 code
  "user_info": {                 // 可选，首次授权时前端 getUserProfile 后传入
    "nickname": "...",
    "avatar_url": "..."
  }
}
```

后端用 `code` 换 `openid` + `session_key`（+ `unionid` 如果有），已注册直接登录，未注册自动注册。

---

### 1.6 登录（苹果 OAuth）

`POST /auth/login-apple`

```json
{
  "identity_token": "eyJ...",
  "authorization_code": "..."
}
```

---

### 1.7 刷新 Token

`POST /auth/refresh`

```json
{ "refresh_token": "eyJ..." }
```

---

### 1.8 登出

`POST /auth/logout` — 将当前 accessToken 加入黑名单（Redis TTL = 剩余有效期）。

---

### 1.9 重置密码

`POST /auth/reset-password`

```json
{
  "type": "email",
  "identifier": "abc@x.com",
  "code": "123456",
  "new_password": "NewP@ssw0rd!"
}
```

---

## 2. 用户模块 `/user`

### 2.1 获取当前用户信息

`GET /user/me`

```json
{
  "code": 0,
  "data": {
    "uuid": "...",
    "nickname": "...",
    "avatar_url": "...",
    "email": "...",
    "phone_masked": "138****0000",    // 脱敏
    "timezone": "Asia/Shanghai",
    "locale": "zh-CN",
    "bound_providers": ["wechat"],
    "created_at": "2026-01-01T00:00:00+08:00"
  }
}
```

### 2.2 更新用户信息

`PATCH /user/me`

```json
{
  "nickname": "...",
  "avatar_url": "...",
  "timezone": "Asia/Shanghai"
}
```

### 2.3 获取用户设置

`GET /user/settings`

```json
{
  "code": 0,
  "data": {
    "daily_new_words_goal": 20,
    "daily_review_goal": 100,
    "default_sort_mode": "alpha",
    "preferred_accent": "uk",
    "auto_play_audio": true,
    "notification_time": "20:00:00",
    "theme": "light",
    "active_levels": ["CET4", "FCE"]
  }
}
```

### 2.4 更新用户设置

`PATCH /user/settings` — 部分更新，传哪些字段更新哪些。

### 2.5 导出学习数据

`GET /user/export?format=csv` — 返回用户所有学习记录的 CSV 下载链接（24h 有效）。

### 2.6 删除账户

`POST /user/delete-account`

```json
{ "confirm_code": "123456" }    // 需要二次验证码确认
```

软删除用户，保留 30 天内可恢复。

---

## 3. 词库模块 `/words`

### 3.1 获取等级列表

`GET /words/levels` — 公开接口，无需登录

```json
{
  "code": 0,
  "data": [
    { "code": "KET", "name_zh": "KET剑桥入门", "name_en": "KET", "target_word_count": 1500, "sort_order": 1 },
    ...
  ]
}
```

### 3.2 获取主题列表

`GET /words/topics`

### 3.3 查询单词详情

`GET /words/{id}` 或 `GET /words/by-word?level=CET4&word=abandon`

```json
{
  "code": 0,
  "data": {
    "id": 12345,
    "level_code": "CET4",
    "word": "abandon",
    "ipa_uk": "/əˈbæn.dən/",
    "ipa_us": "/əˈbæn.dən/",
    "en_definition": "To leave completely and permanently.",
    "zh_definition": "v. 放弃；抛弃",
    "example_en": "They had to abandon the project.",
    "example_zh": "他们不得不放弃这个项目。",
    "topic_code": "ACTION",
    "audio_url_uk": "https://cdn.../abandon_uk.mp3",
    "audio_url_us": "https://cdn.../abandon_us.mp3",
    "image_url": "https://cdn.../abandon.jpg",
    "emoji": "🚪",
    "pos": "v.",
    "related_words": {
      "synonyms": ["desert", "forsake"],
      "antonyms": ["keep", "maintain"]
    }
  }
}
```

### 3.4 批量下载某等级词库（离线用）

`GET /words/download?level=CET4&version=20260401`

支持增量：传入 `since=20260301`，只返回此后新增/更新的词条。

```json
{
  "code": 0,
  "data": {
    "level_code": "CET4",
    "version": "20260417",
    "total": 4523,
    "words": [ /* 完整词条数组 */ ]
  }
}
```

建议返回前用 gzip 压缩，一个 5000 词的等级压缩后约 800KB。

### 3.5 搜索单词

`GET /words/search?q=aband&level=CET4&page=1&page_size=20`

---

## 4. 学习模块 `/study`

### 4.1 获取今日学习计划

`GET /study/today?level=CET4`

返回今日需要学习的词卡列表（复习 + 新学）。

```json
{
  "code": 0,
  "data": {
    "date": "2026-04-17",
    "review_words": [ /* 到期需复习的词卡 */ ],
    "new_words": [ /* 新学词，按用户设置的数量和排序方式 */ ],
    "review_count": 45,
    "new_count": 20,
    "estimated_minutes": 15
  }
}
```

**后端逻辑**：
1. 查 `user_word_progress WHERE user_id=? AND level_code=? AND stage>0 AND stage<9 AND next_review_at<=NOW()`，按 next_review_at 升序
2. 查 `user_settings.daily_new_words_goal`
3. 查 `word_bank` 中该用户未学过的词（LEFT JOIN user_word_progress WHERE progress IS NULL），按 `default_sort_mode` 排序，取 N 条

### 4.2 上报答题结果

`POST /study/answer`

```json
{
  "word_id": 12345,
  "level_code": "CET4",
  "result": "correct",          // correct / wrong / skip
  "mode": "card",               // card / spelling / choice / listening
  "duration_ms": 3200,
  "client_ts": "2026-04-17T10:30:15.123+08:00"
}
```

**响应**：

```json
{
  "code": 0,
  "data": {
    "word_id": 12345,
    "stage_before": 3,
    "stage_after": 4,
    "next_review_at": "2026-04-19T10:30:15+08:00",
    "mastered": false
  }
}
```

### 4.3 批量上报（离线同步用）

`POST /study/answer-batch`

```json
{
  "answers": [
    { "word_id": 1, "result": "correct", "client_ts": "...", ... },
    { "word_id": 2, "result": "wrong", "client_ts": "...", ... }
  ]
}
```

后端按 `client_ts` 排序逐条处理，冲突按 `docs/01-architecture.md#多端同步冲突解决` 规则解决。

响应返回每条的处理结果（成功/跳过/冲突后的最终状态）。

### 4.4 重置单词学习进度

`POST /study/reset`

```json
{
  "word_id": 12345      // 单词 ID，也支持 level_code 重置整个等级
}
```

### 4.5 标记为"已掌握"（跳过学习）

`POST /study/mark-mastered`

```json
{ "word_id": 12345 }
```

直接置 stage = 9。

---

## 5. 测试模块 `/test`

### 5.1 生成拼写测试

`POST /test/generate`

```json
{
  "level_code": "CET4",
  "mode": "spelling",        // spelling / choice / listening
  "size": 20,
  "source": "due"            // due / all / wrong_words / custom
}
```

**响应**：

```json
{
  "code": 0,
  "data": {
    "test_id": "tst_xxx",
    "mode": "spelling",
    "questions": [
      {
        "question_id": "q1",
        "word_id": 12345,
        "prompt": {                 // 根据模式不同
          "zh_definition": "v. 放弃；抛弃",
          "audio_url_uk": "..."     // spelling 模式不给 word，给 zh/音频
        }
      }
    ]
  }
}
```

choice 模式的 prompt 会含 4 个选项的 word 列表。

### 5.2 提交测试答案

`POST /test/submit`

```json
{
  "test_id": "tst_xxx",
  "answers": [
    { "question_id": "q1", "answer": "abandon", "duration_ms": 5000 },
    ...
  ]
}
```

**响应**：包含每题对错、正确答案、准确率。每题的结果同时写入 `study_log` 并触发进度更新（同 `/study/answer` 的逻辑）。

---

## 6. 统计模块 `/stats`

### 6.1 今日统计

`GET /stats/today`

```json
{
  "code": 0,
  "data": {
    "date": "2026-04-17",
    "words_learned": 18,
    "words_reviewed": 42,
    "correct_count": 55,
    "accuracy": 0.917,
    "duration_seconds": 945,
    "goal_progress": { "new": "18/20", "review": "42/100" }
  }
}
```

### 6.2 周报/月报

`GET /stats/summary?period=week&date=2026-04-17`

`period` 支持 `week` / `month` / `custom`（custom 需传 start_date、end_date）。

```json
{
  "code": 0,
  "data": {
    "period": "week",
    "start_date": "2026-04-13",
    "end_date": "2026-04-19",
    "days_active": 5,
    "total_learned": 85,
    "total_reviewed": 320,
    "avg_accuracy": 0.88,
    "daily_breakdown": [
      { "date": "2026-04-13", "learned": 20, "reviewed": 50, "accuracy": 0.9 },
      ...
    ],
    "level_breakdown": [
      { "level_code": "CET4", "mastered": 450, "learning": 200 }
    ],
    "topic_heatmap": [
      { "topic_code": "FOOD", "mastered": 30, "total": 45 }
    ]
  }
}
```

### 6.3 个人遗忘曲线数据

`GET /stats/forgetting-curve?word_id=12345`

返回某个单词的每次复习节点（实际数据）+ 艾宾浩斯理论曲线参数，前端叠加绘制。

```json
{
  "code": 0,
  "data": {
    "word_id": 12345,
    "word": "abandon",
    "reviews": [
      { "ts": "2026-04-01T10:00:00+08:00", "result": "correct", "stage_after": 1 },
      { "ts": "2026-04-01T10:05:00+08:00", "result": "correct", "stage_after": 2 },
      ...
    ],
    "theoretical_curve": {
      "type": "ebbinghaus",
      "stages": [0.083, 0.5, 12, 24, 48, 96, 168, 360, 720]   // 单位：小时
    }
  }
}
```

### 6.4 等级概览

`GET /stats/level-overview?level=CET4`

```json
{
  "code": 0,
  "data": {
    "level_code": "CET4",
    "total_words": 4523,
    "not_started": 3523,
    "learning": 800,
    "mastered": 200,
    "mastery_rate": 0.044,
    "stage_distribution": [
      { "stage": 1, "count": 150 },
      { "stage": 2, "count": 120 },
      ...
    ]
  }
}
```

---

## 7. 打卡模块 `/checkin`

### 7.1 今日打卡

`POST /checkin/today` — 幂等，今日已打卡返回已有记录。

```json
{
  "code": 0,
  "data": {
    "date": "2026-04-17",
    "current_streak": 12,
    "longest_streak": 45,
    "total_days": 128,
    "new_achievements": [        // 本次打卡触发的新成就
      { "code": "STREAK_7", "name_zh": "坚持7天" }
    ]
  }
}
```

### 7.2 打卡日历

`GET /checkin/calendar?month=2026-04`

```json
{
  "code": 0,
  "data": {
    "month": "2026-04",
    "days": [
      { "date": "2026-04-01", "checked_in": true, "words_count": 45 },
      { "date": "2026-04-02", "checked_in": false },
      ...
    ],
    "current_streak": 12,
    "longest_streak": 45
  }
}
```

### 7.3 成就列表

`GET /checkin/achievements`

```json
{
  "code": 0,
  "data": {
    "unlocked": [
      { "code": "STREAK_7", "name_zh": "坚持7天", "icon": "...", "achieved_at": "..." }
    ],
    "locked": [
      { "code": "STREAK_30", "name_zh": "坚持30天", "progress": "12/30" }
    ]
  }
}
```

---

## 8. 错题本 `/wrong-words`

### 8.1 获取错题列表

`GET /wrong-words?level=CET4&resolved=0&page=1&page_size=20`

### 8.2 开始错题复习

`POST /wrong-words/review` — 返回一组错题卡片（同 `/study/today` 格式）。

### 8.3 标记为已解决

`POST /wrong-words/resolve`（一般不手动调，连续答对 3 次自动置 resolved=1）

---

## 9. 同步模块 `/sync`

### 9.1 拉取（增量）

`GET /sync/pull?since=2026-04-16T10:00:00+08:00&device_id=xxx`

返回该设备最后同步之后，其他设备产生的学习记录变更。

```json
{
  "code": 0,
  "data": {
    "server_ts": "2026-04-17T10:30:00+08:00",
    "changes": {
      "progress": [ /* user_word_progress 变更 */ ],
      "checkin": [ /* 打卡变更 */ ],
      "settings": { /* 设置变更 */ }
    }
  }
}
```

### 9.2 推送（离线队列）

`POST /sync/push`

```json
{
  "device_id": "xxx",
  "answers": [ /* 同 /study/answer-batch */ ],
  "checkins": [ { "checkin_date": "2026-04-17", ... } ]
}
```

响应含每条记录的处理结果（accepted/conflict/rejected）和服务端最终状态。

---

## 10. 管理后台 `/admin`（仅管理员）

### 10.1 词库管理

- `GET /admin/words` — 分页查询
- `POST /admin/words` — 新增
- `PATCH /admin/words/{id}` — 更新
- `POST /admin/words/import` — CSV 批量导入
- `POST /admin/words/{id}/audit` — 审核通过/拒绝

### 10.2 用户管理

- `GET /admin/users` — 分页查询
- `PATCH /admin/users/{uuid}` — 修改状态（禁用/解禁）

### 10.3 统计看板

- `GET /admin/dashboard` — DAU / 新注册 / 打卡率等

---

## 附录 A：错误码表

| Code | Msg | HTTP |
|------|-----|------|
| 0 | ok | 200 |
| 10001 | 参数校验失败 | 400 |
| 10002 | 请求过于频繁 | 429 |
| 10003 | 不支持的 Content-Type | 415 |
| 10099 | 通用客户端错误 | 400 |
| 20001 | 未登录或 token 无效 | 401 |
| 20002 | token 已过期 | 401 |
| 20003 | 无权限 | 403 |
| 20004 | refresh_token 无效 | 401 |
| 30001 | 账号已存在 | 409 |
| 30002 | 验证码错误或已过期 | 400 |
| 30003 | 账号不存在 | 404 |
| 30004 | 密码错误 | 401 |
| 30005 | 账号已禁用 | 403 |
| 30006 | 第三方登录失败 | 400 |
| 40001 | 词条不存在 | 404 |
| 40002 | 等级不存在 | 404 |
| 40003 | 不属于该用户 | 403 |
| 40004 | 无效的答题结果 | 400 |
| 40005 | 测试 ID 无效或已过期 | 400 |
| 50001 | 同步冲突 | 409 |
| 50002 | client_ts 过旧 | 400 |
| 90001 | 仅管理员可访问 | 403 |
| 90002 | 词条重复 | 409 |
| 99999 | 服务端错误 | 500 |

## 附录 B：OpenAPI / Swagger

Java 端用 SpringDoc（`springdoc-openapi-starter-webmvc-ui`），访问 `/swagger-ui.html`。

Python 端 FastAPI 自带 Swagger，访问 `/docs`。

前端开发时使用 Apifox 或 OpenAPI Generator 自动生成 TypeScript 接口定义，避免手写。
