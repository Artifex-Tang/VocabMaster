"""
VocabMaster — 批量获取例句 + 近义词 + 反义词 + 衍生词 + 图片
用法:
  python scripts/fetch_examples.py [--limit N] [--level LEVEL]       # 默认: API + WordNet
  python scripts/fetch_examples.py --images [--level LEVEL]          # Wikimedia 图片
  python scripts/fetch_examples.py --skip-api                        # 只 WordNet 衍生词
  python scripts/fetch_examples.py --skip-wordnet                    # 只 API

数据源:
  1. Free Dictionary API (在线): 例句 + synonyms + antonyms
  2. WordNet / NLTK (本地): 衍生词 (derivationally related forms)
  3. Wikimedia Commons (在线): 示意图

写入字段:
  - example_en: 例句
  - related_words (JSON): {synonyms:[], antonyms:[], derived:[]}
"""
import argparse
import json
import os
import sys
import time
import urllib.request
import urllib.error

import pymysql

# ── WordNet ──────────────────────────────────────────────────────────
import nltk
nltk.data.path.append(os.path.join(os.path.dirname(__file__), '..', 'nltk_data'))
try:
    from nltk.corpus import wordnet
except LookupError:
    nltk.download('wordnet', quiet=True)
    nltk.download('omw-1.4', quiet=True)
    from nltk.corpus import wordnet

DB_CONFIG = dict(
    host=os.getenv("DB_HOST", "127.0.0.1"),
    port=int(os.getenv("DB_PORT", 3306)),
    user=os.getenv("DB_USER", "vocab"),
    password=os.getenv("DB_PASS", "vocab123"),
    database=os.getenv("DB_NAME", "vocabmaster"),
    charset="utf8mb4",
)

API_BASE = "https://api.dictionaryapi.dev/api/v2/entries/en/"
RATE_LIMIT_S = 1.1
MAX_DERIVED = 5   # max derived words per entry


# ── API helpers ──────────────────────────────────────────────────────

def fetch_word_data(word: str, retries: int = 2) -> dict | None:
    url = API_BASE + urllib.parse.quote(word)
    for attempt in range(retries + 1):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "VocabMaster/1.0"})
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.loads(resp.read().decode())
                if isinstance(data, list) and data:
                    return data[0]
                return None
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return None
            if e.code == 429:
                wait = 30 * (attempt + 1)
                print(f"    Rate limited, waiting {wait}s...")
                time.sleep(wait)
                continue
            return None
        except (urllib.error.URLError, TimeoutError):
            if attempt < retries:
                time.sleep(5)
                continue
            return None
    return None


def extract_from_api(data: dict) -> dict:
    """Extract example + synonyms + antonyms from Free Dictionary API."""
    result = {"example_en": None, "synonyms": [], "antonyms": []}

    for meaning in data.get("meanings", []):
        # meaning-level synonyms/antonyms
        result["synonyms"].extend(meaning.get("synonyms", []))
        result["antonyms"].extend(meaning.get("antonyms", []))

        for d in meaning.get("definitions", []):
            # first example found
            if result["example_en"] is None and d.get("example"):
                result["example_en"] = d["example"]
            # definition-level synonyms/antonyms
            result["synonyms"].extend(d.get("synonyms", []))
            result["antonyms"].extend(d.get("antonyms", []))

    # deduplicate, limit
    result["synonyms"] = list(dict.fromkeys(result["synonyms"]))[:8]
    result["antonyms"] = list(dict.fromkeys(result["antonyms"]))[:4]

    return result


# ── Wikimedia Commons images ─────────────────────────────────────────

IMAGE_TOPICS = {
    'animals', 'food_drink', 'nature', 'weather', 'body', 'sports',
    'arts', 'technology', 'travel_transport', 'clothing', 'home',
    'science', 'geography', 'entertainment', 'daily_life',
}

SKIP_TITLE = {'logo', 'flag', 'map', 'icon', 'coat of arms', 'heraldic',
              'panoramio', 'wikimedia', 'wikipedia', 'commons-logo'}


def get_wiki_image(word: str, retries: int = 1) -> str | None:
    import urllib.parse
    q = urllib.parse.quote(f'{word} illustration OR photo')
    url = (f'https://commons.wikimedia.org/w/api.php?action=query'
           f'&generator=search&gsrnamespace=6&gsrsearch={q}'
           f'&prop=imageinfo&iiprop=url|size&iiurlwidth=300'
           f'&format=json&limit=5')
    for attempt in range(retries + 1):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "VocabMaster/1.0"})
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.loads(resp.read().decode())
            pages = data.get("query", {}).get("pages", {})
            for pid in sorted(pages, key=int):
                page = pages[pid]
                title = page.get("title", "").lower()
                if any(s in title for s in SKIP_TITLE):
                    continue
                ii = page.get("imageinfo", [{}])
                if ii:
                    thumb = ii[0].get("thumburl")
                    if thumb and thumb.startswith("https://"):
                        return thumb
            return None
        except Exception:
            if attempt < retries:
                time.sleep(3)
            return None
    return None


def run_images(args):
    conn = pymysql.connect(**DB_CONFIG)
    cur = conn.cursor()

    topic_list = ",".join(f"'{t}'" for t in IMAGE_TOPICS)
    where = f"(image_url IS NULL OR image_url = '') AND topic_code IN ({topic_list})"
    if args.level:
        where += f" AND level_code = '{args.level}'"

    cur.execute(f"SELECT COUNT(*) FROM word_bank WHERE {where}")
    total = cur.fetchone()[0]
    limit_clause = f" LIMIT {args.limit}" if args.limit else ""
    print(f"Words needing images (concrete topics only): {total}")

    cur.execute(f"SELECT id, word, topic_code FROM word_bank WHERE {where} ORDER BY id{limit_clause}")
    words = cur.fetchall()
    count = len(words)

    updated = 0
    skipped = 0

    for i, (word_id, word, topic) in enumerate(words):
        if i > 0:
            time.sleep(0.5)

        img = get_wiki_image(word)
        if img is None:
            skipped += 1
            continue

        if not args.dry_run:
            try:
                cur.execute("UPDATE word_bank SET image_url = %s WHERE id = %s", (img, word_id))
                conn.commit()
            except Exception as e:
                print(f"  [{i+1}] DB error for {word}: {e}")
                continue

        updated += 1
        if (i + 1) % 50 == 0:
            pct = (i + 1) / count * 100
            print(f"  [{i+1}/{count}] {pct:.1f}% — updated: {updated}, skipped: {skipped}")

    conn.close()
    print(f"\nImages done! updated={updated}, skipped={skipped}")


# ── WordNet derived forms ────────────────────────────────────────────

def get_derived_forms(word: str) -> list[str]:
    """Get derivationally related forms from WordNet."""
    derived = set()
    for synset in wordnet.synsets(word)[:4]:
        for lemma in synset.lemmas():
            for related in lemma.derivationally_related_forms()[:3]:
                name = related.name().replace('_', ' ')
                if name.lower() != word.lower() and len(name) < 25:
                    derived.add(name)

    # sort by similarity to original word (prefer shorter, alphabetically close)
    result = sorted(derived, key=lambda w: (abs(len(w) - len(word)), w))
    return result[:MAX_DERIVED]


# ── Main ─────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Fetch examples + synonyms + derived words + images")
    parser.add_argument("--limit", type=int, default=0, help="Max words (0=all)")
    parser.add_argument("--dry-run", action="store_true", help="Don't write to DB")
    parser.add_argument("--level", type=str, default=None, help="Only this level_code")
    parser.add_argument("--skip-api", action="store_true", help="Only run WordNet (no API calls)")
    parser.add_argument("--skip-wordnet", action="store_true", help="Only run API (no WordNet)")
    parser.add_argument("--images", action="store_true", help="Fetch images from Wikimedia Commons")
    args = parser.parse_args()

    if args.images:
        run_images(args)
        return

    conn = pymysql.connect(**DB_CONFIG)
    cur = conn.cursor()

    # words needing enrichment: missing example OR missing related_words
    where_parts = ["(example_en IS NULL OR example_en = '')"]
    where_parts.append("(related_words IS NULL OR related_words = '' OR related_words = '{}')")
    where = " AND ".join([f"({p})" for p in where_parts])

    if args.level:
        where += f" AND level_code = '{args.level}'"

    cur.execute(f"SELECT COUNT(*) FROM word_bank WHERE {where}")
    total = cur.fetchone()[0]
    print(f"Words to enrich: {total}")
    if args.limit:
        total = min(total, args.limit)
        print(f"  (limited to {total})")

    cur.execute(f"SELECT id, word FROM word_bank WHERE {where} ORDER BY id LIMIT {args.limit or total}")
    words = cur.fetchall()

    updated = 0
    skipped = 0
    errors = 0

    for i, (word_id, word) in enumerate(words):
        example_en = None
        synonyms = []
        antonyms = []
        derived = []

        # ── API call ──
        if not args.skip_api:
            if i > 0:
                time.sleep(RATE_LIMIT_S)

            api_data = fetch_word_data(word)
            if api_data:
                extracted = extract_from_api(api_data)
                example_en = extracted["example_en"]
                synonyms = extracted["synonyms"]
                antonyms = extracted["antonyms"]
            else:
                skipped += 1

        # ── WordNet derived ──
        if not args.skip_wordnet:
            derived = get_derived_forms(word)

        # ── Build related_words JSON ──
        has_related = synonyms or antonyms or derived
        related_json = None
        if has_related:
            related_json = json.dumps({
                "synonyms": synonyms,
                "antonyms": antonyms,
                "derived": derived,
            }, ensure_ascii=False)

        # ── Write to DB ──
        if args.dry_run:
            if example_en or has_related:
                parts = []
                if example_en: parts.append(f'ex="{example_en[:50]}"')
                if synonyms: parts.append(f'syn={synonyms[:3]}')
                if derived: parts.append(f'deriv={derived[:3]}')
                print(f"  [{word}] {', '.join(parts)}")
            updated += 1
            continue

        if example_en or has_related:
            try:
                if example_en and related_json:
                    cur.execute(
                        "UPDATE word_bank SET example_en = %s, related_words = %s WHERE id = %s",
                        (example_en, related_json, word_id),
                    )
                elif example_en:
                    cur.execute(
                        "UPDATE word_bank SET example_en = %s WHERE id = %s",
                        (example_en, word_id),
                    )
                elif related_json:
                    cur.execute(
                        "UPDATE word_bank SET related_words = %s WHERE id = %s",
                        (related_json, word_id),
                    )
                conn.commit()
                updated += 1
            except Exception as e:
                print(f"  [{i+1}] DB error for {word}: {e}")
                errors += 1
                continue

        if (i + 1) % 50 == 0 or i + 1 == total:
            pct = (i + 1) / total * 100
            print(f"  [{i+1}/{total}] {pct:.1f}% — updated: {updated}, skipped: {skipped}, errors: {errors}")

    conn.close()
    print(f"\nDone! updated={updated}, skipped={skipped}, errors={errors}")


if __name__ == "__main__":
    main()
