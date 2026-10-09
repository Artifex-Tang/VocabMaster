"""
VocabMaster — 用 DeepSeek 为抽象词生成 Pixabay 视觉搜索词，再下载图片
用法:
  python scripts/enrich_visual_queries.py --api-key PIXABAY_KEY --ds-key DEEPSEEK_KEY
  python scripts/enrich_visual_queries.py --api-key KEY --ds-key DSKEY --limit 50 --dry-run

流程:
  1. 从 word_bank 取 topic image_type='icon' 的唯一词（尚无 image_url）
  2. 批量发给 DeepSeek，生成 2-4 个英文视觉关键词（具体场景/物体）
  3. 用关键词搜 Pixabay，做 tags 校验，下载 → 更新 image_url

成本: DeepSeek-V3 文本 ~¥0.001/词，9000词约 ¥9
"""
import argparse
import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request

import pymysql

DB_CONFIG = dict(
    host=os.getenv("DB_HOST", "127.0.0.1"),
    port=int(os.getenv("DB_PORT", 3306)),
    user=os.getenv("DB_USER", "root"),
    password=os.getenv("DB_PASS", "root"),
    database=os.getenv("DB_NAME", "vocabmaster"),
    charset="utf8mb4",
)

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.join(SCRIPT_DIR, "..")
IMAGE_DIR = os.path.join(PROJECT_DIR, "backend-java", "static", "images", "words")

PIXABAY_API = "https://pixabay.com/api/"
DEEPSEEK_API = "https://api.deepseek.com/chat/completions"

RATE_LIMIT_S = 0.75
REQUEST_TIMEOUT = 30
MAX_RETRIES = 3
MIN_IMAGE_BYTES = 1000
DS_BATCH = 30  # words per DeepSeek call


# ── DeepSeek ─────────────────────────────────────────────────────────

SYSTEM_PROMPT = (
    "You are a visual search assistant. Given English vocabulary words, "
    "output a JSON object mapping each word to 2-4 concrete English keywords "
    "that would find a relevant image on a photo stock site. "
    "Focus on physical objects, scenes, or symbols — NOT the abstract word itself. "
    "Example: {\"justice\": \"balance scale courtroom\", \"happy\": \"smiling face child laughing\", "
    "\"abandon\": \"empty house lonely\"}"
)


def generate_visual_queries(ds_key: str, words: list[str]) -> dict[str, str]:
    """Return {word: visual_query_string} for given words."""
    payload = json.dumps({
        "model": "deepseek-chat",
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": "Words: " + json.dumps(words)},
        ],
        "temperature": 0.3,
        "max_tokens": len(words) * 20,
    }).encode()
    req = urllib.request.Request(
        DEEPSEEK_API,
        data=payload,
        headers={
            "Authorization": f"Bearer {ds_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=REQUEST_TIMEOUT) as resp:
            data = json.loads(resp.read())
            content = data["choices"][0]["message"]["content"]
            # Extract JSON block
            start = content.find("{")
            end = content.rfind("}") + 1
            if start != -1 and end > start:
                return json.loads(content[start:end])
    except Exception as e:
        print(f"  [WARN] DeepSeek error: {e}")
    return {}


# ── Pixabay ──────────────────────────────────────────────────────────

def query_matches_tags(query: str, tags_str: str) -> bool:
    """At least one query keyword (len>2) appears in image tags."""
    query_words = {q.strip().lower() for q in query.split() if len(q) > 2}
    tags = {t.strip().lower() for t in tags_str.split(",")}
    return bool(query_words & tags)


def search_pixabay(api_key: str, query: str) -> str | None:
    params = urllib.parse.urlencode({
        "key": api_key,
        "q": query,
        "image_type": "all",
        "safesearch": "true",
        "per_page": 5,
        "lang": "en",
    })
    url = f"{PIXABAY_API}?{params}"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "VocabMaster/1.0"})
        with urllib.request.urlopen(req, timeout=REQUEST_TIMEOUT) as resp:
            data = json.loads(resp.read())
            hits = data.get("hits", [])
            # 优先取 tags 与 query 关键词有交集的图
            for hit in hits:
                if query_matches_tags(query, hit.get("tags", "")):
                    return hit.get("webformatURL") or hit.get("previewURL")
            # 无 tag 匹配 → NULL，宁缺毋滥
            return None
    except (urllib.error.URLError, json.JSONDecodeError, OSError):
        pass
    return None


def download_image(url: str, dest_path: str) -> bool:
    for attempt in range(MAX_RETRIES):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "VocabMaster/1.0"})
            with urllib.request.urlopen(req, timeout=REQUEST_TIMEOUT) as resp:
                data = resp.read()
                if len(data) < MIN_IMAGE_BYTES:
                    return False
                with open(dest_path, "wb") as f:
                    f.write(data)
                return True
        except urllib.error.HTTPError as e:
            if e.code == 429 and attempt < MAX_RETRIES - 1:
                time.sleep(10 * (attempt + 1))
            else:
                return False
        except (urllib.error.URLError, OSError):
            if attempt < MAX_RETRIES - 1:
                time.sleep(2)
            else:
                return False
    return False


# ── Main ─────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--api-key", required=True, help="Pixabay API key")
    parser.add_argument("--ds-key", required=True, help="DeepSeek API key")
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--skip-existing", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    os.makedirs(IMAGE_DIR, exist_ok=True)

    conn = pymysql.connect(**DB_CONFIG)
    cur = conn.cursor()

    limit_clause = f"LIMIT {args.limit}" if args.limit > 0 else ""
    cur.execute(f"""
        SELECT DISTINCT wb.word_lower, wb.en_definition
        FROM word_bank wb
        JOIN word_topic wt ON wb.topic_code = wt.code
        WHERE wt.image_type = 'icon'
          AND wb.deleted_at IS NULL
          AND (wb.image_url IS NULL OR wb.image_url = '')
          AND wb.word_lower NOT IN (
              SELECT DISTINCT word_lower FROM word_bank wb2
              JOIN word_topic wt2 ON wb2.topic_code = wt2.code
              WHERE wt2.image_type = 'photo' AND wb2.deleted_at IS NULL
          )
        ORDER BY wb.word_lower
        {limit_clause}
    """)
    rows = cur.fetchall()
    cur.close()
    print(f"Abstract words to process: {len(rows)}")

    downloaded, no_result, failed = 0, 0, 0

    # Process in batches
    for batch_start in range(0, len(rows), DS_BATCH):
        batch = rows[batch_start: batch_start + DS_BATCH]
        words = [r[0] for r in batch]

        print(f"\n[{batch_start+1}-{batch_start+len(batch)}] Querying DeepSeek...")
        visual_map = generate_visual_queries(args.ds_key, words)

        for word_lower, en_def in batch:
            query = visual_map.get(word_lower, "").strip()
            if not query:
                no_result += 1
                continue

            safe_name = word_lower.replace(" ", "_").replace("/", "_")
            local_path = os.path.join(IMAGE_DIR, f"{safe_name}.jpg")
            local_url = f"/images/words/{safe_name}.jpg"

            if args.skip_existing and os.path.exists(local_path) and os.path.getsize(local_path) > MIN_IMAGE_BYTES:
                downloaded += 1
                continue

            if args.dry_run:
                print(f"  {word_lower:20s} -> query: '{query}'")
                downloaded += 1
                continue

            pix_url = search_pixabay(args.api_key, query)
            time.sleep(RATE_LIMIT_S)

            if not pix_url:
                no_result += 1
                continue

            ok = download_image(pix_url, local_path)
            if ok:
                cur2 = conn.cursor()
                cur2.execute(
                    "UPDATE word_bank SET image_url=%s WHERE word_lower=%s AND deleted_at IS NULL",
                    (local_url, word_lower),
                )
                conn.commit()
                cur2.close()
                downloaded += 1
            else:
                failed += 1

        # Rate limit between batches
        time.sleep(1.0)

        if (batch_start + DS_BATCH) % 300 == 0:
            print(f"Progress: downloaded={downloaded} no_result={no_result} failed={failed}")

    conn.close()
    print(f"\n{'='*50}")
    print(f"Total:      {len(rows)}")
    print(f"Downloaded: {downloaded}")
    print(f"No result:  {no_result}")
    print(f"Failed:     {failed}")


if __name__ == "__main__":
    main()
