"""上传新 dist 到云端 + 重启 web 容器（前端含 TestEntry 修复）。"""
import paramiko
from scp import SCPClient

HOST = "60.205.145.132"; USER = "root"; PWD = "Tang@20023445"; REMOTE = "/opt/vocabmaster"

def run(ssh, cmd):
    print(f"$ {cmd}", flush=True)
    _, o, e = ssh.exec_command(cmd)
    out = o.read().decode("utf-8", "replace").strip()
    err = e.read().decode("utf-8", "replace").strip()
    if out:
        for ln in out.splitlines()[:15]:
            print("  ", ln, flush=True)
    if err:
        print("  [err]", err[:200], flush=True)

ssh = paramiko.SSHClient(); ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(HOST, username=USER, password=PWD, timeout=10)
print("上传 dist ...", flush=True)
# 清旧 dist 再传（避免残留旧 chunk hash 文件）
run(ssh, f"rm -rf {REMOTE}/wordmate-web/dist && mkdir -p {REMOTE}/wordmate-web/dist")
with SCPClient(ssh.get_transport()) as scp:
    scp.put("E:/ccode/vocab-spec/wordmate-web/dist", f"{REMOTE}/wordmate-web/", recursive=True)
run(ssh, f"ls {REMOTE}/wordmate-web/dist | head -5")
print("重启 web 容器 ...", flush=True)
run(ssh, f"cd {REMOTE} && docker compose --profile prod restart wordmate-web nginx 2>&1")
import time; time.sleep(4)
print("验证公网 ...", flush=True)
run(ssh, "curl -sf -o /dev/null -w 'home HTTP %{http_code}\\n' http://localhost/")
run(ssh, "curl -s http://localhost/ | grep -o 'src=\"[^\"]*\\.js\"' | head -1")
ssh.close(); print("DONE", flush=True)
