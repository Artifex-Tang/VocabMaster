-- Import seed data from words_sample.csv into word_bank
-- Usage: docker exec -i vocab-mysql mysql -uvocab -pvocab123 vocabmaster < sql/import_seed.sql
-- Or run after init.sql

LOAD DATA LOCAL INFILE '/tmp/words_sample.csv'
INTO TABLE word_bank
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(level_code, word, @ipa_uk, @ipa_us, en_definition, zh_definition, example_en, example_zh, @topic_code, @audio_uk, @audio_us, @image_url, emoji, difficulty, frequency)
SET
  word_lower     = LOWER(word),
  ipa_uk         = NULLIF(@ipa_uk, ''),
  ipa_us         = NULLIF(@ipa_us, ''),
  topic_code     = NULLIF(@topic_code, ''),
  audio_url_uk   = NULLIF(@audio_uk, ''),
  audio_url_us   = NULLIF(@audio_us, ''),
  image_url      = NULLIF(@image_url, '');
