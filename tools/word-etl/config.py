"""
VocabMaster Word ETL Configuration
"""
import os

# ── Paths ──
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, "data")
RAW_DIR = os.path.join(DATA_DIR, "raw")
OUTPUT_DIR = os.path.join(DATA_DIR, "output")

SQLITE_PATH = os.path.join(RAW_DIR, "stardict.db")
CSV_PATH = os.path.join(RAW_DIR, "ecdict.csv")

# ── Database ──
DB_CONFIG = {
    "host": os.getenv("VM_DB_HOST", "127.0.0.1"),
    "port": int(os.getenv("VM_DB_PORT", "3306")),
    "user": os.getenv("VM_DB_USER", "vocab"),
    "password": os.getenv("VM_DB_PASSWORD", "vocab123"),
    "database": os.getenv("VM_DB_NAME", "vocabmaster"),
    "charset": "utf8mb4",
}

# ── Level definitions ──
# tag_filter: ECDICT tag substring to match (empty = frequency-based)
# bnc_max: max BNC rank for frequency-based levels
# collins_min: minimum Collins star rating
# oxford: require oxford=1
# max_word_len: filter by word length
# target_count: desired word count
LEVEL_CONFIGS = {
    "PRIMARY": {
        "tag_filter": None,
        "oxford": 1,
        "collins_min": 3,
        "bnc_max": 5000,
        "max_word_len": 8,
        "target_count": 800,
        "sort_by": "bnc",
    },
    "KET": {
        "tag_filter": None,
        "oxford": 1,
        "collins_min": 4,
        "bnc_max": 3000,
        "max_word_len": None,
        "target_count": 1500,
        "sort_by": "bnc",
    },
    "JUNIOR": {
        "tag_filter": "zk",
        "target_count": 2000,
    },
    "PET": {
        "tag_filter": None,
        "collins_min": 3,
        "bnc_max": 6000,
        "max_word_len": None,
        "target_count": 3500,
        "sort_by": "bnc",
    },
    "SENIOR": {
        "tag_filter": "gk",
        "target_count": 3500,
    },
    "CET4": {
        "tag_filter": "cet4",
        "target_count": 4500,
    },
    "FCE": {
        "tag_filter": None,
        "collins_min": 2,
        "bnc_max": 10000,
        "max_word_len": None,
        "target_count": 5000,
        "sort_by": "bnc",
    },
    "CET6": {
        "tag_filter": "cet6",
        "target_count": 6000,
    },
    "CAE": {
        "tag_filter": None,
        "collins_min": 1,
        "bnc_max": 16000,
        "max_word_len": None,
        "target_count": 7500,
        "sort_by": "bnc",
    },
    "TEM8": {
        "tag_filter": None,  # ECDICT has no tem4/tem8 tags
        "tag_filter_any": ["gre", "ielts"],  # GRE + IELTS as TEM8 proxy
        "target_count": 13000,
    },
}

# ECDICT SQLite download URL
ECDICT_SQLITE_URL = (
    "https://github.com/skywind3000/ECDICT/releases/download/1.0.28/ecdict-sqlite-28.zip"
)
ECDICT_CSV_URL = "https://github.com/skywind3000/ECDICT/raw/master/ecdict.csv"
