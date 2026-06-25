#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Send verify code for <email> and print the 6-digit code (read from cloud Redis).
Usage: python getcode.py <email>
"""
import sys, json, time, urllib.request, urllib.error, paramiko
API = "http://60.205.145.132/api/v1"; HOST="60.205.145.132"; RPWD="Tang@20023445"
def post(path, body):
    req = urllib.request.Request(API+path, data=json.dumps(body).encode(),
                                 headers={"Content-Type":"application/json"}, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=15) as r: return json.loads(r.read().decode())
    except urllib.error.HTTPError as e: return {"_http":e.code,"_body":e.read().decode(errors="replace")}
email = sys.argv[1]
r = post("/auth/send-code", {"type":"email","identifier":email,"scene":"register"})
if r.get("code")!=0: sys.exit("send-code fail: "+json.dumps(r,ensure_ascii=False))
time.sleep(1.2)
s=paramiko.SSHClient(); s.set_missing_host_key_policy(paramiko.AutoAddPolicy())
s.connect(HOST,username="root",password=RPWD,timeout=15)
_,o,_=s.exec_command("docker exec vocab-redis redis-cli -a redis_prod_2024 --no-auth-warning GET 'code:register:email:%s'"%email)
print(o.read().decode(errors="replace").strip().strip('"')); s.close()
