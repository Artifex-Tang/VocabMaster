"""
Step 2: Extract words by level from ECDICT SQLite.
Outputs per-level CSV files to data/output/.
"""
import os
import re
import sys
import sqlite3

import pandas as pd
from tqdm import tqdm

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from config import RAW_DIR, OUTPUT_DIR, SQLITE_PATH, LEVEL_CONFIGS

SQLITE_PATH = os.path.join(RAW_DIR, "stardict.db")
os.makedirs(OUTPUT_DIR, exist_ok=True)

# Only keep pure alphabetic words (no spaces, hyphens, digits, dots)
WORD_RE = re.compile(r"^[a-zA-Z]+$")


def is_pure_word(word: str) -> bool:
    return bool(WORD_RE.match(word))


def load_all_words(conn: sqlite3.Connection) -> pd.DataFrame:
    """Load all pure alphabetic words with BNC > 0 or with tags."""
    print("Loading words from SQLite...")
    query = """
        SELECT word, sw, phonetic, definition, translation, pos,
               collins, oxford, tag, bnc, frq
        FROM stardict
        WHERE (tag IS NOT NULL AND tag != '')
           OR (bnc IS NOT NULL AND bnc > 0)
           OR (collins IS NOT NULL AND collins > 0)
    """
    df = pd.read_sql_query(query, conn)
    print(f"  Total rows: {len(df)}")

    # Filter to pure alphabetic words only
    mask = df["word"].apply(is_pure_word)
    df = df[mask].copy()
    print(f"  Pure alpha words: {len(df)}")

    # Normalize
    df["word_lower"] = df["word"].str.lower()
    df["collins"] = df["collins"].fillna(0).astype(int)
    df["oxford"] = df["oxford"].fillna(0).astype(int)
    df["bnc"] = pd.to_numeric(df["bnc"], errors="coerce")
    df["frq"] = pd.to_numeric(df["frq"], errors="coerce")

    return df


def has_tag(tag_str: str, tag: str) -> bool:
    """Check if tag string contains the given tag."""
    if not tag_str:
        return False
    return tag in str(tag_str).split()


def extract_tag_based(df: pd.DataFrame, tag: str) -> pd.DataFrame:
    """Extract words matching a specific ECDICT tag."""
    mask = df["tag"].apply(lambda t: has_tag(t, tag))
    result = df[mask].copy()
    return result


def extract_tag_any(df: pd.DataFrame, tags: list) -> pd.DataFrame:
    """Extract words matching any of the given tags."""
    mask = df["tag"].apply(lambda t: any(has_tag(t, tag) for tag in tags) if t else False)
    return df[mask].copy()


def extract_frequency_based(
    df: pd.DataFrame,
    bnc_max: int,
    collins_min: int = 0,
    oxford: int = 0,
    max_word_len: int | None = None,
    target: int = 0,
    sort_by: str = "bnc",
) -> pd.DataFrame:
    """Extract words by frequency criteria."""
    result = df.copy()

    # Must have BNC rank
    result = result[result["bnc"].notna() & (result["bnc"] > 0)]

    if bnc_max:
        result = result[result["bnc"] <= bnc_max]

    if collins_min:
        result = result[result["collins"] >= collins_min]

    if oxford:
        result = result[result["oxford"] >= oxford]

    if max_word_len:
        result = result[result["word"].str.len() <= max_word_len]

    # Sort by frequency (lowest BNC = most common)
    result = result.sort_values(sort_by, ascending=True, na_position="last")

    if target and len(result) > target:
        result = result.head(target)

    return result


def main():
    conn = sqlite3.connect(SQLITE_PATH)
    df = load_all_words(conn)
    conn.close()

    results = {}

    for level_code, cfg in LEVEL_CONFIGS.items():
        print(f"\n--- {level_code} ---")
        tag_filter = cfg.get("tag_filter")
        tag_any = cfg.get("tag_filter_any")
        target = cfg.get("target_count", 0)

        if tag_filter:
            level_df = extract_tag_based(df, tag_filter)
        elif tag_any:
            level_df = extract_tag_any(df, tag_any)
        else:
            level_df = extract_frequency_based(
                df,
                bnc_max=cfg.get("bnc_max", 999999),
                collins_min=cfg.get("collins_min", 0),
                oxford=cfg.get("oxford", 0),
                max_word_len=cfg.get("max_word_len"),
                target=target,
                sort_by=cfg.get("sort_by", "bnc"),
            )

        # Deduplicate by word_lower (keep first = highest frequency)
        level_df = level_df.drop_duplicates(subset=["word_lower"], keep="first")

        # Ensure minimum data quality: has translation
        level_df = level_df[level_df["translation"].notna() & (level_df["translation"] != "")]

        print(f"  Extracted: {len(level_df)} words (target: {target})")

        # Save
        output_path = os.path.join(OUTPUT_DIR, f"words_{level_code}.csv")
        level_df.to_csv(output_path, index=False, encoding="utf-8")
        print(f"  Saved: {output_path}")
        results[level_code] = len(level_df)

    # Summary
    print("\n=== Summary ===")
    for level, count in results.items():
        target = LEVEL_CONFIGS[level].get("target_count", "?")
        status = "OK" if count >= (target * 0.8 if isinstance(target, int) else 0) else "LOW"
        print(f"  {level}: {count} / {target} {status}")

    print("\nNext: python 03_transform.py")


if __name__ == "__main__":
    main()
