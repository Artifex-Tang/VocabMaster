#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Provision a real test user on cloud prod for E2E.
Flow: send-code -> read code from cloud Redis (SSH) -> register -> login.
Writes tokens+user to the given JSON file. Prints nothing on success except OK.
Usage: python provision.py <email> <password> <outfile.json>
"""
import sys, json, time, urllib.request, paramiko

CLOUD = "http://60.205.145.132"
API = CLOUD + "/api/v1"
HOST = "60.205.145.132"; RPWD = "Tang@20023445"

def post(path, body):
    req = urllib.request.Request(API + path, data=json.dumps(body).encode(),
                                 headers={"Content-Type": "application/json"}, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        return {"_http": e.code, "_body": e.read().decode(errors="replace")}

def redis_get(key):
    s = paramiko.SSHClient(); s.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    s.connect(HOST, username="root", password=RPWD, timeout=15)
    _, o, _ = s.exec_command("docker exec vocab-redis redis-cli -a redis_prod_2024 --no-auth-warning GET '%s'" % key)
    v = o.read().decode(errors="replace").strip().strip('"')
    s.close(); return v

def main():
    email = sys.argv[1]; pwd = sys.argv[2]; out = sys.argv[3]
    r = post("/auth/send-code", {"type": "email", "identifier": email, "scene": "register"})
    if r.get("code") != 0:
        sys.exit("send-code failed: " + json.dumps(r, ensure_ascii=False))
    time.sleep(1.2)
    code = redis_get("code:register:email:%s" % email)
    if not code:
        sys.exit("no code in redis for " + email)
    r = post("/auth/register", {"type": "email", "identifier": email, "password": pwd,
                                "code": code, "nickname": "e2e"})
    if r.get("code") != 0:
        sys.exit("register failed: " + json.dumps(r, ensure_ascii=False))
    r = post("/auth/login", {"type": "email", "identifier": email, "password": pwd})
    if r.get("code") != 0:
        sys.exit("login failed: " + json.dumps(r, ensure_ascii=False))
    data = r["data"]
    json.dump({"email": email, "password": pwd,
               "access_token": data["access_token"], "refresh_token": data["refresh_token"],
               "user": data["user"]}, open(out, "w", encoding="utf-8"), ensure_ascii=False)
    print("OK " + email)

if __name__ == "__main__":
    main()
