-- =========================================================================
-- VocabMaster 数据库初始化脚本
-- 目标：MySQL 8.0+
-- 字符集：utf8mb4 / utf8mb4_0900_ai_ci
-- 时区：UTC（所有 DATETIME 字段存 UTC）
-- =========================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `vocabmaster`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `vocabmaster`;

-- =========================================================================
-- 用户相关
-- =========================================================================

DROP TABLE IF EXISTS `user`;
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
  `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER / ADMIN',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户';

DROP TABLE IF EXISTS `user_auth`;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='第三方登录绑定';

DROP TABLE IF EXISTS `user_settings`;
CREATE TABLE `user_settings` (
  `user_id` BIGINT UNSIGNED NOT NULL,
  `daily_new_words_goal` INT NOT NULL DEFAULT 20,
  `daily_review_goal` INT NOT NULL DEFAULT 100,
  `default_sort_mode` VARCHAR(10) NOT NULL DEFAULT 'alpha' COMMENT 'alpha/topic/random',
  `preferred_accent` VARCHAR(4) NOT NULL DEFAULT 'uk' COMMENT 'uk/us',
  `auto_play_audio` TINYINT NOT NULL DEFAULT 1,
  `notification_time` TIME DEFAULT '20:00:00',
  `theme` VARCHAR(10) NOT NULL DEFAULT 'light',
  `active_levels` JSON DEFAULT NULL COMMENT '用户当前在学的等级数组',
  `extra` JSON DEFAULT NULL,
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户设置';

-- =========================================================================
-- 词库相关
-- =========================================================================

DROP TABLE IF EXISTS `level`;
CREATE TABLE `level` (
  `code` VARCHAR(20) NOT NULL,
  `name_zh` VARCHAR(50) NOT NULL,
  `name_en` VARCHAR(50) NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `target_word_count` INT NOT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='等级字典';

DROP TABLE IF EXISTS `word_topic`;
CREATE TABLE `word_topic` (
  `code` VARCHAR(30) NOT NULL,
  `name_zh` VARCHAR(50) NOT NULL,
  `name_en` VARCHAR(50) NOT NULL,
  `icon` VARCHAR(20) DEFAULT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='主题字典';

DROP TABLE IF EXISTS `word_bank`;
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
  `emoji` VARCHAR(20) DEFAULT NULL,
  `difficulty` TINYINT DEFAULT NULL COMMENT '1-10',
  `frequency` DECIMAL(4,3) DEFAULT NULL COMMENT '0.000-1.000',
  `pos` VARCHAR(50) DEFAULT NULL,
  `related_words` JSON DEFAULT NULL,
  `audit_status` TINYINT NOT NULL DEFAULT 1 COMMENT '1通过 0待审 -1下架',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at` DATETIME(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level_word` (`level_code`, `word_lower`, `deleted_at`),
  KEY `idx_level_topic` (`level_code`, `topic_code`),
  KEY `idx_word_lower` (`word_lower`),
  KEY `idx_level_word_lower` (`level_code`, `word_lower`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='词库';

-- =========================================================================
-- 学习相关
-- =========================================================================

DROP TABLE IF EXISTS `user_word_progress`;
CREATE TABLE `user_word_progress` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `word_id` BIGINT UNSIGNED NOT NULL,
  `level_code` VARCHAR(20) NOT NULL,
  `stage` TINYINT NOT NULL DEFAULT 0 COMMENT '0未学 1-9艾宾浩斯阶段',
  `correct_count` INT NOT NULL DEFAULT 0,
  `wrong_count` INT NOT NULL DEFAULT 0,
  `last_reviewed_at` DATETIME(3) DEFAULT NULL,
  `next_review_at` DATETIME(3) DEFAULT NULL,
  `first_learned_at` DATETIME(3) DEFAULT NULL,
  `mastered_at` DATETIME(3) DEFAULT NULL,
  `client_updated_at` DATETIME(3) DEFAULT NULL COMMENT '客户端时间戳，用于同步',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_word` (`user_id`, `word_id`),
  KEY `idx_user_next_review` (`user_id`, `next_review_at`),
  KEY `idx_user_level_stage` (`user_id`, `level_code`, `stage`),
  KEY `idx_next_review` (`next_review_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户学习进度（核心）';

DROP TABLE IF EXISTS `study_log`;
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
  `duration_ms` INT DEFAULT NULL,
  `client_ts` DATETIME(3) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`, `created_at`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  KEY `idx_word_created` (`word_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习行为日志（建议分区）';
-- 生产环境建议按月分区，示例：
-- PARTITION BY RANGE (UNIX_TIMESTAMP(created_at)) (
--   PARTITION p202604 VALUES LESS THAN (UNIX_TIMESTAMP('2026-05-01')),
--   PARTITION p202605 VALUES LESS THAN (UNIX_TIMESTAMP('2026-06-01')),
--   PARTITION p_future VALUES LESS THAN MAXVALUE
-- );

DROP TABLE IF EXISTS `wrong_word`;
CREATE TABLE `wrong_word` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `word_id` BIGINT UNSIGNED NOT NULL,
  `level_code` VARCHAR(20) NOT NULL,
  `wrong_count` INT NOT NULL DEFAULT 1,
  `last_wrong_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `resolved` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_word` (`user_id`, `word_id`),
  KEY `idx_user_resolved` (`user_id`, `resolved`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='错题本';

-- =========================================================================
-- 打卡 + 成就
-- =========================================================================

DROP TABLE IF EXISTS `checkin`;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日打卡';

DROP TABLE IF EXISTS `user_streak`;
CREATE TABLE `user_streak` (
  `user_id` BIGINT UNSIGNED NOT NULL,
  `current_streak` INT NOT NULL DEFAULT 0,
  `longest_streak` INT NOT NULL DEFAULT 0,
  `last_checkin_date` DATE DEFAULT NULL,
  `total_days` INT NOT NULL DEFAULT 0,
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='连续打卡统计';

DROP TABLE IF EXISTS `achievement`;
CREATE TABLE `achievement` (
  `code` VARCHAR(50) NOT NULL,
  `name_zh` VARCHAR(100) NOT NULL,
  `description_zh` VARCHAR(500) NOT NULL,
  `icon` VARCHAR(500) DEFAULT NULL,
  `category` VARCHAR(20) NOT NULL COMMENT 'streak/volume/accuracy/level',
  `trigger_rule` JSON NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成就字典';

DROP TABLE IF EXISTS `user_achievement`;
CREATE TABLE `user_achievement` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `achievement_code` VARCHAR(50) NOT NULL,
  `achieved_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_ach` (`user_id`, `achievement_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户已获成就';

-- =========================================================================
-- 同步 + 管理
-- =========================================================================

DROP TABLE IF EXISTS `sync_token`;
CREATE TABLE `sync_token` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `device_id` VARCHAR(100) NOT NULL,
  `device_type` VARCHAR(20) NOT NULL,
  `last_sync_at` DATETIME(3) DEFAULT NULL,
  `last_push_at` DATETIME(3) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_device` (`user_id`, `device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多端同步令牌';

DROP TABLE IF EXISTS `admin_log`;
CREATE TABLE `admin_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `admin_user_id` BIGINT UNSIGNED NOT NULL,
  `action` VARCHAR(50) NOT NULL,
  `target_type` VARCHAR(50) DEFAULT NULL,
  `target_id` VARCHAR(100) DEFAULT NULL,
  `detail` JSON DEFAULT NULL,
  `ip` VARCHAR(50) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_admin_created` (`admin_user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理操作日志';

-- =========================================================================
-- 等级字典数据
-- =========================================================================

INSERT INTO `level` (`code`, `name_zh`, `name_en`, `sort_order`, `target_word_count`, `description`) VALUES
  ('PRIMARY', '小学', 'Primary School', 1, 800, '小学 1-6 年级课标词汇'),
  ('KET', 'KET剑桥入门', 'KET (A2)', 2, 1500, '剑桥通用英语 A2 级'),
  ('JUNIOR', '初中', 'Junior High', 3, 2000, '初中课标词汇'),
  ('PET', 'PET剑桥初级', 'PET (B1)', 4, 3500, '剑桥通用英语 B1 级'),
  ('SENIOR', '高中', 'Senior High', 5, 3500, '高中课标词汇'),
  ('CET4', '大学四级', 'CET-4', 6, 4500, '大学英语四级'),
  ('FCE', 'FCE剑桥中级', 'FCE (B2)', 7, 5000, '剑桥通用英语 B2 级'),
  ('CET6', '大学六级', 'CET-6', 8, 6000, '大学英语六级'),
  ('CAE', 'CAE剑桥高级', 'CAE (C1)', 9, 7500, '剑桥通用英语 C1 级'),
  ('TEM8', '专业八级', 'TEM-8', 10, 13000, '英语专业八级');

-- =========================================================================
-- 主题字典数据
-- =========================================================================

INSERT INTO `word_topic` (`code`, `name_zh`, `name_en`, `icon`, `sort_order`) VALUES
  ('FOOD', '食物饮食', 'Food', '🍔', 1),
  ('ANIMAL', '动物', 'Animal', '🐾', 2),
  ('PERSON', '人物', 'Person', '👤', 3),
  ('EMOTION', '情感情绪', 'Emotion', '😊', 4),
  ('ACTION', '动作', 'Action', '🏃', 5),
  ('DESCRIBE', '形容词汇', 'Descriptive', '🎨', 6),
  ('ACADEMIC', '学术', 'Academic', '🎓', 7),
  ('BUSINESS', '商业', 'Business', '💼', 8),
  ('NATURE', '自然环境', 'Nature', '🌳', 9),
  ('COLOR', '颜色', 'Color', '🌈', 10),
  ('TIME', '时间', 'Time', '⏰', 11),
  ('QUANTITY', '数量', 'Quantity', '🔢', 12),
  ('THINKING', '思维', 'Thinking', '🧠', 13),
  ('SOCIETY', '社会', 'Society', '🏛️', 14),
  ('HEALTH', '健康医疗', 'Health', '🏥', 15),
  ('ACHIEVEMENT', '成就', 'Achievement', '🏆', 16),
  ('ABSTRACT', '抽象概念', 'Abstract', '💭', 17),
  ('LANGUAGE', '语言', 'Language', '💬', 18),
  ('LIFE', '生活', 'Life', '🏠', 19),
  ('WORK', '工作', 'Work', '⚒️', 20),
  ('OTHER', '其他', 'Other', '📦', 99);

-- =========================================================================
-- 成就字典数据
-- =========================================================================

INSERT INTO `achievement` (`code`, `name_zh`, `description_zh`, `icon`, `category`, `trigger_rule`, `sort_order`) VALUES
  ('FIRST_WORD', '初露锋芒', '学习第一个单词', '🌱', 'volume', '{"type":"words_learned","count":1}', 1),
  ('WORDS_100', '百词斩', '累计学习 100 个单词', '💯', 'volume', '{"type":"words_learned","count":100}', 2),
  ('WORDS_500', '五百词王', '累计学习 500 个单词', '🎯', 'volume', '{"type":"words_learned","count":500}', 3),
  ('WORDS_1000', '千词达人', '累计学习 1000 个单词', '🏹', 'volume', '{"type":"words_learned","count":1000}', 4),
  ('WORDS_5000', '词海泛舟', '累计学习 5000 个单词', '🚣', 'volume', '{"type":"words_learned","count":5000}', 5),
  ('STREAK_3', '初心不改', '连续打卡 3 天', '🔥', 'streak', '{"type":"streak","days":3}', 10),
  ('STREAK_7', '坚持一周', '连续打卡 7 天', '🔥', 'streak', '{"type":"streak","days":7}', 11),
  ('STREAK_30', '月更达人', '连续打卡 30 天', '🌟', 'streak', '{"type":"streak","days":30}', 12),
  ('STREAK_100', '百日筑基', '连续打卡 100 天', '💎', 'streak', '{"type":"streak","days":100}', 13),
  ('STREAK_365', '年度学霸', '连续打卡 365 天', '👑', 'streak', '{"type":"streak","days":365}', 14),
  ('ACCURACY_90', '神枪手', '单次学习正确率达 90%', '🎯', 'accuracy', '{"type":"session_accuracy","min":0.9,"min_count":20}', 20),
  ('ACCURACY_100', '百发百中', '单次学习 20 词全对', '🏆', 'accuracy', '{"type":"session_accuracy","min":1.0,"min_count":20}', 21),
  ('LEVEL_PRIMARY', '小学毕业', '完成小学等级', '🎓', 'level', '{"type":"level_mastered","level":"PRIMARY"}', 30),
  ('LEVEL_JUNIOR', '初中毕业', '完成初中等级', '🎓', 'level', '{"type":"level_mastered","level":"JUNIOR"}', 31),
  ('LEVEL_SENIOR', '高中毕业', '完成高中等级', '🎓', 'level', '{"type":"level_mastered","level":"SENIOR"}', 32),
  ('LEVEL_CET4', '四级达标', '完成大学四级', '🎓', 'level', '{"type":"level_mastered","level":"CET4"}', 33),
  ('LEVEL_CET6', '六级达标', '完成大学六级', '🎓', 'level', '{"type":"level_mastered","level":"CET6"}', 34),
  ('LEVEL_KET', '剑桥 A2', '完成 KET 等级', '🎓', 'level', '{"type":"level_mastered","level":"KET"}', 35),
  ('LEVEL_PET', '剑桥 B1', '完成 PET 等级', '🎓', 'level', '{"type":"level_mastered","level":"PET"}', 36),
  ('LEVEL_FCE', '剑桥 B2', '完成 FCE 等级', '🎓', 'level', '{"type":"level_mastered","level":"FCE"}', 37);

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================================
-- 结束
-- =========================================================================
