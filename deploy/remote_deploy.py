"""
VocabMaster 远程部署脚本
本地编译 → 上传 → 远程 Docker 启动
"""
import paramiko
import os
import sys
import time
from scp import SCPClient

HOST = "60.205.145.132"
USER = "root"
PWD = "Tang@20023445"
REMOTE_DIR = "/opt/vocabmaster"

def ssh_exec(ssh, cmd, check=True):
    """执行远程命令，打印输出"""
    print(f"  $ {cmd}")
    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode().strip()
    err = stderr.read().decode().strip()
    exit_code = stdout.channel.recv_exit_status()
    if out:
        for line in out.split('\n'):
            print(f"    {line}")
    if err and exit_code != 0:
        for line in err.split('\n'):
            print(f"    [stderr] {line}")
    if check and exit_code != 0:
        raise Exception(f"Command failed (exit {exit_code}): {cmd}")
    return out, err, exit_code

def main():
    print("=" * 50)
    print("  VocabMaster 远程部署")
    print("=" * 50)

    # ── 1. 连接 SSH ──
    print("\n[1/7] 连接服务器...")
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, password=PWD, timeout=10)
    print(f"  已连接 {HOST}")

    # ── 2. 创建目录结构 ──
    print("\n[2/7] 创建目录...")
    dirs = [
        REMOTE_DIR,
        f"{REMOTE_DIR}/backend-java/target",
        f"{REMOTE_DIR}/wordmate-web/dist",
        f"{REMOTE_DIR}/deploy/nginx/conf.d",
        f"{REMOTE_DIR}/deploy/nginx/ssl",
        f"{REMOTE_DIR}/deploy/certbot/webroot",
        f"{REMOTE_DIR}/sql",
    ]
    ssh_exec(ssh, f"mkdir -p {' '.join(dirs)}")

    # ── 3. 上传文件 ──
    print("\n[3/7] 上传文件...")
    local_base = os.path.dirname(os.path.abspath(__file__))
    project_base = os.path.dirname(local_base)

    uploads = [
        (f"{project_base}/backend-java/target/vocabmaster-backend.jar", f"{REMOTE_DIR}/backend-java/target/vocabmaster-backend.jar"),
        (f"{project_base}/backend-java/Dockerfile", f"{REMOTE_DIR}/backend-java/Dockerfile"),
        (f"{project_base}/docker-compose.yml", f"{REMOTE_DIR}/docker-compose.yml"),
        (f"{project_base}/deploy/nginx/nginx.conf", f"{REMOTE_DIR}/deploy/nginx/nginx.conf"),
        (f"{project_base}/deploy/nginx/conf.d/vocabmaster.conf", f"{REMOTE_DIR}/deploy/nginx/conf.d/vocabmaster.conf"),
        (f"{project_base}/deploy/.env.example", f"{REMOTE_DIR}/.env"),
    ]

    # 检查 SQL 文件
    sql_file = f"{project_base}/sql/init.sql"
    if os.path.exists(sql_file):
        uploads.append((sql_file, f"{REMOTE_DIR}/sql/init.sql"))

    # 检查前端 dist
    dist_dir = f"{project_base}/wordmate-web/dist"
    if os.path.exists(dist_dir) and os.listdir(dist_dir):
        print("  上传前端 dist/...")
        with SCPClient(ssh.get_transport()) as scp:
            for item in uploads:
                local_path = item[0]
                remote_path = item[1]
                if os.path.exists(local_path):
                    size_mb = os.path.getsize(local_path) / 1024 / 1024
                    print(f"  {os.path.basename(local_path)} ({size_mb:.1f}MB)")
                    scp.put(local_path, remote_path)
            # 上传前端 dist 目录
            if os.path.exists(dist_dir):
                scp.put(dist_dir, f"{REMOTE_DIR}/wordmate-web/", recursive=True)
    else:
        print("  前端 dist 不存在，跳过前端上传")
        with SCPClient(ssh.get_transport()) as scp:
            for local_path, remote_path in uploads:
                if os.path.exists(local_path):
                    size_mb = os.path.getsize(local_path) / 1024 / 1024
                    print(f"  {os.path.basename(local_path)} ({size_mb:.1f}MB)")
                    scp.put(local_path, remote_path)

    # ── 4. 配置 .env ──
    print("\n[4/7] 配置环境变量...")
    env_content = """# VocabMaster 生产环境
DB_ROOT_PASSWORD=vocab_root_2024
DB_NAME=vocabmaster
DB_USERNAME=vocab
DB_PASSWORD=vocab_prod_2024
REDIS_PASSWORD=redis_prod_2024
JWT_SECRET=vocabmaster-jwt-secret-prod-2024-at-least-32-characters
AES_KEY=ZGV2LWFlcy1rZXktMzJieXRlcy1sb25n
WECHAT_APP_ID=
WECHAT_APP_SECRET=
"""
    # 写入 .env
    ssh_exec(ssh, f"cat > {REMOTE_DIR}/.env << 'ENVEOF'\n{env_content}ENVEOF")

    # ── 5. 调整 docker-compose 内存限制（1.6G RAM）──
    print("\n[5/7] 调整内存限制（1.6G RAM）...")
    # MySQL 256M, Redis 64M, Java 384M, Nginx 48M = 752M, 剩余 ~850M 给系统
    adjustments = [
        f"sed -i 's/memory: 400M/memory: 256M/' {REMOTE_DIR}/docker-compose.yml",
        f"sed -i 's/memory: 180M/memory: 96M/' {REMOTE_DIR}/docker-compose.yml",
        f"sed -i 's/memory: 500M/memory: 420M/' {REMOTE_DIR}/docker-compose.yml",
        f"sed -i 's/memory: 64M/memory: 48M/' {REMOTE_DIR}/docker-compose.yml",
        f"sed -i 's/maxmemory 128mb/maxmemory 64mb/' {REMOTE_DIR}/docker-compose.yml",
        f"sed -i 's/innodb-buffer-pool-size=256M/innodb-buffer-pool-size=128M/' {REMOTE_DIR}/docker-compose.yml",
    ]
    for cmd in adjustments:
        ssh_exec(ssh, cmd)

    # ── 6. Docker 构建 + 启动 ──
    print("\n[6/7] Docker 构建并启动...")
    # 注意：绝不加 -v！-v 会删除数据卷清空 MySQL 增量数据。只停容器，保留 mysql_data/redis_data。
    ssh_exec(ssh, f"cd {REMOTE_DIR} && docker compose down 2>/dev/null; true")
    ssh_exec(ssh, f"cd {REMOTE_DIR} && docker compose build backend-java 2>&1", check=False)
    print("  启动所有服务...")
    ssh_exec(ssh, f"cd {REMOTE_DIR} && docker compose up -d 2>&1")

    # ── 7. 等待健康检查 ──
    print("\n[7/7] 等待服务就绪...")
    for i in range(40):
        time.sleep(5)
        out, _, _ = ssh_exec(ssh, "curl -sf http://localhost:8080/api/v1/actuator/health 2>/dev/null || echo NOT_READY", check=False)
        if "NOT_READY" not in out:
            print(f"\n  后端就绪! ({(i+1)*5}s)")
            break
        print(f"  等待中... {(i+1)*5}s", end='\r')
    else:
        print("\n  警告: 后端未在 200s 内就绪，请检查日志")

    # 显示状态
    print("\n" + "=" * 50)
    print("  部署结果")
    print("=" * 50)
    ssh_exec(ssh, f"cd {REMOTE_DIR} && docker compose ps")
    print()
    print(f"  Web 前端:  http://{HOST}")
    print(f"  API:       http://{HOST}/api/v1")
    print(f"  Swagger:   http://{HOST}/swagger-ui/")
    print(f"  健康检查:  http://{HOST}/api/v1/actuator/health")
    print()

    ssh.close()
    print("完成!")

if __name__ == "__main__":
    main()
