"""云端清理 orphan 容器 + 重新 up + 验证（数据卷保留，DB 用户/study 数据不动）。"""
import paramiko, time, sys

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
        for ln in err.splitlines()[:10]:
            print("  [err]", ln, flush=True)
    if check and rc != 0:
        raise Exception(f"fail exit {rc}: {cmd}")
    return out, rc

def main():
    ssh = paramiko.SSHClient(); ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, password=PWD, timeout=10)
    print("=== 清理 orphan vocab 容器（卷保留）===", flush=True)
    run(ssh, "docker rm -f vocab-autoheal vocab-nginx vocab-frontend-web vocab-wordmate-web vocab-certbot 2>/dev/null; true")
    run(ssh, "docker rm -f vocab-mysql vocab-redis vocab-backend 2>/dev/null; true")
    print("=== 数据卷仍在？===", flush=True)
    run(ssh, "docker volume ls | grep vocabmaster")
    print("=== docker compose up -d ===", flush=True)
    run(ssh, f"cd {REMOTE} && docker compose up -d 2>&1", check=True)
    print("=== 等健康（最多 200s）===", flush=True)
    ready = False
    for i in range(40):
        time.sleep(5)
        out, _ = run(ssh, "curl -sf http://localhost:8080/api/v1/actuator/health 2>/dev/null || echo NOT_READY")
        if out and "NOT_READY" not in out:
            print(f"  后端就绪 ({(i+1)*5}s)", flush=True); ready = True; break
    if not ready:
        print("  [WARN] 后端未就绪，看日志", flush=True)
    print("=== ps ===", flush=True)
    run(ssh, f"cd {REMOTE} && docker compose ps")
    print("=== 验证数据保留 + flyway + 词库端点 ===", flush=True)
    run(ssh, 'docker exec vocab-mysql mysql -uroot -pvocab_root_2024 vocabmaster -e "SELECT COUNT(*) AS users FROM user; SELECT installed_rank,version,success FROM flyway_schema_history ORDER BY installed_rank;" 2>/dev/null')
    out, _ = run(ssh, 'docker exec vocab-mysql mysql -uroot -pvocab_root_2024 vocabmaster -N -e "SELECT COUNT(*) FROM word_bank WHERE level_code LIKE \'THINK_%\';" 2>/dev/null')
    print(f"  云端 THINK 词数（Phase2 前，应为 0）: {out.strip()}", flush=True)
    run(ssh, "curl -sf http://localhost:8080/api/v1/word-lists -o /dev/null -w 'word-lists HTTP %{http_code}\\n' 2>/dev/null || echo 'word-lists 401(expected, needs auth)'")
    ssh.close()
    print("=== 完成 ===", flush=True)

if __name__ == "__main__":
    main()
