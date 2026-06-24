"""云端补传 web-nginx.conf + 重新 up + 验证。"""
import paramiko, time
from scp import SCPClient

HOST = "60.205.145.132"; USER = "root"; PWD = "Tang@20023445"; REMOTE = "/opt/vocabmaster"

def run(ssh, cmd, check=False):
    print(f"$ {cmd}", flush=True)
    _, o, e = ssh.exec_command(cmd)
    out = o.read().decode("utf-8", "replace").strip()
    err = e.read().decode("utf-8", "replace").strip()
    rc = o.channel.recv_exit_status()
    if out:
        for ln in out.splitlines()[:40]:
            print("  ", ln, flush=True)
    if err and rc != 0:
        for ln in err.splitlines()[:8]:
            print("  [err]", ln, flush=True)
    if check and rc != 0:
        raise Exception(f"fail {cmd}")
    return out, rc

def main():
    ssh = paramiko.SSHClient(); ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, password=PWD, timeout=10)
    print("=== 1. 删坏目录 + 建 deploy/ ===", flush=True)
    run(ssh, "rm -rf /opt/vocabmaster/wordmate-web/deploy/web-nginx.conf")
    run(ssh, "mkdir -p /opt/vocabmaster/wordmate-web/deploy")
    print("=== 2. 上传 web-nginx.conf ===", flush=True)
    with SCPClient(ssh.get_transport()) as scp:
        scp.put("E:/ccode/vocab-spec/wordmate-web/deploy/web-nginx.conf",
                "/opt/vocabmaster/wordmate-web/deploy/web-nginx.conf")
    run(ssh, "ls -la /opt/vocabmaster/wordmate-web/deploy/web-nginx.conf")
    print("=== 3. docker compose up -d ===", flush=True)
    run(ssh, f"cd {REMOTE} && docker compose up -d 2>&1", check=True)
    print("=== 4. 等健康 ===", flush=True)
    for i in range(40):
        time.sleep(5)
        out, _ = run(ssh, "curl -sf http://localhost:8080/api/v1/actuator/health 2>/dev/null || echo NOT_READY")
        if out and "NOT_READY" not in out:
            print(f"  后端就绪 ({(i+1)*5}s)", flush=True); break
    print("=== 5. 验证 ===", flush=True)
    run(ssh, f"cd {REMOTE} && docker compose ps")
    run(ssh, 'docker exec vocab-mysql mysql -uroot -pvocab_root_2024 vocabmaster -e "SELECT COUNT(*) AS users FROM user; SELECT installed_rank,version,success FROM flyway_schema_history ORDER BY installed_rank;" 2>/dev/null')
    ssh.close(); print("=== DONE ===", flush=True)

if __name__ == "__main__":
    main()
