"""定向重建 prod backend-java 容器（仅 env 改动用）。

不碰 MySQL/Redis/nginx/web，不 down 整栈，不 -v（保数据卷）。
读 deploy/.deploy.env 凭据 + WECHAT_APP_ID/SECRET，patch prod .env 后
`docker compose up -d --force-recreate backend-java`（restart 不重读 .env，必须 recreate）。

用法：cd deploy && python redeploy_backend.py
"""
import os
import sys
import time

import paramiko


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
REMOTE_DIR = os.environ.get("DEPLOY_REMOTE_DIR", "/opt/vocabmaster")
PWD = os.environ.get("DEPLOY_PWD")
if not PWD:
    sys.exit("缺 DEPLOY_PWD：请在 deploy/.deploy.env 填写")

APP_ID = os.environ.get("WECHAT_APP_ID")
APP_SECRET = os.environ.get("WECHAT_APP_SECRET")
if not APP_ID or not APP_SECRET:
    sys.exit("缺 WECHAT_APP_ID/SECRET：请在 deploy/.deploy.env 填写")


def run(ssh, cmd, check=True):
    print(f"$ {cmd}")
    _, o, e = ssh.exec_command(cmd)
    out = o.read().decode().strip()
    err = e.read().decode().strip()
    rc = o.channel.recv_exit_status()
    if out:
        print(out)
    if err and rc != 0:
        print(f"[stderr] {err}")
    if check and rc != 0:
        raise SystemExit(f"命令失败 exit {rc}: {cmd}")
    return out


def main():
    print("=" * 50)
    print("  定向重建 backend-java（env-only）")
    print("=" * 50)

    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, password=PWD, timeout=10)
    print(f"已连接 {HOST}")

    # 1. patch prod .env 的 WECHAT 两行（幂等：覆盖已有值）
    print("\n[1/3] 更新 prod .env WECHAT 配置...")
    run(ssh, f"sed -i 's|^WECHAT_APP_ID=.*|WECHAT_APP_ID={APP_ID}|' {REMOTE_DIR}/.env")
    run(ssh, f"sed -i 's|^WECHAT_APP_SECRET=.*|WECHAT_APP_SECRET={APP_SECRET}|' {REMOTE_DIR}/.env")
    # 校验：只打印键名，值脱敏
    run(ssh, f"grep -E '^WECHAT_APP' {REMOTE_DIR}/.env | sed 's/=.*/=***(已设)/'")

    # 2. 只重建 backend-java（recreate 才重读 .env；其它服务不动）
    print("\n[2/3] 重建 backend-java 容器...")
    run(ssh, f"cd {REMOTE_DIR} && docker compose up -d --force-recreate backend-java 2>&1")

    # 3. 健康检查
    print("\n[3/3] 等待后端就绪...")
    for i in range(24):
        time.sleep(5)
        out = run(ssh, "curl -sf http://localhost:8080/api/v1/actuator/health 2>/dev/null || echo NOT_READY", check=False)
        if "NOT_READY" not in out and out:
            print(f"\n后端就绪 ({(i + 1) * 5}s)")
            break
        print(f"  等待... {(i + 1) * 5}s", end="\r")
    else:
        print("\n警告：后端 120s 内未就绪，查日志：docker logs vocab-backend --tail 50")

    ssh.close()
    print("\n完成。体验版重试微信一键登录。")


if __name__ == "__main__":
    main()
