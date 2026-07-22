"""
只读探活：dump 服务器上 nginx 活配置 + 现有 ssl 目录 + 端口监听
不改任何东西。
"""
import paramiko
from remote_deploy import HOST, USER, PWD, REMOTE_DIR  # 复用凭据，不在此硬编码

def run(ssh, cmd):
    _, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode("utf-8", "replace").strip()
    err = stderr.read().decode("utf-8", "replace").strip()
    return out, err

def main():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, password=PWD, timeout=10)
    print(f"connected {HOST}\n")

    print("=== 容器状态 ===")
    o, _ = run(ssh, "docker ps --format '{{.Names}}\t{{.Status}}\t{{.Ports}}' | grep -E 'nginx|web'")
    print(o)

    print("\n=== nginx -T (active config, 仅 server/listen/ssl/location 行) ===")
    o, e = run(ssh, f"docker exec vocab-nginx nginx -T 2>&1 | grep -nE 'listen|server_name|ssl_certificate|location |proxy_pass|include.*conf|root |try_files' ")
    print(o or "(空)")

    print("\n=== conf.d 是否被 include? ===")
    o, _ = run(ssh, f"docker exec vocab-nginx nginx -T 2>&1 | grep -nE 'include.*conf.d|include.*mime'")
    print(o or "(无 conf.d include → conf.d/vocabmaster.conf 未生效)")

    print("\n=== 现有 ssl 目录 ===")
    o, _ = run(ssh, f"ls -la {REMOTE_DIR}/deploy/nginx/ssl/ 2>&1")
    print(o)

    print("\n=== 80/443 监听 ===")
    o, _ = run(ssh, "ss -tlnp 2>/dev/null | grep -E ':80 |:443 ' || docker exec vocab-nginx sh -c 'netstat -tlnp 2>/dev/null || true'")
    print(o)

    ssh.close()

if __name__ == "__main__":
    main()
