"""本地 MySQL → 云端 MySQL 数据迁移"""
import pymysql
import paramiko
import time
import os

# ── 本地 DB ──
LOCAL_DB = dict(host="127.0.0.1", port=3306, user="vocab", password="vocab123", database="vocabmaster", charset="utf8mb4")

# ── 云端 SSH ──
HOST = "60.205.145.132"
USER = "root"
PWD = "Tang@20023445"

import tempfile
DUMP_FILE = os.path.join(tempfile.gettempdir(), "vocabmaster_data.sql")

# 要迁移的表（只迁移有数据的表，跳过 user 系列表避免冲突）
TABLES = [
    "level",            # 10 rows - 等级数据
    "word_topic",       # 21 rows - 主题
    "word_bank",        # 42531 rows - 词库
]

def escape_val(v):
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "1" if v else "0"
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, bytes):
        return "0x" + v.hex()
    # string
    s = str(v).replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r")
    return f"'{s}'"

def dump_table(cur, table):
    """导出单表为 SQL INSERT"""
    cur.execute(f"SHOW COLUMNS FROM `{table}`")
    cols = [r[0] for r in cur.fetchall()]
    col_list = ", ".join(f"`{c}`" for c in cols)

    cur.execute(f"SELECT * FROM `{table}`")
    rows = cur.fetchall()

    lines = [f"\n-- Table: {table} ({len(rows)} rows)"]
    lines.append(f"DELETE FROM `{table}`;")

    # 批量 INSERT，每 500 行一条
    batch_size = 500
    for i in range(0, len(rows), batch_size):
        batch = rows[i:i+batch_size]
        values = []
        for row in batch:
            vals = ", ".join(escape_val(v) for v in row)
            values.append(f"({vals})")
        lines.append(f"INSERT INTO `{table}` ({col_list}) VALUES")
        lines.append(",\n".join(values) + ";")

    return "\n".join(lines), len(rows)

def main():
    print("=== VocabMaster 数据迁移 ===")

    # 1. 导出本地数据
    print("\n[1/3] 导出本地数据...")
    conn = pymysql.connect(**LOCAL_DB)
    cur = conn.cursor()

    sql_parts = [
        "SET NAMES utf8mb4;",
        "SET FOREIGN_KEY_CHECKS=0;",
    ]
    total_rows = 0
    for table in TABLES:
        sql, cnt = dump_table(cur, table)
        sql_parts.append(sql)
        total_rows += cnt
        print(f"  {table}: {cnt} rows")
    sql_parts.append("SET FOREIGN_KEY_CHECKS=1;")

    dump_sql = "\n".join(sql_parts)
    conn.close()

    # 写临时文件
    with open(DUMP_FILE, "w", encoding="utf-8") as f:
        f.write(dump_sql)
    dump_size = len(dump_sql) / 1024 / 1024
    print(f"  导出完成: {total_rows} rows, {dump_size:.1f}MB")

    # 2. 上传到云端
    print("\n[2/3] 上传到云端...")
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, password=PWD, timeout=10)

    from scp import SCPClient
    with SCPClient(ssh.get_transport()) as scp:
        scp.put(DUMP_FILE, DUMP_FILE)
    print(f"  已上传 {DUMP_FILE}")

    # 3. 导入到云端 MySQL
    print("\n[3/3] 导入云端 MySQL...")
    # 复制进 MySQL 容器
    stdin, stdout, stderr = ssh.exec_command(f"docker cp {DUMP_FILE} vocab-mysql:/tmp/dump.sql")
    stdout.channel.recv_exit_status()
    print("  已复制进容器")

    # 执行导入
    stdin, stdout, stderr = ssh.exec_command(
        "docker exec vocab-mysql mysql -uvocab -pvocab_prod_2024 vocabmaster -e 'source /tmp/dump.sql' 2>&1"
    )
    out = stdout.read().decode().strip()
    err = stderr.read().decode().strip()
    if out:
        print(f"  {out}")
    if err and "Warning" not in err:
        print(f"  [stderr] {err}")

    # 验证
    print("\n=== 验证 ===")
    for table in TABLES:
        stdin, stdout, stderr = ssh.exec_command(
            f"docker exec vocab-mysql mysql -uvocab -pvocab_prod_2024 vocabmaster -N -e 'SELECT COUNT(*) FROM {table}' 2>/dev/null"
        )
        cnt = stdout.read().decode().strip()
        print(f"  {table}: {cnt} rows")

    # 清理
    ssh.exec_command(f"rm -f {DUMP_FILE}")
    ssh.exec_command("docker exec vocab-mysql rm -f /tmp/dump.sql")

    ssh.close()
    print("\n迁移完成!")

if __name__ == "__main__":
    main()
