# 02 — 数据库设计

## 设计原则

1. **命名**：表名单数、下划线蛇形（`user`、`word_bank`、`user_word_progress`）
2. **主键**：全部用 `BIGINT` 自增 `id`，业务键另建唯一索引
3. **时间**：所有表含 `created_at` `updated_at`，存 UTC `DATETIME(3)`
4. **软删除**：重要业务表（user、word_bank）加 `deleted_at`；日志类（study_log）物理删除
5. **字符集**：`utf8mb4` + `utf8mb4_0900_ai_ci`（MySQL 8 默认）
6. **外键**：不建数据库外键（性能 + 灵活性），用应用层保证完整性

## 表清单

| 表名 | 用途 | 量级预估 |
|------|------|---------|
| `user` | 用户账号 | 万级 |
| `user_auth` | 第三方登录绑定 | 万级 |
| `user_settings` | 用户配置（JSON） | 万级 |
| `level` | 等级字典 | 10 条 |
| `word_bank` | 词库（全部等级的单词） | ~5 万 |
| `word_topic` | 主题字典 | ~50 |
| `user_word_progress` | 用户学习进度 | 千万级（用户×单词） |
| `study_log` | 学习行为日志 | 亿级（按月分区） |
| `checkin` | 每日打卡 | 百万级 |
| `user_streak` | 连续打卡统计 | 万级（每用户一条） |
| `achievement` | 成就徽章字典 | ~20 |
| `user_achievement` | 用户获得的成就 | 十万级 |
| `wrong_word` | 错题本 | 百万级 |
| `sync_token` | 多端同步令牌 | 万级 |
| `admin_log` | 管理操作日志 | 十万级 |

## 完整 DDL

见 `sql/init.sql`。以下是核心表的说明。

---

### `user`

```sql
CREATE TABLE `user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `uuid` CHAR(36) NOT NULL COMMENT '对外暴露的 ID，避免枚举',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名，可选',
  `phone` VARCHAR(128) DEFAULT NULL COMMENT 'AES 加密后的手机号',
  `phone_hash` CHAR(64) DEFAULT NULL COMMENT '手机号 SHA-256，用于查询',
  `email` VARCHAR(100) DEFAULT NULL,
  `password_hash` VARCHAR(100) DEFAULT NULL COMMENT 'bcrypt',
  `avatar_url` VARCHAR(500) DEFAULT NULL,
  `nickname` VARCHAR(50) DEFAULT NULL,
  `timezone` VARCHAR(50) NOT NULL DEFAULT 'Asia/Shanghai',
  `locale` VARCHAR(10) NOT NULL DEFAULT 'zh-CN',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  `last_login_at` DATETIME(3) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at` DATETIME(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uuid` (`uuid`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_phone_hash` (`phone_hash`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**注意**：
- `phone` 加密存储、`phone_hash` 用于唯一性校验和登录查询
- `email` 和 `phone_hash` 都允许 NULL（第三方登录用户可能两者都没有）
- `uuid` 用于 JWT sub 字段，API 中只传 uuid，不传 id

---

### `user_auth`

第三方登录绑定（一个用户可绑定多个第三方账号）。

```sql
CREATE TABLE `user_auth` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `provider` VARCHAR(20) NOT NULL COMMENT 'wechat / apple / google',
  `provider_user_id` VARCHAR(100) NOT NULL COMMENT '第三方的 openid / sub',
  `union_id` VARCHAR(100) DEFAULT NULL COMMENT '微信 unionid',
  `raw_profile` JSON DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_pid` (`provider`, `provider_user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_union_id` (`union_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### `user_settings`

```sql
CREATE TABLE `user_settings` (
  `user_id` BIGINT UNSIGNED NOT NULL,
  `daily_new_words_goal` INT NOT NULL DEFAULT 20 COMMENT '每日新学词目标',
  `daily_review_goal` INT NOT NULL DEFAULT 100 COMMENT '每日复习目标',
  `default_sort_mode` VARCHAR(10) NOT NULL DEFAULT 'alpha' COMMENT 'alpha/topic/random',
  `preferred_accent` VARCHAR(4) NOT NULL DEFAULT 'uk' COMMENT 'uk/us',
  `auto_play_audio` TINYINT NOT NULL DEFAULT 1,
  `notification_time` TIME DEFAULT '20:00:00' COMMENT '提醒学习时间',
  `theme` VARCHAR(10) NOT NULL DEFAULT 'light' COMMENT 'light/dark/auto',
  `active_levels` JSON DEFAULT NULL COMMENT '用户当前在学的等级数组，如 ["CET4","FCE"]',
  `extra` JSON DEFAULT NULL COMMENT '预留扩展字段',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### `level`

```sql
CREATE TABLE `level` (
  `code` VARCHAR(20) NOT NULL COMMENT 'KET/PET/FCE/CAE/PRIMARY/JUNIOR/SENIOR/CET4/CET6/TEM8',
  `name_zh` VARCHAR(50) NOT NULL,
  `name_en` VARCHAR(50) NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `target_word_count` INT NOT NULL COMMENT '目标词汇量',
  `description` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**初始化数据**（见 `sql/init.sql`）：10 个等级预置。

---

### `word_bank`

**核心词库表**。同一单词在不同等级是不同记录。

```sql
CREATE TABLE `word_bank` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `level_code` VARCHAR(20) NOT NULL,
  `word` VARCHAR(100) NOT NULL,
  `word_lower` VARCHAR(100) NOT NULL COMMENT '全小写，用于查询',
  `ipa_uk` VARCHAR(100) DEFAULT NULL,
  `ipa_us` VARCHAR(100) DEFAULT NULL,
  `en_definition` TEXT NOT NULL,
  `zh_definition` TEXT NOT NULL COMMENT '含词性标注',
  `example_en` TEXT DEFAULT NULL,
  `example_zh` TEXT DEFAULT NULL,
  `topic_code` VARCHAR(30) DEFAULT NULL,
  `audio_url_uk` VARCHAR(500) DEFAULT NULL,
  `audio_url_us` VARCHAR(500) DEFAULT NULL,
  `image_url` VARCHAR(500) DEFAULT NULL,
  `emoji` VARCHAR(20) DEFAULT NULL COMMENT '作为图片 fallback',
  `difficulty` TINYINT DEFAULT NULL COMMENT '1-10',
  `frequency` DECIMAL(4,3) DEFAULT NULL COMMENT '0.000-1.000 词频',
  `pos` VARCHAR(50) DEFAULT NULL COMMENT '词性 n./v./adj./adv.',
  `related_words` JSON DEFAULT NULL COMMENT '同根词/同义词/反义词',
  `audit_status` TINYINT NOT NULL DEFAULT 1 COMMENT '1审核通过 0待审 -1下架',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at` DATETIME(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level_word` (`level_code`, `word_lower`, `deleted_at`),
  KEY `idx_level_topic` (`level_code`, `topic_code`),
  KEY `idx_word_lower` (`word_lower`),
  KEY `idx_level_word_lower` (`level_code`, `word_lower`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### `word_topic`

```sql
CREATE TABLE `word_topic` (
  `code` VARCHAR(30) NOT NULL,
  `name_zh` VARCHAR(50) NOT NULL,
  `name_en` VARCHAR(50) NOT NULL,
  `icon` VARCHAR(20) DEFAULT NULL COMMENT 'emoji',
  `sort_order` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

预置主题：食物、动物、人物、情感、动作、形容、学术、商业、自然、颜色、时间、数量、思维、社会、健康、成就、抽象、语言、生活、工作。

---

### `user_word_progress`（关键表）

**最核心的表**。记录每个用户每个单词的学习进度。

```sql
CREATE TABLE `user_word_progress` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `word_id` BIGINT UNSIGNED NOT NULL,
  `level_code` VARCHAR(20) NOT NULL COMMENT '冗余字段，避免 join',
  `stage` TINYINT NOT NULL DEFAULT 0 COMMENT '0未学 1-9艾宾浩斯阶段 9已掌握',
  `correct_count` INT NOT NULL DEFAULT 0,
  `wrong_count` INT NOT NULL DEFAULT 0,
  `last_reviewed_at` DATETIME(3) DEFAULT NULL,
  `next_review_at` DATETIME(3) DEFAULT NULL COMMENT '预计下次复习时间',
  `first_learned_at` DATETIME(3) DEFAULT NULL,
  `mastered_at` DATETIME(3) DEFAULT NULL COMMENT 'stage 首次达到 9 的时间',
  `client_updated_at` DATETIME(3) DEFAULT NULL COMMENT '客户端时间戳，用于同步冲突解决',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_word` (`user_id`, `word_id`),
  KEY `idx_user_next_review` (`user_id`, `next_review_at`) COMMENT '查今日复习',
  KEY `idx_user_level_stage` (`user_id`, `level_code`, `stage`),
  KEY `idx_next_review` (`next_review_at`) COMMENT '全局到期扫描'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**关键索引设计**：
- `uk_user_word` 保证幂等（同一用户同一词只有一条进度）
- `idx_user_next_review` 是今日复习查询的核心索引，`WHERE user_id = ? AND next_review_at <= NOW() ORDER BY next_review_at` 走这个索引
- `next_review_at = NULL` 表示从未学过

---

### `study_log`

学习行为流水表，数据量大，**按月分区**。

```sql
CREATE TABLE `study_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `word_id` BIGINT UNSIGNED NOT NULL,
  `level_code` VARCHAR(20) NOT NULL,
  `action` VARCHAR(20) NOT NULL COMMENT 'learn/review/test',
  `result` VARCHAR(20) NOT NULL COMMENT 'correct/wrong/skip',
  `mode` VARCHAR(20) DEFAULT NULL COMMENT 'card/spelling/choice/listening',
  `stage_before` TINYINT DEFAULT NULL,
  `stage_after` TINYINT DEFAULT NULL,
  `duration_ms` INT DEFAULT NULL COMMENT '用户思考+答题耗时',
  `client_ts` DATETIME(3) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`, `created_at`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  KEY `idx_word_created` (`word_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
PARTITION BY RANGE (UNIX_TIMESTAMP(created_at)) (
  PARTITION p202601 VALUES LESS THAN (UNIX_TIMESTAMP('2026-02-01')),
  PARTITION p202602 VALUES LESS THAN (UNIX_TIMESTAMP('2026-03-01')),
  -- ... 按月追加，通过定时任务维护
  PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

---

### `checkin`

```sql
CREATE TABLE `checkin` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `checkin_date` DATE NOT NULL COMMENT '用户本地日期',
  `words_learned` INT NOT NULL DEFAULT 0,
  `words_reviewed` INT NOT NULL DEFAULT 0,
  `correct_count` INT NOT NULL DEFAULT 0,
  `duration_seconds` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### `user_streak`

```sql
CREATE TABLE `user_streak` (
  `user_id` BIGINT UNSIGNED NOT NULL,
  `current_streak` INT NOT NULL DEFAULT 0 COMMENT '当前连续天数',
  `longest_streak` INT NOT NULL DEFAULT 0,
  `last_checkin_date` DATE DEFAULT NULL,
  `total_days` INT NOT NULL DEFAULT 0 COMMENT '累计学习天数',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### `achievement`

```sql
CREATE TABLE `achievement` (
  `code` VARCHAR(50) NOT NULL,
  `name_zh` VARCHAR(100) NOT NULL,
  `description_zh` VARCHAR(500) NOT NULL,
  `icon` VARCHAR(500) DEFAULT NULL,
  `category` VARCHAR(20) NOT NULL COMMENT 'streak/volume/accuracy/level',
  `trigger_rule` JSON NOT NULL COMMENT '触发规则，如 {"type":"streak","days":7}',
  `sort_order` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### `user_achievement`

```sql
CREATE TABLE `user_achievement` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `achievement_code` VARCHAR(50) NOT NULL,
  `achieved_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_ach` (`user_id`, `achievement_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### `wrong_word`

错题本。单词答错后自动进入，专项复习时读取。

```sql
CREATE TABLE `wrong_word` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `word_id` BIGINT UNSIGNED NOT NULL,
  `level_code` VARCHAR(20) NOT NULL,
  `wrong_count` INT NOT NULL DEFAULT 1,
  `last_wrong_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `resolved` TINYINT NOT NULL DEFAULT 0 COMMENT '连续答对 N 次后置 1',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_word` (`user_id`, `word_id`),
  KEY `idx_user_resolved` (`user_id`, `resolved`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### `sync_token`

多端同步用。每个设备一个 token，记录该设备最后拉取的时间点。

```sql
CREATE TABLE `sync_token` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `device_id` VARCHAR(100) NOT NULL,
  `device_type` VARCHAR(20) NOT NULL COMMENT 'web/miniprogram/android',
  `last_sync_at` DATETIME(3) DEFAULT NULL,
  `last_push_at` DATETIME(3) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_device` (`user_id`, `device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 容量估算

假设 DAU 1 万，每个用户平均学 3 个等级 ≈ 1.5 万个单词：

- `user`: 10 万行 × ~500B = 50MB
- `word_bank`: 5 万行 × ~2KB = 100MB
- `user_word_progress`: 10 万 × 1.5 万 = 15 亿行 ❌

**这里是瓶颈**。10 万用户 × 平均 5000 个已学词 = 5 亿行。需要：

1. **只在用户真正学过一个词后才插入记录**（`stage > 0`），未学过的词不写 progress 表
2. 按 `user_id` 做哈希分表（预留，初期不做）
3. 冷数据归档（6 个月未更新的进度移到 `user_word_progress_archive`）

`study_log` 表按日志处理，按月分区 + 6 个月滚动清理，稳定在 ~20GB 以内。

## 索引优化清单

| 查询场景 | 涉及表 | 用到的索引 |
|---------|--------|-----------|
| 查今日复习 | user_word_progress | idx_user_next_review |
| 查某级别已学词 | user_word_progress | idx_user_level_stage |
| 按字母/主题排序取词 | word_bank | idx_level_topic |
| 词形模糊查询 | word_bank | idx_word_lower |
| 连续打卡计算 | checkin | uk_user_date |
| 用户日报 | study_log | idx_user_created |

## 迁移策略

使用 Flyway（Java 侧）/ Alembic（Python 侧）管理 schema 迁移。初始化脚本 `V1__init.sql` 对应 `sql/init.sql`。后续每个变更一个 `V{n}__desc.sql`。
