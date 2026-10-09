"""
VocabMaster — 从 Iconify 下载抽象词图标（SVG）到本地
用法:
  python scripts/download_images_iconify.py
  python scripts/download_images_iconify.py --limit 100
  python scripts/download_images_iconify.py --skip-existing
  python scripts/download_images_iconify.py --dry-run

Iconify 公开 API（无需 key）:
  搜索: https://api.iconify.design/search?query={word}&limit=5
  下载: https://api.iconify.design/{prefix}/{name}.svg

  优先图标集（清晰、扁平风格）: mdi → fluent → tabler → ph → heroicons
"""
import argparse
import json
import os
import sys
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

ICONIFY_SEARCH = "https://api.iconify.design/search"
ICONIFY_BASE = "https://api.iconify.design"

RATE_LIMIT_S = 0.3   # Iconify 无明确限速，保守 ~200 req/min
REQUEST_TIMEOUT = 15
MAX_RETRIES = 3
MIN_SVG_BYTES = 100

# 优先图标集顺序（扁平、线性风格更适合词汇 app）
PREFERRED_PREFIXES = ("mdi", "fluent", "tabler", "ph", "heroicons", "lucide", "carbon")


def search_iconify(word: str) -> str | None:
    """Return best icon ID (prefix:name) for word, or None."""
    params = urllib.parse.urlencode({"query": word, "limit": 20})
    url = f"{ICONIFY_SEARCH}?{params}"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "VocabMaster/1.0"})
        with urllib.request.urlopen(req, timeout=REQUEST_TIMEOUT) as resp:
            data = json.loads(resp.read())
            icons = data.get("icons", [])
            if not icons:
                return None
            # 优先选偏好图标集
            for prefix in PREFERRED_PREFIXES:
                for icon_id in icons:
                    if icon_id.startswith(prefix + ":"):
                        return icon_id
            return icons[0]
    except (urllib.error.URLError, json.JSONDecodeError, OSError):
        return None


def download_svg(icon_id: str, dest_path: str) -> bool:
    """Download SVG for icon_id (prefix:name) to dest_path."""
    prefix, name = icon_id.split(":", 1)
    url = f"{ICONIFY_BASE}/{prefix}/{name}.svg"
    for attempt in range(MAX_RETRIES):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "VocabMaster/1.0"})
            with urllib.request.urlopen(req, timeout=REQUEST_TIMEOUT) as resp:
                data = resp.read()
                if len(data) < MIN_SVG_BYTES:
                    return False
                with open(dest_path, "wb") as f:
                    f.write(data)
                return True
        except urllib.error.HTTPError as e:
            if e.code == 429 and attempt < MAX_RETRIES - 1:
                time.sleep(5 * (attempt + 1))
            else:
                return False
        except (urllib.error.URLError, OSError):
            if attempt < MAX_RETRIES - 1:
                time.sleep(2)
            else:
                return False
    return False


def main():
    parser = argparse.ArgumentParser(description="Download abstract word icons from Iconify")
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--skip-existing", action="store_true", help="Resume mode")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    os.makedirs(IMAGE_DIR, exist_ok=True)

    conn = pymysql.connect(**DB_CONFIG)
    cur = conn.cursor()

    # 只处理 word_topic.image_type = 'icon' 的词（每个 word_lower 一条）
    limit_clause = f"LIMIT {args.limit}" if args.limit > 0 else ""
    cur.execute(f"""
        SELECT DISTINCT wb.word_lower
        FROM word_bank wb
        JOIN word_topic wt ON wb.topic_code = wt.code
        WHERE wt.image_type = 'icon'
          AND wb.deleted_at IS NULL
          AND (wb.image_url IS NULL OR wb.image_url = '')
        ORDER BY wb.word_lower
        {limit_clause}
    """)
    rows = cur.fetchall()
    cur.close()
    print(f"Icon words to process: {len(rows)}")

    downloaded, skipped, no_result, failed = 0, 0, 0, 0

    for i, (word_lower,) in enumerate(rows):
        safe_name = word_lower.replace(" ", "_").replace("/", "_")
        local_path = os.path.join(IMAGE_DIR, f"{safe_name}.svg")
        local_url = f"/images/words/{safe_name}.svg"

        if args.skip_existing and os.path.exists(local_path) and os.path.getsize(local_path) > MIN_SVG_BYTES:
            skipped += 1
            continue

        icon_id = search_iconify(word_lower)
        time.sleep(RATE_LIMIT_S)

        if not icon_id:
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
            print(f"  [{i+1}] {word_lower} -> {icon_id}")
            downloaded += 1
            continue

        ok = download_svg(icon_id, local_path)
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

        if (i + 1) % 200 == 0:
            print(f"  [{i+1}/{len(rows)}] downloaded={downloaded} skipped={skipped} "
                  f"no_result={no_result} failed={failed}")

    conn.close()
    print(f"\n{'='*50}")
    print(f"Total:       {len(rows)}")
    print(f"Downloaded:  {downloaded}")
    print(f"Skipped:     {skipped}")
    print(f"No result:   {no_result}")
    print(f"Failed:      {failed}")
    if args.dry_run:
        print("(dry-run)")


if __name__ == "__main__":
    main()
