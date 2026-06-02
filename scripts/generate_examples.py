"""
VocabMaster — 用 DeepSeek-V4-Flash 批量生成英文例句
用法:
  python scripts/generate_examples.py                       # 生成全部缺失例句
  python scripts/generate_examples.py --limit 100           # 只处理 100 个
  python scripts/generate_examples.py --level CET4           # 只处理指定等级
  python scripts/generate_examples.py --batch-size 10        # 每批 10 个词
  python scripts/generate_examples.py --dry-run              # 只统计，不调用 API

成本估算（DeepSeek-V4-Flash 非 thinking mode）:
  - 36,067 词 ÷ 20/批 ≈ 1,804 次 API 调用
  - Input:  ~2.9M tokens × $0.14/1M ≈ $0.41
  - Output: ~1.1M tokens × $0.28/1M ≈ $0.31
  - 总计: ≈ $0.72 (≈ ¥5.2)

环境变量:
  DEEPSEEK_API_KEY  — DeepSeek API Key (必填，除非 --dry-run)
  DB_HOST           — MySQL 主机 (默认 127.0.0.1)
  DB_PORT           — MySQL 端口 (默认 3306)
  DB_USER           — MySQL 用户 (默认 root)
  DB_PASS           — MySQL 密码 (默认 root)
  DB_NAME           — MySQL 数据库 (默认 vocabmaster)
"""
import argparse
import json
import os
import sys
import time
import urllib.request
import urllib.error

import pymysql

# ── Config ──────────────────────────────────────────────────────────

DB_CONFIG = dict(
    host=os.getenv("DB_HOST", "127.0.0.1"),
    port=int(os.getenv("DB_PORT", 3306)),
    user=os.getenv("DB_USER", "root"),
    password=os.getenv("DB_PASS", "root"),
    database=os.getenv("DB_NAME", "vocabmaster"),
    charset="utf8mb4",
)

DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "")
DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions"
MODEL = "deepseek-chat"  # maps to DeepSeek-V4-Flash non-thinking mode
DEFAULT_BATCH_SIZE = 20
RATE_LIMIT_S = 0.3  # seconds between API calls

SYSTEM_PROMPT = """You are an English vocabulary teacher. For each word below, write ONE natural example sentence.

Rules:
- The sentence must clearly demonstrate the word's meaning as defined
- Use intermediate difficulty grammar (B1-B2 level, suitable for ESL learners)
- Each sentence should be 8-20 words long
- Do NOT start every sentence with "The"
- Vary sentence structures (questions, conditionals, negatives, etc.)
- Output a JSON array with objects: {"word": "xxx", "example_en": "..."}
- Output ONLY the JSON array, no markdown fences, no explanation"""


def call_deepseek(words: list[dict]) -> list[dict]:
    """Call DeepSeek API with a batch of words. Returns list of {word, example_en}."""
    if not DEEPSEEK_API_KEY:
        raise RuntimeError("DEEPSEEK_API_KEY not set")

    # Build user message
    lines = []
    for i, w in enumerate(words, 1):
        pos = w.get("pos", "")
        pos_str = f" ({pos})" if pos else ""
        zh = w.get("zh_definition", "")
        lines.append(f"{i}. {w['word']}{pos_str} — {zh}")
    user_msg = "\n".join(lines)

    payload = json.dumps({
        "model": MODEL,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_msg},
        ],
        "temperature": 0.7,
        "max_tokens": 2000,
        "response_format": {"type": "json_object"},
    }).encode("utf-8")

    req = urllib.request.Request(
        DEEPSEEK_API_URL,
        data=payload,
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {DEEPSEEK_API_KEY}",
        },
    )

    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                result = json.loads(resp.read().decode("utf-8"))
            content = result["choices"][0]["message"]["content"]
            # Parse JSON — API may return {"examples": [...]} or just [...]
            parsed = json.loads(content)
            if isinstance(parsed, dict):
                # Try common wrapper keys
                for key in ("examples", "data", "results", "words"):
                    if key in parsed and isinstance(parsed[key], list):
                        return parsed[key]
                # Fallback: find first list value
                for v in parsed.values():
                    if isinstance(v, list):
                        return v
            elif isinstance(parsed, list):
                return parsed
            return []
        except urllib.error.HTTPError as e:
            body = e.read().decode("utf-8", errors="replace")
            if e.code == 429:
                wait = 5 * (attempt + 1)
                print(f"  [WARN] Rate limited, waiting {wait}s...")
                time.sleep(wait)
                continue
            print(f"  X API error {e.code}: {body[:200]}")
            return []
        except (json.JSONDecodeError, KeyError) as e:
            print(f"  X Parse error: {e}")
            return []
        except (urllib.error.URLError, OSError) as e:
            print(f"  X Network error: {e}")
            if attempt < 2:
                time.sleep(3)
            return []
    return []


def main():
    parser = argparse.ArgumentParser(description="Generate example sentences using DeepSeek")
    parser.add_argument("--limit", type=int, default=0, help="Max words to process (0=all)")
    parser.add_argument("--level", type=str, default="", help="Filter by level_code")
    parser.add_argument("--batch-size", type=int, default=DEFAULT_BATCH_SIZE, help="Words per API call")
    parser.add_argument("--dry-run", action="store_true", help="Count only, no API calls")
    parser.add_argument("--api-key", type=str, default="", help="DeepSeek API key (or set DEEPSEEK_API_KEY env)")
    args = parser.parse_args()

    # Resolve API key: CLI arg > env var > .env file
    global DEEPSEEK_API_KEY
    if args.api_key:
        DEEPSEEK_API_KEY = args.api_key
    elif not DEEPSEEK_API_KEY:
        # Try loading from .env file
        env_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".env")
        if os.path.exists(env_path):
            with open(env_path, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if line.startswith("DEEPSEEK_API_KEY="):
                        DEEPSEEK_API_KEY = line.split("=", 1)[1].strip().strip("\"'")
                        break

    if not args.dry_run and not DEEPSEEK_API_KEY:
        print("Error: DEEPSEEK_API_KEY environment variable not set")
        print("Usage: DEEPSEEK_API_KEY=sk-xxx python scripts/generate_examples.py")
        sys.exit(1)

    conn = pymysql.connect(**DB_CONFIG)
    cur = conn.cursor()

    # Query words missing example_en
    where_parts = [
        "(example_en IS NULL OR example_en = '')",
        "deleted_at IS NULL",
    ]
    if args.level:
        where_parts.append(f"level_code = '{args.level}'")

    where = " AND ".join(where_parts)

    cur.execute(f"SELECT COUNT(*) FROM word_bank WHERE {where}")
    total = cur.fetchone()[0]
    print(f"Words missing example_en: {total}")

    if total == 0:
        print("All words have examples. Nothing to do.")
        conn.close()
        return

    limit_clause = f" LIMIT {args.limit}" if args.limit > 0 else ""
    cur.execute(
        f"SELECT id, word, pos, zh_definition FROM word_bank WHERE {where} ORDER BY id{limit_clause}"
    )
    rows = cur.fetchall()
    print(f"Processing {len(rows)} words in batches of {args.batch_size}")

    total_batches = (len(rows) + args.batch_size - 1) // args.batch_size
    updated = 0
    failed_batches = 0

    for batch_idx in range(total_batches):
        start = batch_idx * args.batch_size
        end = min(start + args.batch_size, len(rows))
        batch_rows = rows[start:end]

        # Build word list for this batch
        words = [
            {"id": r[0], "word": r[1], "pos": r[2] or "", "zh_definition": r[3] or ""}
            for r in batch_rows
        ]
        word_names = [w["word"] for w in words]

        if args.dry_run:
            print(f"  [Batch {batch_idx + 1}/{total_batches}] would process: {', '.join(word_names[:5])}...")
            updated += len(words)
            continue

        print(f"  [Batch {batch_idx + 1}/{total_batches}] {', '.join(word_names[:5])}"
              f"{'...' if len(word_names) > 5 else ''}")

        # Call API
        results = call_deepseek(words)

        if not results:
            print(f"    X Empty response, skipping batch")
            failed_batches += 1
            time.sleep(RATE_LIMIT_S)
            continue

        # Map results by word
        result_map = {}
        for r in results:
            w = r.get("word", "").lower().strip()
            ex = r.get("example_en", "").strip()
            if w and ex:
                result_map[w] = ex

        # Update DB
        batch_updated = 0
        for w in words:
            example = result_map.get(w["word"].lower())
            if example:
                try:
                    cur.execute(
                        "UPDATE word_bank SET example_en = %s WHERE id = %s",
                        (example, w["id"]),
                    )
                    batch_updated += 1
                except Exception as e:
                    print(f"    X DB error for {w['word']}: {e}")

        conn.commit()
        updated += batch_updated
        print(f"    [OK] Updated {batch_updated}/{len(words)} examples")

        if batch_idx < total_batches - 1:
            time.sleep(RATE_LIMIT_S)

    conn.close()

    print(f"\n{'='*50}")
    print(f"Total processed: {len(rows)}")
    print(f"Examples updated: {updated}")
    print(f"Failed batches: {failed_batches}")
    if args.dry_run:
        print("(dry-run mode, no changes made)")
    else:
        remaining = total - updated
        print(f"Remaining without examples: {remaining}")


if __name__ == "__main__":
    main()
