"""重置 prod tester 账号密码 + 写本地凭据 txt（提审用）。

密码 DB 里是 bcrypt(12) 单向哈希，无法反查明文 → 只能重置成已知值。
SQL 经 stdin 注入 mysql 容器，避开 bcrypt hash 里 `$` 的 shell 展开坑。

读 deploy/.deploy.env（SSH + DB 凭据）。
用法：cd deploy && python reset_tester_pwd.py [新密码]
"""
import os
import sys
import json
import urllib.request

import bcrypt
import paramiko

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

EMAIL = "tester@vocab-master.cn"
DEFAULT_PWD = "VocabMaster2026"
CREDS_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "tester-credentials.txt")


def _load_env(path: str) -> None:
    if not os.path.exists(path):
        return
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            os.environ.setdefault(k.strip(), v.strip().strip('"').strip("'"))


_HERE = os.path.dirname(os.path.abspath(__file__))
_load_env(os.path.join(_HERE, ".deploy.env"))

HOST = os.environ.get("DEPLOY_HOST", "60.205.145.132")
USER = os.environ.get("DEPLOY_USER", "root")
PWD = os.environ.get("DEPLOY_PWD")
DB_ROOT = os.environ.get("DB_ROOT_PASSWORD")
DB_NAME = os.environ.get("DB_NAME", "vocabmaster")
NEW_PWD = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_PWD

if not PWD or not DB_ROOT:
    sys.exit("缺 DEPLOY_PWD / DB_ROOT_PASSWORD：检查 deploy/.deploy.env")


def run_sql(ssh, sql: str) -> str:
    """经 stdin 注入 SQL，避开 shell 对 bcrypt hash 中 $ 的展开。"""
    cmd = f"docker exec -i vocab-mysql mysql -uroot -p{DB_ROOT} -N {DB_NAME}"
    print(f"$ docker exec -i vocab-mysql mysql -uroot -p*** -N {DB_NAME}")
    stdin, stdout, stderr = ssh.exec_command(cmd)
    stdin.write(sql + "\n")
    stdin.channel.shutdown_write()
    out = stdout.read().decode().strip()
    err = stderr.read().decode().strip()
    rc = stdout.channel.recv_exit_status()
    if out:
        print(out)
    if rc != 0:
        # mysql 把密码警告写 stderr 但 rc 可能仍 0；这里只在 rc!=0 报错
        if err:
            print(f"[stderr] {err}")
        raise SystemExit(f"SQL 失败 exit {rc}")
    return out


def main():
    print("=" * 50)
    print(f"  重置 tester 密码 → {EMAIL}")
    print("=" * 50)

    # 1. 生成 bcrypt(12) 哈希
    hashed = bcrypt.hashpw(NEW_PWD.encode(), bcrypt.gensalt(rounds=12)).decode()
    print(f"bcrypt(12) hash 生成 ✓  (前缀 {hashed[:7]}...)")

    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, password=PWD, timeout=10)
    print(f"已连接 {HOST}")

    # 0. 先列出现有用户（phone 是 AES 密文，用 *** 占位避免二进制乱码）
    print("\n[0] 现有用户：")
    run_sql(ssh, "SELECT id, email, IF(phone IS NULL,'(无)','***'), status, created_at FROM user ORDER BY id;")

    # 2. UPDATE（参数化值已无单引号外的特殊字符；走 stdin）
    print("\n[1/3] 更新 password_hash...")
    run_sql(ssh, f"UPDATE user SET password_hash='{hashed}' WHERE email='{EMAIL}';")

    # 3. 校验行存在 + status
    print("\n[2/3] 校验账号...")
    run_sql(ssh, f"SELECT id, email, status FROM user WHERE email='{EMAIL}';")

    ssh.close()

    # 4. 真实登录验证（打 prod API）
    print("\n[3/3] 登录验证（prod API）...")
    payload = json.dumps({"type": "email", "identifier": EMAIL, "password": NEW_PWD}).encode()
    req = urllib.request.Request(
        "https://vocab-master.cn/api/v1/auth/login",
        data=payload,
        headers={"Content-Type": "application/json"},
    )
    try:
        resp = urllib.request.urlopen(req, timeout=15)
        body = resp.read().decode()
        ok = '"access_token"' in body or '"token"' in body or resp.status == 200
        print(f"登录 {'OK' if ok else '?'} (HTTP {resp.status})")
    except Exception as e:
        print(f"登录验证失败：{e}")

    # 5. 写凭据 txt
    with open(CREDS_FILE, "w", encoding="utf-8") as f:
        f.write(f"# VocabMaster 提审测试账号（prod）\n")
        f.write(f"# 生成于本地，勿提交 git，提审后可删\n")
        f.write(f"email={EMAIL}\n")
        f.write(f"password={NEW_PWD}\n")
        f.write(f"login=登录页 → 邮箱登录 tab → 填上述邮箱+密码\n")
    print(f"\n凭据已写：{CREDS_FILE}")
    print("（确保该文件 gitignored；提审后建议删除）")


if __name__ == "__main__":
    main()
