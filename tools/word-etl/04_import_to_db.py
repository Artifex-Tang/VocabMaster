"""
Step 4: Import generated SQL into MySQL via docker exec.
Faster than pymysql for large SQL files.
"""
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from config import DB_CONFIG, OUTPUT_DIR

SQL_PATH = os.path.join(OUTPUT_DIR, "import_wordbank.sql")


def main():
    if not os.path.exists(SQL_PATH):
        print(f"ERROR: {SQL_PATH} not found. Run 03_transform.py first.")
        sys.exit(1)

    file_size = os.path.getsize(SQL_PATH) / 1024 / 1024
    print(f"=== Import word_bank ===")
    print(f"SQL file: {SQL_PATH} ({file_size:.1f} MB)")

    # Copy SQL into MySQL container and execute
    print("Copying SQL into MySQL container...")
    cp_result = subprocess.run(
        ["docker", "cp", SQL_PATH, "vocab-mysql:/tmp/import_wordbank.sql"],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
    )
    if cp_result.returncode != 0:
        print(f"docker cp failed: {cp_result.stderr}")
        sys.exit(1)
    print("  Copied.")

    print("Executing SQL import...")
    mysql_cmd = (
        f'mysql -u{DB_CONFIG["user"]} -p{DB_CONFIG["password"]} '
        f'{DB_CONFIG["database"]} -e "source /tmp/import_wordbank.sql"'
    )
    result = subprocess.run(
        ["docker", "exec", "vocab-mysql", "bash", "-c", mysql_cmd],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
    )
    if result.returncode != 0:
        print(f"MySQL import error: {result.stderr}")
        # Try line-by-line fallback for large files
        print("Trying batch import with mysql client...")
        result2 = subprocess.run(
            ["docker", "exec", "vocab-mysql", "mysql",
             f"-u{DB_CONFIG['user']}", f"-p{DB_CONFIG['password']}",
             DB_CONFIG["database"],
             "-e", "source /tmp/import_wordbank.sql"],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
        )
        if result2.returncode != 0:
            print(f"Still failed: {result2.stderr}")
            sys.exit(1)

    print("  Import complete.")

    # Verify counts
    print("\n=== Verification ===")
    verify_cmd = (
        f'mysql -u{DB_CONFIG["user"]} -p{DB_CONFIG["password"]} '
        f'{DB_CONFIG["database"]} -e '
        f'"SELECT level_code, COUNT(*) as cnt FROM word_bank GROUP BY level_code ORDER BY level_code;"'
    )
    result = subprocess.run(
        ["docker", "exec", "vocab-mysql", "bash", "-c", verify_cmd],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
    )
    print(result.stdout)
    if result.stderr and "Warning" not in result.stderr:
        print(result.stderr)

    # Total
    total_cmd = (
        f'mysql -u{DB_CONFIG["user"]} -p{DB_CONFIG["password"]} '
        f'{DB_CONFIG["database"]} -e "SELECT COUNT(*) as total FROM word_bank;"'
    )
    result = subprocess.run(
        ["docker", "exec", "vocab-mysql", "bash", "-c", total_cmd],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
    )
    print(result.stdout)

    print("Done!")


if __name__ == "__main__":
    main()
