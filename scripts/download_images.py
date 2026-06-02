"""
VocabMaster — 下载 Wikimedia 图片到本地
用法:
  python scripts/download_images.py                    # 下载全部
  python scripts/download_images.py --limit 100        # 只下载 100 张
  python scripts/download_images.py --level CET4       # 只下载指定等级
  python scripts/download_images.py --dry-run          # 只统计，不下载

功能:
  1. 从 word_bank 读取 image_url (以 https://upload.wikimedia.org 开头)
  2. 下载缩略图到 backend-java/static/images/words/{word_lower}.jpg
  3. 更新 DB image_url 为 /images/words/{word_lower}.jpg
  4. 跳过已存在的文件（断点续传）
"""
import argparse
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

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.join(SCRIPT_DIR, "..")
IMAGE_DIR = os.path.join(PROJECT_DIR, "backend-java", "static", "images", "words")

RATE_LIMIT_S = 1.0   # seconds between requests (Wikimedia rate limit)
REQUEST_TIMEOUT = 15  # seconds
MAX_RETRIES = 2


def download_image(url: str, dest_path: str, retries: int = MAX_RETRIES) -> bool:
    """Download image from URL to local file."""
    for attempt in range(retries + 1):
        try:
            req = urllib.request.Request(url, headers={
                "User-Agent": "VocabMaster/1.0 (https://github.com/vocabmaster)"
            })
            with urllib.request.urlopen(req, timeout=REQUEST_TIMEOUT) as resp:
                data = resp.read()
                if len(data) < 500:  # skip tiny files (likely placeholders)
                    return False
                with open(dest_path, "wb") as f:
                    f.write(data)
                return True
        except urllib.error.HTTPError as e:
            if e.code == 429 and attempt < retries:
                wait = 5 * (attempt + 1)
                print(f"  [WARN] 429 rate limited, waiting {wait}s...")
                time.sleep(wait)
                continue
            return False
        except (urllib.error.URLError, OSError) as e:
            if attempt < retries:
                time.sleep(2)
            else:
                print(f"  [FAIL] download failed after {retries + 1} attempts: {e}")
                return False
    return False


def main():
    parser = argparse.ArgumentParser(description="Download Wikimedia images to local storage")
    parser.add_argument("--limit", type=int, default=0, help="Max words to process (0=all)")
    parser.add_argument("--level", type=str, default="", help="Filter by level_code")
    parser.add_argument("--dry-run", action="store_true", help="Count only, no download")
    args = parser.parse_args()

    # Ensure output directory exists
    os.makedirs(IMAGE_DIR, exist_ok=True)
    print(f"Output directory: {IMAGE_DIR}")

    # Connect to DB
    conn = pymysql.connect(**DB_CONFIG)
    cur = conn.cursor()

    # Query words with external Wikimedia URLs
    where_parts = [
        "image_url IS NOT NULL",
        "image_url != ''",
        "image_url LIKE 'https://upload.wikimedia.org%'",
    ]
    if args.level:
        where_parts.append(f"level_code = '{args.level}'")

    where = " AND ".join(where_parts)
    limit_clause = f" LIMIT {args.limit}" if args.limit > 0 else ""

    cur.execute(f"SELECT COUNT(*) FROM word_bank WHERE {where}")
    total = cur.fetchone()[0]
    print(f"Words with Wikimedia image_url: {total}")

    if total == 0:
        print("Nothing to do.")
        conn.close()
        return

    cur.execute(
        f"SELECT id, word, word_lower, image_url FROM word_bank WHERE {where} ORDER BY id{limit_clause}"
    )
    rows = cur.fetchall()

    downloaded = 0
    skipped_existing = 0
    skipped_failed = 0

    for i, (word_id, word, word_lower, image_url) in enumerate(rows):
        # Use word_lower for filename (safe for filesystem)
        safe_name = (word_lower or word.lower()).replace(" ", "_")
        local_path = os.path.join(IMAGE_DIR, f"{safe_name}.jpg")
        local_url = f"/images/words/{safe_name}.jpg"

        # Skip if file already exists (resume support)
        if os.path.exists(local_path) and os.path.getsize(local_path) > 500:
            # Still update DB if not yet updated
            cur.execute(
                "SELECT image_url FROM word_bank WHERE id = %s", (word_id,)
            )
            current_url = cur.fetchone()[0]
            if current_url != local_url:
                if not args.dry_run:
                    cur.execute(
                        "UPDATE word_bank SET image_url = %s WHERE id = %s",
                        (local_url, word_id),
                    )
                    conn.commit()
                skipped_existing += 1
            else:
                skipped_existing += 1
            if (i + 1) % 100 == 0:
                print(f"  [{i + 1}/{len(rows)}] skipped (exists): {word}")
            continue

        if args.dry_run:
            print(f"  [{i + 1}] would download: {word} <- {image_url[:80]}...")
            skipped_failed += 1  # reused as counter
            continue

        # Download
        if i > 0:
            time.sleep(RATE_LIMIT_S)

        success = download_image(image_url, local_path)
        if success:
            # Update DB to local URL
            cur.execute(
                "UPDATE word_bank SET image_url = %s WHERE id = %s",
                (local_url, word_id),
            )
            conn.commit()
            downloaded += 1
            if (downloaded) % 50 == 0:
                print(f"  [{i + 1}/{len(rows)}] downloaded {downloaded}, "
                      f"skipped {skipped_existing}, failed {skipped_failed}")
        else:
            skipped_failed += 1
            if (i + 1) % 100 == 0:
                print(f"  [{i + 1}/{len(rows)}] download {downloaded}, "
                      f"skipped {skipped_existing}, failed {skipped_failed}")

    conn.close()

    print(f"\n{'='*50}")
    print(f"Total:   {len(rows)}")
    print(f"Downloaded: {downloaded}")
    print(f"Skipped (already exists): {skipped_existing}")
    print(f"Failed:  {skipped_failed}")
    if args.dry_run:
        print(f"(dry-run mode, no files downloaded)")


if __name__ == "__main__":
    main()
