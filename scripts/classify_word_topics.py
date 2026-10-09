"""
VocabMaster — 对唯一单词分类 image_type，写入 word_topic 表
用法:
  python scripts/classify_word_topics.py          # 分类全部唯一词
  python scripts/classify_word_topics.py --dry-run # 只打印，不写 DB
  python scripts/classify_word_topics.py --stats   # 只显示统计

分类规则（优先级从高到低）:
  1. pos = 副词/介词/数词/感叹词  → none（功能词，无图意义）
  2. pos = 名词 + 具象 topic      → photo（Pixabay 照片/插图）
  3. pos = 名词 + 抽象 topic      → icon（Iconify 图标）
  4. pos = 动词/形容词            → icon（动作/状态图标）
  5. 其余                         → none

相同 word_lower 跨等级共用同一条 word_topic 记录。
"""
import argparse
import os
import sys

import pymysql

DB_CONFIG = dict(
    host=os.getenv("DB_HOST", "127.0.0.1"),
    port=int(os.getenv("DB_PORT", 3306)),
    user=os.getenv("DB_USER", "root"),
    password=os.getenv("DB_PASS", "root"),
    database=os.getenv("DB_NAME", "vocabmaster"),
    charset="utf8mb4",
)

# 具象 topic：有明确视觉形象
CONCRETE_TOPICS = {
    "animals",          # 动物
    "food_drink",       # 食物饮料
    "clothing",         # 服饰
    "body",             # 身体部位
    "home",             # 家居
    "weather",          # 天气
    "technology",       # 科技设备
    "travel_transport", # 交通出行
    "nature",           # 自然植物
    "geography",        # 地理地貌
    "sports",           # 体育
    "arts",             # 艺术
    "entertainment",    # 娱乐
    "science",          # 科学仪器
    "health",           # 健康医疗
    "education",        # 教育
    "family",           # 家庭人物
    "daily_life",       # 日常生活
}

# 抽象 topic：概念/情感/动作
ABSTRACT_TOPICS = {
    "action",           # 动作
    "description",      # 描述形容
    "abstract",         # 抽象概念
    "emotion",          # 情感
    "communication",    # 沟通
    "language",         # 语言
    "personality",      # 个性
    "society",          # 社会
    "time",             # 时间
    "law_politics",     # 法律政治
    "business",         # 商业
    "work",             # 工作
    "math",             # 数学
}

# POS 无图功能词前缀（ECDICT 格式: n=名词 v=动词 j=形容词 r=副词 p=介词 m=数词 i=习语 u=助词 c=连词 e=叹词）
NO_IMAGE_POS = {"r", "p", "m", "u", "c", "e"}


def get_pos_prefix(pos: str) -> str:
    if not pos or pos == "nan":
        return "unknown"
    return pos.split(":")[0].strip().lower()


def classify(word_lower: str, pos: str, topic_code: str) -> str:
    p = get_pos_prefix(pos)

    # 副词/介词/数词/连词/助词/叹词 → none
    if p in NO_IMAGE_POS:
        return "none"

    # 名词
    if p == "n":
        if topic_code in CONCRETE_TOPICS:
            return "photo"
        if topic_code in ABSTRACT_TOPICS:
            return "icon"
        return "photo"  # 未知 topic 的名词默认当 photo 试试

    # 动词/形容词 → icon（动作/状态图标）
    if p in ("v", "j"):
        return "icon"

    # 习语/短语 → none（太复杂）
    if p == "i":
        return "none"

    # 其余未知 → none
    return "none"


def main():
    parser = argparse.ArgumentParser(description="Classify unique words into image_type")
    parser.add_argument("--dry-run", action="store_true", help="Print only, no DB write")
    parser.add_argument("--stats", action="store_true", help="Show stats only")
    args = parser.parse_args()

    conn = pymysql.connect(**DB_CONFIG)
    cur = conn.cursor()

    # 每个 word_lower 取一条代表行（MIN(id) 保证唯一）
    cur.execute("""
        SELECT word_lower, pos, topic_code
        FROM word_bank
        WHERE deleted_at IS NULL
          AND id IN (
              SELECT MIN(id) FROM word_bank
              WHERE deleted_at IS NULL
              GROUP BY word_lower
          )
        ORDER BY word_lower
    """)
    rows = cur.fetchall()
    print(f"Unique words: {len(rows)}")

    if args.stats:
        counts = {"photo": 0, "icon": 0, "none": 0}
        for word_lower, pos, topic_code in rows:
            t = classify(word_lower, pos, topic_code)
            counts[t] += 1
        for k, v in counts.items():
            print(f"  {k:10s} {v:>6}")
        conn.close()
        return

    photo, icon, none_count = 0, 0, 0
    batch = []
    for word_lower, pos, topic_code in rows:
        t = classify(word_lower, pos, topic_code)
        if t == "photo":
            photo += 1
        elif t == "icon":
            icon += 1
        else:
            none_count += 1
        batch.append((word_lower, t))

    if args.dry_run:
        print(f"photo={photo}  icon={icon}  none={none_count}")
        print("\nSample photo:", [w for w, t in batch if t == "photo"][:20])
        print("Sample icon: ", [w for w, t in batch if t == "icon"][:20])
        print("Sample none: ", [w for w, t in batch if t == "none"][:20])
        conn.close()
        return

    # 1. 批量 upsert word_topic（去重层）
    cur.executemany(
        """
        INSERT INTO word_topic (word_lower, image_type)
        VALUES (%s, %s)
        ON DUPLICATE KEY UPDATE image_type = VALUES(image_type),
                                classified_at = CURRENT_TIMESTAMP(3)
        """,
        batch,
    )
    conn.commit()
    print(f"word_topic upserted {len(batch)} rows -> photo={photo}  icon={icon}  none={none_count}")

    # 2. 同步写回 word_bank.topic_code（相同 word_lower 跨等级共用同一分类）
    cur.execute("""
        UPDATE word_bank wb
        JOIN word_topic wt ON wb.word_lower = wt.word_lower
        SET wb.topic_code = wt.image_type
        WHERE wb.deleted_at IS NULL
    """)
    conn.commit()
    print(f"word_bank.topic_code updated: {cur.rowcount} rows")

    # 3. none 类型清空 image_url
    cur.execute("""
        UPDATE word_bank SET image_url = NULL
        WHERE topic_code = 'none' AND deleted_at IS NULL
    """)
    conn.commit()
    print(f"Reset image_url=NULL for 'none': {cur.rowcount} rows")

    cur.close()
    conn.close()


if __name__ == "__main__":
    main()
