"""重建 prod backend-java（代码或 env 改动）。

scp 新 jar → docker compose build backend-java → up --force-recreate。
不碰 MySQL/Redis/nginx/web，不 -v（保数据卷）。
读 deploy/.deploy.env 凭据。

用法：
  1. cd backend-java && mvn package -DskipTests   # 产出 target/*.jar
  2. cd deploy && python redeploy_backend.py
"""
import os
import sys
import time

import paramiko
from scp import SCPClient


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
try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

HOST = os.environ.get("DEPLOY_HOST", "60.205.145.132")
USER = os.environ.get("DEPLOY_USER", "root")
REMOTE_DIR = os.environ.get("DEPLOY_REMOTE_DIR", "/opt/vocabmaster")
PWD = os.environ.get("DEPLOY_PWD")
if not PWD:
    sys.exit("缺 DEPLOY_PWD：请在 deploy/.deploy.env 填写")

PROJECT_BASE = os.path.dirname(_HERE)
JAR_LOCAL = os.path.join(PROJECT_BASE, "backend-java", "target", "vocabmaster-backend.jar")
DOCKERFILE_LOCAL = os.path.join(PROJECT_BASE, "backend-java", "Dockerfile")


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
    if not os.path.exists(JAR_LOCAL):
        sys.exit(f"jar 不存在：{JAR_LOCAL}\n先 cd backend-java && mvn package -DskipTests")

    print("=" * 50)
    print("  重建 backend-java（代码+env）")
    print("=" * 50)

    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, password=PWD, timeout=10)
    print(f"已连接 {HOST}")

    # 1. scp jar + Dockerfile
    print("\n[1/4] 上传 jar + Dockerfile...")
    with SCPClient(ssh.get_transport()) as scp:
        scp.put(JAR_LOCAL, f"{REMOTE_DIR}/backend-java/target/vocabmaster-backend.jar")
        scp.put(DOCKERFILE_LOCAL, f"{REMOTE_DIR}/backend-java/Dockerfile")
    print(f"  {os.path.basename(JAR_LOCAL)} ({os.path.getsize(JAR_LOCAL) / 1024 / 1024:.1f}MB)")

    # 2. build backend image（Dockerfile COPY jar）
    print("\n[2/4] 构建后端镜像...")
    run(ssh, f"cd {REMOTE_DIR} && docker compose build backend-java 2>&1", check=False)

    # 3. recreate backend（recreate 才吃到新 jar；MySQL/Redis/nginx/web 不动）
    print("\n[3/4] 重建 backend-java 容器...")
    run(ssh, f"cd {REMOTE_DIR} && docker compose up -d --force-recreate backend-java 2>&1")

    # 4. health check
    print("\n[4/4] 等待后端就绪...")
    for i in range(24):
        time.sleep(5)
        out = run(ssh, "curl -sf http://localhost:8080/api/v1/actuator/health 2>/dev/null || echo NOT_READY", check=False)
        if "NOT_READY" not in out and out:
            print(f"\n后端就绪 ({(i + 1) * 5}s)")
            break
        print(f"  等待... {(i + 1) * 5}s", end="\r")
    else:
        print("\n警告：后端 120s 未就绪，查日志：docker logs vocab-backend --tail 50")

    ssh.close()
    print("\n完成。")


if __name__ == "__main__":
    main()
