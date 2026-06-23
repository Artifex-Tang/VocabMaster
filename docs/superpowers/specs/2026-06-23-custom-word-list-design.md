# 自定义词库（Custom Word List）设计

- 日期：2026-06-23
- 状态：设计已确认，待评审
- 首批数据：剑桥《Think》教材 5 个级别（德语版双语词表，缺中文释义）

## 1. 背景与目标

用户拿到《Think》第一版 5 个 Excel 双语词表（德语版），共 **2639 词、60 单元**，需要纳入 App 用艾宾浩斯曲线背。本设计同时落地"自定义词库"这一一等概念，使《Think》成为首批**内置共享词库**，并为后续"用户上传 Excel 建个人词库"留好扩展位。

数据画像（实测）：

| 级别 | 词数 | 单元 | 短语 | 单词 |
|------|------|------|------|------|
| Starter | 596 | 12 | 94 | 502 |
| L2 | 489 | 12 | 203 | 286 |
| L3 | 470 | 12 | 193 | 277 |
| L4 | 566 | 12 | 181 | 385 |
| L5 | 518 | 12 | 215 | 303 |
| **合计** | **2639** | **60** | **886** | **1753** |

Excel 列：`Word / Unit No / Page / Definition(英) / PoS / Example(英) / CEF / IPA / Translation(德)`。

## 2. 关键决策

1. **学习流程 = 单元学新词 + 艾宾浩斯跨单元复习**（org 模型 A，用户已确认）。
   - 单元只负责"今天学哪些新词"（跟教材节奏）。
   - 复习由艾宾浩斯调度，跨单元混合。
2. **"词库"为新建一等概念**（非把 Think 当普通等级）。词存 `word_bank`，词库是 `word_bank` 之上的课程分组层。Think = 5 个内置共享词库。
3. **词必须进 `word_bank`**：艾宾浩斯引擎 `user_word_progress` 按 `(user_id, word_id, level_code)` 跑，整套学习/复习/测试/统计/打卡都绑 `word_bank.id`。否则需另造平行进度系统，不做。
4. **中文释义来源 = English Definition 列**（不是德语）。Excel 有剑桥原版英文释义，EN→ZH 质量高于 DE→ZH 且少一跳；德语列忽略。
5. **同词不同等级 = 不同记录，不合并**（遵循项目规则 #2）。Think 词即使已在 CET4/FCE，也作为 `THINK_*` 独立记录。

## 3. 数据模型

### 3.1 新增 3 张表（Flyway `V3__custom_word_list.sql`）

```sql
-- 词库（一等概念）
CREATE TABLE word_list (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  owner_user_id   BIGINT NULL,           -- NULL=系统内置共享；非空=用户个人上传
  name            VARCHAR(128) NOT NULL,
  description     VARCHAR(512),
  source_type     ENUM('builtin','imported') NOT NULL DEFAULT 'builtin',
  origin_level_code VARCHAR(32) NULL,    -- 内置教材词库挂 level_code（如 THINK_L2）
  word_count      INT NOT NULL DEFAULT 0,
  cover_emoji     VARCHAR(16),
  sort_order      INT NOT NULL DEFAULT 0,
  created_at      DATETIME(3),
  updated_at      DATETIME(3),
  deleted_at      DATETIME(3) NULL,
  INDEX idx_owner (owner_user_id),
  INDEX idx_source (source_type)
);

-- 词库项 = 课程分组（单元/页/顺序）
CREATE TABLE word_list_item (
  id        BIGINT PRIMARY KEY AUTO_INCREMENT,
  list_id   BIGINT NOT NULL,
  word_id   BIGINT NOT NULL,             -- → word_bank.id（强制存在）
  unit_no   INT NOT NULL,
  page      INT,
  sort_order INT NOT NULL DEFAULT 0,
  INDEX idx_list_unit (list_id, unit_no, sort_order),
  INDEX idx_word (word_id)
);

-- 用户订阅 + 单元游标
CREATE TABLE user_list_subscription (
  user_id          BIGINT NOT NULL,
  list_id          BIGINT NOT NULL,
  current_unit_no  INT NOT NULL DEFAULT 1,  -- "我学到第几单元"（仅作用域：新词引入）
  subscribed_at    DATETIME(3),
  updated_at       DATETIME(3),
  PRIMARY KEY (user_id, list_id)
);
```

### 3.2 复用不变的表

- `word_bank`：新增 5 个 `level_code`（`THINK_STARTER / THINK_L2 / THINK_L3 / THINK_L4 / THINK_L5`）。
- `level`：注册上述 5 行（驱动等级选择器，`LevelService.getLevels()` 读此表，Redis 缓存 `word:levels`，导入后需 evict）。
- `user_word_progress` / `study_log` / `wrong_word` / 测试 / 统计 / 打卡：**零改动**。

### 3.3 关系

```
word_list(Think L2) ──1:N── word_list_item(unit=3,page=42,order=5)
                                   │ word_id
                                   ▼
                              word_bank(levelCode=THINK_L2, "give up", en_def, zh_def, ipa, pos='phrasal verb'...)
                                   ▲
                                   │ word_id（学习/复习/测试都走它）
                              user_word_progress(user, stage 1-9, next_review_at)
```

词库与进度正交：词库管"词怎么分组/来自哪本教材"，进度管"这个词背到第几阶段"。

## 4. Think 数据导入流水线

脚本：`scripts/import_think_wordlist.py`（解析+翻译+插入，幂等）。

1. **解析** 5 个 Excel → 抽 `(word, unit, page, en_definition, pos, example_en, cef, ipa)`。
   - 级别按文件名映射：`Think_Starter→THINK_STARTER`、`Think_Level_2→THINK_L2` … `Think_Level_5→THINK_L5`。
2. **翻译** `en_definition → zh_definition`，DeepSeek-V3，批 30 词/请求，输出 `{word: zh}`。
   - 成本：2639÷30≈88 请求 ≈ ¥0.1。
   - **不翻译 example_zh**（全局未生成，YAGNI）。
3. **插 `word_bank`**：`level_code=THINK_*`、`word`、`word_lower`、`ipa_uk=ipa`、`pos`、`en_definition`、`zh_definition`、`example_en`、`difficulty/frequency/topic_code` 留空或按 cef 估。
   - 幂等：先按 `(level_code, word_lower)` 查重，已存在跳过。
4. **插 `level` 表** 5 行（code/nameZh/nameEn/sortOrder）。
5. **插 `word_list`** 5 行（builtin，owner_user_id=NULL，origin_level_code=THINK_*）。
6. **插 `word_list_item`**：每词一行，带 `unit_no/page/sort_order`。
7. **evict Redis `word:levels`**（LevelService.evictCache()）。

字段映射注意：Excel `IPA` 是单列 → 存 `ipa_uk`，`ipa_us` 留空（后续可用 TTS 补）；Excel `CEF`(CEFR A1-C2) 暂只存档，不直接映射 difficulty。

## 5. 学习流程

### 5.1 学新词（按单元，需新查询）

- 入口：用户在已订阅词库的"当前单元"点"学新词"。
- 拉词（**新 SQL**，`word_list_item` JOIN `word_bank`，反连接 `user_word_progress`）：
  ```sql
  SELECT w.* FROM word_list_item i
  JOIN word_bank w ON w.id = i.word_id
  WHERE i.list_id = :listId AND i.unit_no = :unit
    AND w.id NOT IN (SELECT word_id FROM user_word_progress
                     WHERE user_id = :userId AND stage > 0)
  ORDER BY i.sort_order LIMIT :limit
  ```
- 批量 = 用户每日新词配额（默认 20）。
- 卡片展示 word/ipa/zh_definition/例句 → 用户标"学会了"。
- 动作：复用 `POST /study/answer`（result=correct）→ `user_word_progress` stage 0→1，`first_learned_at=now`，`next_review_at = now + 5min`（艾宾浩斯第 1 阶段）。

### 5.2 复习（跨单元，零改动）

- 现有 `findDueForReview(userId, levelCode, now, limit)`：
  ```sql
  WHERE user_id = :userId AND level_code = :levelCode
    AND stage > 0 AND stage < 9 AND next_review_at <= :now
  ```
- **无 unit 过滤** → 一个 THINK 级内 12 单元的到期词天然混合 → 跨单元复习零改动。
- 调用：`POST /test/generate { level_code: "THINK_L2", source: "due" }`。
- 限制：**跨词库**（如 L2+L3+CET4 同池复习）当前查询不支持，MVP 不做；每词库各自复习。

### 5.3 单元推进

- `user_list_subscription.current_unit_no` 是新词引入的作用域游标。
- 单元"学完"定义 = 该单元所有词 `stage ≥ 1`（至少学过一次；**非** stage 9 掌握，太严会卡死）。
- 学完 → 前端显示"进入下一单元" → `current_unit_no++`（`POST /word-lists/{id}/units/{n}/advance`）。
- 用户可手动跳任意单元。

## 6. 短语测试适配（886 短语，34%）

- 现有测试模式 `VALID_MODES = {spelling, choice, listening}`。
- 判定短语：`word` 含空格 **或** `pos ∈ {phrase, phrasal verb}`（用 `word_bank.pos`，不加列）。
- 路由：
  - **单词**（1753）→ 三模式全开。
  - **短语**（886）→ **排除 `spelling`**（手打 "leave something to the last minute" 不现实）。`choice`（中英选择）、`listening`（听音频）OK。
- 改动：`TestService` 选词/出题时，若 `mode=spelling` 且命中短语 → 跳过该词或回退 choice。`buildChoices` 用 `pickDistractors(levelCode, topicCode, ...)`，Think 短语 `topic_code` 可能为空 → 干扰词选取需兼容 null topic（实现时验证）。

## 7. API（新增，后端 `wordlist` 业务包）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/word-lists?source_type=builtin` | 词库广场（内置 + 我的） |
| GET | `/word-lists/{id}` | 词库详情（总词数/单元数/我的进度） |
| GET | `/word-lists/{id}/units` | 单元列表（每单元词数/已学/已掌握） |
| POST | `/word-lists/{id}/subscribe` | 订阅（建 subscription，current_unit=1） |
| GET | `/word-lists/{id}/learn?unit={n}&limit={m}` | 拉当前单元新词（§5.1 新查询） |
| POST | `/word-lists/{id}/units/{n}/advance` | 推进单元游标 |

学习/复习/测试/答题：全复用现有 `/study/answer`、`/test/generate`、`/test/submit`（`level_code` 传 `THINK_*`）。

命名遵循项目约定：GET 查询参数 `snake_case` 后端 `@RequestParam` 显式映射；响应 `R<T>` / `PageResult<T>`。

## 8. 前端（Web 端，wordmate-web）

- 新增页：词库广场（浏览 5 个内置 Think 词库卡片）→ 订阅 → 单元列表（12 单元进度条）→ 当前单元"学新词"入口。
- 学新词卡片、复习、测试：复用现有组件，仅传 `level_code=THINK_*`。
- 入口：主导航/首页加"词库"入口。

## 9. 改动清单

| 层 | 改动 |
|----|------|
| DB | Flyway `V3` 建 3 表；`import_think_wordlist.py` 插 word_bank + level + word_list + word_list_item |
| 后端 | `wordlist` 业务包（Controller/Service/Mapper/DTO/Entity）；`TestService` 加短语守卫；新学新词查询 |
| 前端 Web | 词库广场页 + 订阅 + 单元列表 + 学新词入口；复用复习/测试页 |
| 脚本 | `import_think_wordlist.py`（解析+DeepSeek翻译+插入，幂等） |

## 10. 不做（YAGNI，后续增量）

- 用户 Excel 上传建个人词库（表已支持 `owner_user_id`，下增量加导入接口）。
- 手动选词组库 / 智能推荐。
- `example_zh` 翻译。
- 跨词库同池复习。

## 11. 风险与验证点

- **复习范围**：已核实 `findDueForReview` 按 level 维度、不过滤 unit → 跨单元零改动成立。跨词库不支持（已知限制）。
- **短语 topic_code 缺失**：`pickDistractors` 依赖 topic_code，Think 短语无 topic → 选择题干扰词降级策略需在实现时验证（可按 level 随机兜底）。
- **CEFR/difficulty 映射**：Excel CEF 列暂存档，不驱动难度，避免引入复杂映射。
- **幂等**：导入脚本按 `(level_code, word_lower)` 查重，重复运行安全。
- **缓存**：导入后必须 evict `word:levels`，否则等级选择器看不到 THINK_*。
