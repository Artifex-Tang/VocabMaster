"""
Step 1: Download ECDICT data.
Tries SQLite release first (faster queries), falls back to CSV.
"""
import os
import sys
import zipfile
import requests
from tqdm import tqdm

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from config import RAW_DIR, SQLITE_PATH, CSV_PATH, ECDICT_SQLITE_URL, ECDICT_CSV_URL

os.makedirs(RAW_DIR, exist_ok=True)


def download_file(url: str, dest: str) -> bool:
    """Download with progress bar."""
    if os.path.exists(dest):
        print(f"  Already exists: {dest}")
        return True
    print(f"  Downloading {url} ...")
    try:
        resp = requests.get(url, stream=True, timeout=60)
        resp.raise_for_status()
        total = int(resp.headers.get("content-length", 0))
        with open(dest, "wb") as f:
            with tqdm(total=total, unit="B", unit_scale=True) as pbar:
                for chunk in resp.iter_content(chunk_size=8192):
                    f.write(chunk)
                    pbar.update(len(chunk))
        print(f"  Saved: {dest} ({os.path.getsize(dest) / 1024 / 1024:.1f} MB)")
        return True
    except Exception as e:
        print(f"  Download failed: {e}")
        if os.path.exists(dest):
            os.remove(dest)
        return False


def extract_sqlite(zip_path: str) -> bool:
    """Extract stardict.db from zip."""
    if os.path.exists(SQLITE_PATH):
        print(f"  SQLite already exists: {SQLITE_PATH}")
        return True
    print(f"  Extracting {zip_path} ...")
    try:
        with zipfile.ZipFile(zip_path, "r") as zf:
            for name in zf.namelist():
                if name.endswith(".db"):
                    print(f"    Found: {name}")
                    with zf.open(name) as src, open(SQLITE_PATH, "wb") as dst:
                        dst.write(src.read())
                    print(f"  Extracted: {SQLITE_PATH}")
                    return True
        print("  No .db file found in zip")
        return False
    except Exception as e:
        print(f"  Extraction failed: {e}")
        return False


def main():
    print("=== ECDICT Data Download ===")

    # Try SQLite first
    sqlite_zip = os.path.join(RAW_DIR, "ecdict-sqlite-28.zip")
    if download_file(ECDICT_SQLITE_URL, sqlite_zip):
        if extract_sqlite(sqlite_zip):
            print("\nSQLite database ready.")
            return

    # Fallback to CSV
    print("\nFalling back to CSV download...")
    if download_file(ECDICT_CSV_URL, CSV_PATH):
        print("CSV ready (will be slower for queries).")
        return

    print("\nERROR: Could not download ECDICT data.")
    print("Please manually download from:")
    print(f"  {ECDICT_SQLITE_URL}")
    print(f"  or {ECDICT_CSV_URL}")
    sys.exit(1)


if __name__ == "__main__":
    main()
