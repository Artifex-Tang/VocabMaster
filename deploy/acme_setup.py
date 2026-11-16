"""
acme.sh 一键装 Let's Encrypt 证书 + 自动续签（替代阿里云手动 DV 续签）。
- 服务器装 acme.sh（自带每日 cron，<30 天自动续签）
- dns_ali：用阿里云 RAM 子账号 AccessKey 自动加/删 _acme-challenge TXT 验证
- 签发 vocab-master.cn + www（--server letsencrypt；acme.sh v3 默认 ZeroSSL 需显式覆盖）
- install-cert 写现有 nginx ssl 路径，reloadcmd 接管续签后 reload
- 末尾验证证书日期 + HTTPS + cron

凭据：SSH 复用 remote_deploy.py（读 deploy/.deploy.env）；
阿里云 Key 同读 .deploy.env 的 ALIYUN_AK_ID / ALIYUN_AK_SECRET，
首次 --issue 后 acme.sh 会持久化到服务器 ~/.acme.sh/account.conf。
"""
import os
import sys
import time

import paramiko
from scp import SCPClient  # noqa: F401  # 与 install_ssl.py 同依赖，暂未用

from remote_deploy import HOST, USER, PWD, REMOTE_DIR

DOMAIN = "vocab-master.cn"
SSL_REMOTE = f"{REMOTE_DIR}/deploy/nginx/ssl"
NGINX_CONTAINER = "vocab-nginx"

ALI_KEY = os.environ.get("ALIYUN_AK_ID")
ALI_SECRET = os.environ.get("ALIYUN_AK_SECRET")
if not ALI_KEY or not ALI_SECRET:
    sys.exit("缺 ALIYUN_AK_ID / ALIYUN_AK_SECRET：请在 deploy/.deploy.env 填写")


def run(ssh, cmd, check=True):
    _, stdout, stderr = ssh.exec_command(cmd, timeout=300)
    out = stdout.read().decode("utf-8", "replace").strip()
    err = stderr.read().decode("utf-8", "replace").strip()
    code = stdout.channel.recv_exit_status()
    print(f"  $ {cmd}")
    if out:
        print(f"    {out}")
    if err:
        print(f"    [err] {err}")
    if check and code != 0:
        raise SystemExit(f"FAIL exit {code}: {cmd}")
    return out, err, code


def main():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, password=PWD, timeout=10)
    print(f"connected {HOST}\n")

    # 1. 装 acme.sh（幂等，已装则跳过；自动装 cron）
    print("[1/5] 安装 acme.sh")
    run(ssh, "test -f ~/.acme.sh/acme.sh && echo already-installed || "
             "curl -s https://get.acme.sh | sh", check=True)

    # 2. 签发（dns_ali 自动 TXT；LE 强制显式指定）
    print("\n[2/5] 签发证书（约 1-3 分钟，含 DNS 验证）")
    issue = (f"export Ali_Key='{ALI_KEY}' Ali_Secret='{ALI_SECRET}'; "
             f"~/.acme.sh/acme.sh --issue --dns dns_ali --server letsencrypt "
             f"-d {DOMAIN} -d www.{DOMAIN}")
    _, _, code = run(ssh, issue, check=False)
    already = False
    if code != 0:
        # 已有在有效期内且同域名的证书 → --issue 报 Skip，视为成功
        run(ssh, f"ls ~/.acme.sh/{DOMAIN}_ecc/", check=False)
        out, _, _ = run(ssh, f"~/.acme.sh/acme.sh --list | grep {DOMAIN}", check=False)
        already = bool(out)
        if not already:
            sys.exit(f"签发失败 exit {code}，看上面错误（多为 RAM 无 DNS 权限）")
        print("    已存在有效证书，跳过签发继续部署")

    # 3. 部署到现有 nginx ssl 路径 + reload 接管
    print("\n[3/5] install-cert → nginx ssl 目录")
    run(ssh,
        f"~/.acme.sh/acme.sh --install-cert -d {DOMAIN} --ecc "
        f"--fullchain-file {SSL_REMOTE}/{DOMAIN}.pem "
        f"--key-file {SSL_REMOTE}/{DOMAIN}.key "
        f"--reloadcmd 'docker exec {NGINX_CONTAINER} nginx -s reload'")

    # 4. 确认 cron
    print("\n[4/5] 确认自动续签 cron")
    run(ssh, "crontab -l | grep -i acme || echo '!!! 无 acme cron'", check=False)

    # 5. 验证
    print("\n[5/5] 验证")
    time.sleep(3)
    run(ssh, f"echo | openssl s_client -connect 127.0.0.1:443 -servername {DOMAIN} 2>/dev/null "
             f"| openssl x509 -noout -subject -issuer -dates", check=False)
    run(ssh, f"curl -sI --resolve {DOMAIN}:443:127.0.0.1 https://{DOMAIN}/ | head -3", check=False)
    run(ssh, f"curl -s --resolve {DOMAIN}:443:127.0.0.1 "
             f"https://{DOMAIN}/api/v1/actuator/health", check=False)
    run(ssh, f"curl -sI --resolve www.{DOMAIN}:443:127.0.0.1 https://www.{DOMAIN}/ | head -3", check=False)

    ssh.close()
    print("\nDONE：证书已换 LE，cron 自动续签（<30 天触发，reload nginx 零停机）")


if __name__ == "__main__":
    main()
