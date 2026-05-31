"""
LLM 全量主题分类脚本 — DeepSeek V3
31 类主题体系（参考 Cambridge/Oxford/IELTS 词汇分类）
用法：
  export DEEPSEEK_API_KEY=your_key_here
  python classify_topics.py
"""

import os
import json
import time
import pymysql
from openai import OpenAI

# ── 配置 ──────────────────────────────────────────────────
API_KEY = os.environ.get("DEEPSEEK_API_KEY", "")
if not API_KEY:
    print("ERROR: set DEEPSEEK_API_KEY env var first")
    exit(1)

BASE_URL = "https://api.deepseek.com"
MODEL = "deepseek-chat"
BATCH_SIZE = 200
SLEEP_SEC = 2

# ── 31 个主题 ─────────────────────────────────────────────
TOPICS = [
    "family", "personality", "emotion", "body", "health",
    "food_drink", "clothing", "home", "daily_life", "education",
    "work", "business", "law_politics", "nature", "animals",
    "weather", "geography", "travel_transport", "sports", "arts",
    "entertainment", "technology", "science", "math", "communication",
    "language", "time", "action", "description", "society",
    "abstract",
]

TOPIC_LABELS = {
    "family":          "家庭人际 — 家庭成员、亲戚、婚姻、友谊、社交关系",
    "personality":     "性格品质 — 性格特征、品德、态度、个人品质(如honest, brave, lazy)",
    "emotion":         "情感心理 — 情绪、心理状态、感受(如happy, angry, anxious)",
    "body":            "身体部位 — 身体器官、生理、外貌描述",
    "health":          "健康医疗 — 疾病、治疗、药物、医疗、养生",
    "food_drink":      "食物饮品 — 食材、烹饪、餐饮、饮料",
    "clothing":        "服饰外表 — 衣服、配饰、时尚、穿着",
    "home":            "居家生活 — 住房、家具、家务、家居用品",
    "daily_life":      "日常生活 — 日常活动、习惯、购物、生活琐事",
    "education":       "教育学习 — 学校、课程、考试、学习、知识",
    "work":            "工作职业 — 职业、工作场所、职场、行业",
    "business":        "商业金融 — 经济、贸易、金融、投资、金钱",
    "law_politics":    "法律政治 — 法律、司法、政府、政治、选举",
    "nature":          "自然环境 — 自然景观、生态、植物、环保",
    "animals":         "动物 — 动物、鸟类、昆虫、宠物",
    "weather":         "天气季节 — 天气、气候、季节、自然灾害",
    "geography":       "地理区域 — 国家、城市、地形、方位、地图",
    "travel_transport": "旅行交通 — 交通工具、旅游、酒店、出行",
    "sports":          "体育运动 — 运动项目、比赛、健身",
    "arts":            "艺术文化 — 绘画、音乐、文学、设计、传统文化",
    "entertainment":   "娱乐媒体 — 电影、电视、游戏、社交媒体、娱乐活动",
    "technology":      "科技数码 — 计算机、互联网、软件、电子产品",
    "science":         "科学研究 — 物理、化学、生物、实验、研究方法",
    "math":            "数学度量 — 数学概念、数量、测量、统计",
    "communication":   "交流沟通 — 说话、写作、对话、演讲、媒体传播",
    "language":        "语言语法 — 语法术语、功能词(连词/介词/代词)、语言学概念",
    "time":            "时间频率 — 时间、日期、频率、时长、先后顺序",
    "action":          "动作行为 — 动词：移动、操作、创造、改变等动作",
    "description":     "描述形容 — 形容词：大小、颜色、形状、质量、程度",
    "society":         "社会文化 — 社会问题、宗教、历史、文化习俗、人口",
    "abstract":        "抽象概念 — 无法归入以上任何类别的纯抽象概念",
}

# ── DB ────────────────────────────────────────────────────
DB = dict(host="127.0.0.1", port=3306, user="vocab", password="vocab123", database="vocabmaster", charset="utf8mb4")

# ── LLM ──────────────────────────────────────────────────
client = OpenAI(api_key=API_KEY, base_url=BASE_URL)

TOPIC_JSON = json.dumps({k: v.split("—")[0].strip() for k, v in TOPIC_LABELS.items()}, ensure_ascii=False, indent=2)

SYSTEM_PROMPT = f"""你是英语词汇分类专家。根据单词的主要含义和最常见用法，将每个单词归入以下 31 个主题之一。

{json.dumps(TOPIC_LABELS, ensure_ascii=False, indent=2)}

分类原则：
1. 虚词(for, that, also, because等)归入 language
2. 动词优先按语义领域分类(如 cook→food_drink, run→sports)，纯动作动词归 action
3. 形容词优先按语义领域分类(如 delicious→food_drink, fast→travel_transport)，纯描述词归 description
4. 时间词(yesterday, often, early等)归 time
5. abstract 只用于无法归入其他任何类别的词

返回纯 JSON，格式：{{"word": "topic_code"}}，不要其他内容。"""

def classify_batch(words: list[str], batch_idx: int) -> tuple[int, dict[str, str]]:
    """调 API 分类一批单词"""
    word_list = "\n".join(words)
    user_msg = f"请分类以下 {len(words)} 个单词：\n{word_list}"

    for attempt in range(5):
        try:
            resp = client.chat.completions.create(
                model=MODEL,
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": user_msg},
                ],
                temperature=0.1,
                max_tokens=4096,
            )
            text = resp.choices[0].message.content
            if not text or not text.strip():
                print(f"  Batch {batch_idx+1} empty response, attempt {attempt+1}", flush=True)
                time.sleep(2 ** attempt)
                continue
            text = text.strip()
            if "```" in text:
                text = text.split("```")[1]
                if text.startswith("json"):
                    text = text[4:]
                text = text.split("```")[0]
            result = json.loads(text.strip())
            valid = {}
            for w, t in result.items():
                w_lower = w.strip().lower()
                if t in TOPICS:
                    valid[w_lower] = t
            return (batch_idx, valid)
        except Exception as e:
            print(f"  Batch {batch_idx+1} error (attempt {attempt+1}): {e}", flush=True)
            time.sleep(2 ** attempt)
    return (batch_idx, {})

def main():
    conn = pymysql.connect(**DB)
    cur = conn.cursor()

    cur.execute("SELECT id, word, level_code FROM word_bank WHERE deleted_at IS NULL ORDER BY level_code, id")
    rows = cur.fetchall()
    print(f"Total words: {len(rows)}")

    total_batches = (len(rows) + BATCH_SIZE - 1) // BATCH_SIZE
    batches = []
    for i in range(total_batches):
        start = i * BATCH_SIZE
        end = min(start + BATCH_SIZE, len(rows))
        batches.append(rows[start:end])

    print(f"Batches: {total_batches}, Model: {MODEL}")

    # 串行调 API
    results = {}
    for i, batch in enumerate(batches):
        words = [r[1] for r in batch]
        level = batch[0][2]
        print(f"Batch {i+1}/{total_batches} [{level}] {len(words)} words...", end=" ", flush=True)
        _, classified = classify_batch(words, i)
        results[i] = classified
        cnt = len(classified)
        print(f"{'OK' if cnt else 'SKIP'} ({cnt} classified)", flush=True)
        time.sleep(SLEEP_SEC if cnt else 5)

    # 写 DB
    print("Writing to database...")
    total_updated = 0
    for i in range(total_batches):
        batch = batches[i]
        classified = results.get(i, {})
        if not classified:
            continue
        for r in batch:
            wid, word, level = r
            topic = classified.get(word.lower())
            if topic and topic in TOPICS:
                cur.execute(
                    "UPDATE word_bank SET topic_code = %s WHERE id = %s",
                    (topic, wid)
                )
                total_updated += 1
        if (i + 1) % 20 == 0:
            conn.commit()
            print(f"  DB committed {i+1}/{total_batches}", flush=True)

    conn.commit()
    cur.close()
    conn.close()
    print(f"\nDone! Updated: {total_updated}/{len(rows)}")

if __name__ == "__main__":
    main()
