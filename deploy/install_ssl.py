"""
安装阿里云 DV 证书到生产 nginx。
- 上传 pem/key + 新 vocabmaster.conf
- 备份旧 conf
- nginx -t 校验，失败则中止（不 reload），回滚 conf
- 通过才 reload
- --resolve 绕 DNS 验 HTTPS + API
"""
import paramiko
from scp import SCPClient
from remote_deploy import HOST, USER, PWD, REMOTE_DIR
import os, sys, time

CERT_DIR = r"E:\ccode\vocab-spec\26225047_vocab-master.cn_nginx"
PEM = os.path.join(CERT_DIR, "vocab-master.cn.pem")
KEY = os.path.join(CERT_DIR, "vocab-master.cn.key")
CONF_LOCAL = os.path.join(os.path.dirname(__file__), "nginx", "conf.d", "vocabmaster.conf")

SSL_REMOTE = f"{REMOTE_DIR}/deploy/nginx/ssl"
CONF_REMOTE = f"{REMOTE_DIR}/deploy/nginx/conf.d/vocabmaster.conf"

def run(ssh, cmd, check=True):
    _, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode("utf-8", "replace").strip()
    err = stderr.read().decode("utf-8", "replace").strip()
    code = stdout.channel.recv_exit_status()
    print(f"  $ {cmd}")
    if out: print(f"    {out}")
    if err: print(f"    [err] {err}")
    if check and code != 0:
        raise SystemExit(f"FAIL exit {code}: {cmd}")
    return out, err, code

def main():
    for f in (PEM, KEY, CONF_LOCAL):
        if not os.path.exists(f):
            sys.exit(f"缺文件: {f}")

    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, password=PWD, timeout=10)
    print(f"connected {HOST}\n")

    # 1. 备份现 conf
    print("[1/6] 备份现 vocabmaster.conf")
    run(ssh, f"cp {CONF_REMOTE} {CONF_REMOTE}.bak.$(date +%s) 2>/dev/null; ls -t {REMOTE_DIR}/deploy/nginx/conf.d/vocabmaster.conf.bak.* 2>/dev/null | head -1")

    # 2. 上传 pem/key
    print("\n[2/6] 上传证书 pem/key")
    run(ssh, f"mkdir -p {SSL_REMOTE}")
    with SCPClient(ssh.get_transport()) as scp:
        scp.put(PEM, f"{SSL_REMOTE}/vocab-master.cn.pem")
        scp.put(KEY, f"{SSL_REMOTE}/vocab-master.cn.key")
    run(ssh, f"chmod 644 {SSL_REMOTE}/vocab-master.cn.pem && chmod 600 {SSL_REMOTE}/vocab-master.cn.key")
    run(ssh, f"ls -la {SSL_REMOTE}/")

    # 3. 上传新 conf
    print("\n[3/6] 上传新 vocabmaster.conf")
    with SCPClient(ssh.get_transport()) as scp:
        scp.put(CONF_LOCAL, CONF_REMOTE)

    # 4. nginx -t
    print("\n[4/6] nginx -t 校验（失败则回滚+中止）")
    _, _, code = run(ssh, "docker exec vocab-nginx nginx -t", check=False)
    if code != 0:
        print("!!! nginx -t 失败，回滚 conf")
        run(ssh, f"NEWEST=$(ls -t {REMOTE_DIR}/deploy/nginx/conf.d/vocabmaster.conf.bak.* | head -1) && cp $NEWEST {CONF_REMOTE}")
        sys.exit("已回滚，中止。请看上面的错误。")

    # 5. reload
    print("\n[5/6] nginx -s reload")
    run(ssh, "docker exec vocab-nginx nginx -s reload")

    # 6. 验证（绕 DNS）
    print("\n[6/6] 验证（curl --resolve 绕 DNS）")
    time.sleep(2)
    run(ssh, 'curl -sI --resolve vocab-master.cn:443:127.0.0.1 https://vocab-master.cn/ | head -5', check=False)
    run(ssh, 'curl -s --resolve vocab-master.cn:443:127.0.0.1 https://vocab-master.cn/api/v1/actuator/health', check=False)
    run(ssh, 'echo | openssl s_client -connect 127.0.0.1:443 -servername vocab-master.cn 2>/dev/null | openssl x509 -noout -subject -issuer -dates', check=False)
    run(ssh, 'curl -sI --resolve www.vocab-master.cn:443:127.0.0.1 https://www.vocab-master.cn/ | head -3', check=False)
    # 80 跳转验证
    run(ssh, 'curl -sI --resolve vocab-master.cn:80:127.0.0.1 http://vocab-master.cn/ | head -3', check=False)

    ssh.close()
    print("\nDONE")

if __name__ == "__main__":
    main()
