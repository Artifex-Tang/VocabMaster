-- V3__custom_word_list.sql
-- 自定义词库：词库 / 词库项（课程分组）/ 用户订阅 + 单元游标
-- Spec: docs/superpowers/specs/2026-06-23-custom-word-list-design.md

CREATE TABLE word_list (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  owner_user_id     BIGINT NULL COMMENT 'NULL=系统内置共享；非空=用户个人上传',
  name              VARCHAR(128) NOT NULL,
  description       VARCHAR(512),
  source_type       VARCHAR(16) NOT NULL DEFAULT 'builtin' COMMENT 'builtin|imported',
  origin_level_code VARCHAR(32) NULL COMMENT '内置教材词库挂 level_code（如 THINK_L2）',
  word_count        INT NOT NULL DEFAULT 0,
  cover_emoji       VARCHAR(16),
  sort_order        INT NOT NULL DEFAULT 0,
  created_at        DATETIME(3),
  updated_at        DATETIME(3),
  deleted_at        DATETIME(3) NULL,
  INDEX idx_wl_owner (owner_user_id),
  INDEX idx_wl_source (source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE word_list_item (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  list_id    BIGINT NOT NULL,
  word_id    BIGINT NOT NULL COMMENT '-> word_bank.id',
  unit_no    INT NOT NULL,
  page       INT,
  sort_order INT NOT NULL DEFAULT 0,
  INDEX idx_wli_list_unit (list_id, unit_no, sort_order),
  INDEX idx_wli_word (word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_list_subscription (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id         BIGINT NOT NULL,
  list_id         BIGINT NOT NULL,
  current_unit_no INT NOT NULL DEFAULT 1,
  subscribed_at   DATETIME(3),
  updated_at      DATETIME(3),
  UNIQUE KEY uk_uls_user_list (user_id, list_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
