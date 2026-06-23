-- 给 word_topic 主题字典加 image_type 列（幂等：列已存在则 no-op）
-- photo = 从 Pixabay 下载实景图
-- icon  = 图标/插图（暂用 emoji 兜底）
-- none  = 纯功能词，无图
--
-- 背景：V2 曾被 Python 手动 apply 到 DB，但未进 flyway_schema_history
-- （flyway 仅 baseline@V1）。直接 ALTER 会在重建镜像时 Duplicate column 报错。
-- 用 information_schema 守卫：列不存在才 ADD，已存在跳过。UPDATE 天然幂等。

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'word_topic'
    AND column_name = 'image_type'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `word_topic` ADD COLUMN `image_type` ENUM(''photo'',''icon'',''none'') NOT NULL DEFAULT ''none'' AFTER `sort_order`',
  'DO 0'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `word_topic` SET `image_type` = 'photo' WHERE `code` IN (
  'animals','food_drink','clothing','body','home','weather',
  'travel_transport','nature','geography','sports','arts',
  'entertainment','health','technology','science','education',
  'family','daily_life'
);

UPDATE `word_topic` SET `image_type` = 'icon' WHERE `code` IN (
  'emotion','personality','action','description','abstract',
  'communication','society','law_politics','business','work'
);

-- language / time / math 保持默认 none
