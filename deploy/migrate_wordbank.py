"""迁移 word_bank 到云端"""
import pymysql, paramiko, os, tempfile
from scp import SCPClient

LOCAL_DB = dict(host="127.0.0.1", port=3306, user="vocab", password="vocab123",
                database="vocabmaster", charset="utf8mb4")

def escape_val(v):
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "1" if v else "0"
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, bytes):
        return "0x" + v.hex()
    s = str(v).replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
    return "'" + s + "'"

# 导出
print("Exporting word_bank...")
conn = pymysql.connect(**LOCAL_DB)
cur = conn.cursor()
cur.execute("SHOW COLUMNS FROM word_bank")
cols = [r[0] for r in cur.fetchall()]
col_list = ", ".join("`" + c + "`" for c in cols)

cur.execute("SELECT * FROM word_bank ORDER BY id")
rows = cur.fetchall()
print(f"  {len(rows)} rows")

tmp = os.path.join(tempfile.gettempdir(), "word_bank.sql")
with open(tmp, "w", encoding="utf-8") as f:
    f.write("SET NAMES utf8mb4;\nSET FOREIGN_KEY_CHECKS=0;\n")
    batch = 200
    for i in range(0, len(rows), batch):
        b = rows[i:i+batch]
        vals = []
        for row in b:
            vals.append("(" + ", ".join(escape_val(v) for v in row) + ")")
        f.write("INSERT INTO word_bank (" + col_list + ") VALUES\n")
        f.write(",\n".join(vals) + ";\n")
    f.write("SET FOREIGN_KEY_CHECKS=1;\n")
conn.close()
print(f"  SQL file: {os.path.getsize(tmp)/1024/1024:.1f}MB")

# 上传
print("Uploading to cloud...")
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect("60.205.145.132", username="root", password="Tang@20023445", timeout=10)

with SCPClient(ssh.get_transport()) as scp:
    scp.put(tmp, "/tmp/word_bank.sql")
print("  Uploaded")

# 复制进容器
stdin, stdout, stderr = ssh.exec_command("docker cp /tmp/word_bank.sql vocab-mysql:/tmp/word_bank.sql")
stdout.channel.recv_exit_status()

# 验证文件
stdin, stdout, stderr = ssh.exec_command("docker exec vocab-mysql ls -lh /tmp/word_bank.sql")
print("  " + stdout.read().decode().strip())

# 导入
print("Importing...")
stdin, stdout, stderr = ssh.exec_command(
    'docker exec vocab-mysql bash -c "mysql -uvocab -pvocab_prod_2024 vocabmaster < /tmp/word_bank.sql" 2>&1'
)
out = stdout.read().decode().strip()
if out:
    print("  " + out)

# 验证
stdin, stdout, stderr = ssh.exec_command(
    'docker exec vocab-mysql mysql -uvocab -pvocab_prod_2024 vocabmaster -N -e "SELECT COUNT(*) FROM word_bank" 2>/dev/null'
)
cnt = stdout.read().decode().strip()
print(f"\nword_bank on cloud: {cnt} rows")

ssh.close()
print("Done!")
