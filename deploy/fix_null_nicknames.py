"""给 nickname 为空的存量用户补默认昵称 '词友'+uuid前4（对齐新用户逻辑）。

场景：删 getUserInfo 前微信登录过的老用户 nickname=null，mine 页显示"用户"。
读 deploy/.deploy.env 凭据。SQL 走 stdin。
"""
import os
import sys

import paramiko


def _load_env(path):
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
try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

HOST = os.environ.get("DEPLOY_HOST", "60.205.145.132")
USER = os.environ.get("DEPLOY_USER", "root")
PWD = os.environ.get("DEPLOY_PWD")
DB_ROOT = os.environ.get("DB_ROOT_PASSWORD")
DB_NAME = os.environ.get("DB_NAME", "vocabmaster")
if not PWD or not DB_ROOT:
    sys.exit("缺 DEPLOY_PWD / DB_ROOT_PASSWORD")


def run_sql(ssh, sql):
    cmd = f"docker exec -i vocab-mysql mysql -uroot -p{DB_ROOT} -N {DB_NAME}"
    print(f"$ mysql> {sql}")
    stdin, stdout, stderr = ssh.exec_command(cmd)
    stdin.write(sql + "\n")
    stdin.channel.shutdown_write()
    out = stdout.read().decode().strip()
    rc = stdout.channel.recv_exit_status()
    if out:
        print(out)
    return out


def main():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, password=PWD, timeout=10)
    print(f"已连接 {HOST}\n")

    print("== 修复前：nickname 为空的用户 ==")
    run_sql(ssh, "SELECT id, uuid, nickname, email FROM user WHERE nickname IS NULL OR nickname='';")

    print("\n== 执行 UPDATE ==")
    run_sql(ssh, "UPDATE user SET nickname=CONCAT('词友', LEFT(uuid,4)) WHERE nickname IS NULL OR nickname='';")

    print("\n== 修复后：这些用户现在的 nickname ==")
    run_sql(ssh, "SELECT id, nickname, email FROM user WHERE nickname LIKE '词友%';")

    ssh.close()
    print("\n完成。重新登录小程序即可看到新昵称。")


if __name__ == "__main__":
    main()
