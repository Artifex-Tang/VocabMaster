"""
VocabMaster — 从 Pixabay 下载具象词图片到本地
用法:
  python scripts/download_images_pixabay.py --api-key YOUR_KEY
  python scripts/download_images_pixabay.py --api-key KEY --skip-existing  # 断点续传
  python scripts/download_images_pixabay.py --api-key KEY --limit 100
  python scripts/download_images_pixabay.py --api-key KEY --dry-run

前置条件: 先运行 classify_word_topics.py 生成 word_topic 表。
只处理 word_topic.image_type = 'photo' 的词。
策略: Pixabay photo → illustration 兜底 → NULL
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
RATE_LIMIT_S = 0.75
REQUEST_TIMEOUT = 20
MAX_RETRIES = 3
MIN_IMAGE_BYTES = 1000


def word_stem(word: str) -> str:
    """Return rough stem: strip common suffixes for tag matching."""
    w = word.lower().rstrip("s")   # cats→cat
    for suffix in ("ing", "tion", "ed", "er", "ly", "al", "ful", "ness", "ment"):
        if w.endswith(suffix) and len(w) - len(suffix) >= 3:
            return w[: -len(suffix)]
    return w


def tags_match(word: str, tags_str: str) -> bool:
    """Return True if any tag is semantically close to the search word."""
    stem = word_stem(word)
    for tag in tags_str.lower().split(","):
        tag = tag.strip()
        if tag == word.lower() or tag == stem or word.lower() in tag or tag in word.lower():
            return True
    return False


def search_pixabay(api_key: str, word: str, image_type: str = "photo") -> str | None:
    """Search Pixabay; return URL only if tags validate the word, else None."""
    params = urllib.parse.urlencode({
        "key": api_key,
        "q": word,
        "image_type": image_type,
        "safesearch": "true",
        "per_page": 5,   # fetch more to find a tag-validated hit
        "lang": "en",
    })
    url = f"{PIXABAY_API}?{params}"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "VocabMaster/1.0"})
        with urllib.request.urlopen(req, timeout=REQUEST_TIMEOUT) as resp:
            data = json.loads(resp.read())
            hits = data.get("hits", [])
            # 优先返回 tags 匹配的图片
            for hit in hits:
                if tags_match(word, hit.get("tags", "")):
                    return hit.get("webformatURL") or hit.get("previewURL")
            # 无 tag 匹配但有结果 → 用第一张（可能仍合理）
            if hits:
                return hits[0].get("webformatURL") or hits[0].get("previewURL")
    except (urllib.error.URLError, json.JSONDecodeError, OSError):
        pass
    return None


def find_image(api_key: str, word: str) -> str | None:
    url = search_pixabay(api_key, word, image_type="photo")
    if url:
        return url
    time.sleep(RATE_LIMIT_S)
    return search_pixabay(api_key, word, image_type="illustration")


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
                wait = 10 * (attempt + 1)
                print(f"  [WARN] 429, wait {wait}s...")
                time.sleep(wait)
            else:
                return False
        except (urllib.error.URLError, OSError):
            if attempt < MAX_RETRIES - 1:
                time.sleep(2)
            else:
                return False
    return False


def main():
    parser = argparse.ArgumentParser(description="Download concrete word images from Pixabay")
    parser.add_argument("--api-key", required=True)
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--skip-existing", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    os.makedirs(IMAGE_DIR, exist_ok=True)

    conn = pymysql.connect(**DB_CONFIG)
    cur = conn.cursor()

    limit_clause = f"LIMIT {args.limit}" if args.limit > 0 else ""
    cur.execute(f"""
        SELECT DISTINCT wb.word_lower
        FROM word_bank wb
        JOIN word_topic wt ON wb.topic_code = wt.code
        WHERE wt.image_type = 'photo'
          AND wb.deleted_at IS NULL
          AND (wb.image_url IS NULL OR wb.image_url = '')
        ORDER BY wb.word_lower
        {limit_clause}
    """)
    rows = cur.fetchall()
    cur.close()
    print(f"Photo words to process: {len(rows)}")

    downloaded, skipped, no_result, failed = 0, 0, 0, 0

    for i, (word_lower,) in enumerate(rows):
        safe_name = word_lower.replace(" ", "_").replace("/", "_")
        local_path = os.path.join(IMAGE_DIR, f"{safe_name}.jpg")
        local_url = f"/images/words/{safe_name}.jpg"

        if args.skip_existing and os.path.exists(local_path) and os.path.getsize(local_path) > MIN_IMAGE_BYTES:
            skipped += 1
            continue

        pixabay_url = find_image(args.api_key, word_lower)
        time.sleep(RATE_LIMIT_S)

        if not pixabay_url:
            no_result += 1
            if not args.dry_run:
                cur2 = conn.cursor()
                cur2.execute(
                    "UPDATE word_bank SET image_url = NULL WHERE word_lower = %s AND deleted_at IS NULL",
                    (word_lower,),
                )
                conn.commit()
                cur2.close()
            continue

        if args.dry_run:
            print(f"  [{i+1}] {word_lower} -> {pixabay_url[:60]}...")
            downloaded += 1
            continue

        ok = download_image(pixabay_url, local_path)
        if ok:
            cur2 = conn.cursor()
            cur2.execute(
                "UPDATE word_bank SET image_url = %s WHERE word_lower = %s AND deleted_at IS NULL",
                (local_url, word_lower),
            )
            conn.commit()
            cur2.close()
            downloaded += 1
        else:
            failed += 1

        if (i + 1) % 100 == 0:
            print(f"  [{i+1}/{len(rows)}] downloaded={downloaded} skipped={skipped} "
                  f"no_result={no_result} failed={failed}")

    conn.close()
    print(f"\n{'='*50}")
    print(f"Total:      {len(rows)}")
    print(f"Downloaded: {downloaded}")
    print(f"Skipped:    {skipped}")
    print(f"No result:  {no_result}")
    print(f"Failed:     {failed}")
    if args.dry_run:
        print("(dry-run)")


if __name__ == "__main__":
    main()
