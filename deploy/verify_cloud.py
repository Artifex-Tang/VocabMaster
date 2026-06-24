"""验证云端 Think 数据导入结果（干净引号）。"""
import paramiko

HOST = "60.205.145.132"; USER = "root"; PWD = "Tang@20023445"

def run(ssh, cmd):
    _, o, e = ssh.exec_command(cmd)
    out = o.read().decode("utf-8", "replace").strip()
    err = e.read().decode("utf-8", "replace").strip()
    rc = o.channel.recv_exit_status()
    if out:
        print(out, flush=True)
    if err:
        print("[err]", err[:300], flush=True)
    return out

ssh = paramiko.SSHClient(); ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(HOST, username=USER, password=PWD, timeout=10)

MY = "docker exec vocab-mysql mysql --default-character-set=utf8mb4 -uroot -pvocab_root_2024 vocabmaster"
run(ssh, MY + ' -e "SELECT COUNT(*) AS think_words FROM word_bank WHERE level_code LIKE \'THINK_%\';" 2>/dev/null')
run(ssh, MY + ' -e "SELECT level_code,COUNT(*) FROM word_bank WHERE level_code LIKE \'THINK_%\' GROUP BY level_code;" 2>/dev/null')
run(ssh, MY + ' -e "SELECT COUNT(*) AS lists FROM word_list WHERE source_type=\'builtin\'; SELECT COUNT(*) AS items FROM word_list_item; SELECT COUNT(*) AS levels FROM level WHERE code LIKE \'THINK_%\';" 2>/dev/null')
run(ssh, MY + ' -e "SELECT word,zh_definition FROM word_bank WHERE level_code=\'THINK_L2\' AND zh_definition!=\'\' LIMIT 4;" 2>/dev/null')
run(ssh, MY + ' -e "SELECT word,CHAR_LENGTH(zh_definition) AS zh_len FROM word_bank WHERE word=\'caring\' AND level_code=\'THINK_L2\';" 2>/dev/null')
run(ssh, MY + ' -e "SELECT COUNT(*) AS users FROM user;" 2>/dev/null')
print("=== /word-lists 公网（401=端点在） ===", flush=True)
run(ssh, "curl -s -o /dev/null -w 'word-lists HTTP %{http_code}\\n' http://localhost/api/v1/word-lists")
ssh.close(); print("DONE", flush=True)
