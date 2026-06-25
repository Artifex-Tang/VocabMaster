#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Provision a real E2E test user. register-or-login (reuse fixed email), then login.
Writes tokens+user to the given JSON file. Prints OK <email> on success.

Target backend via E2E_API env (default local docker http://localhost:8080/api/v1).
Verification code is read from Redis:
  - local docker  : `docker exec vocab-redis redis-cli -a $REDIS_PASSWORD GET <key>`
  - cloud prod    : SSH root@host `redis-cli` (paramiko)

Usage: E2E_API=... python provision.py <email> <password> <outfile.json>
"""
import sys, json, time, os, subprocess, urllib.request, urllib.error

API = os.environ.get('E2E_API', 'http://localhost:8080/api/v1')
REDIS_PWD = os.environ.get('REDIS_PASSWORD', 'redis123')
IS_LOCAL = any(h in API for h in ('localhost', '127.0.0.1'))
# cloud SSH (only used when not local)
CLOUD_HOST = '60.205.145.132'; CLOUD_RPWD = 'Tang@20023445'


def post(path, body):
    req = urllib.request.Request(API + path, data=json.dumps(body).encode(),
                                 headers={'Content-Type': 'application/json'}, method='POST')
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        return {'_http': e.code, '_body': e.read().decode(errors='replace')}


def redis_get_local(key):
    out = subprocess.run(['docker', 'exec', 'vocab-redis', 'redis-cli', '-a',
                          REDIS_PWD, '--no-auth-warning', 'GET', key],
                         capture_output=True, text=True, timeout=10)
    return out.stdout.strip().strip('"')


def redis_get_cloud(key):
    import paramiko
    s = paramiko.SSHClient(); s.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    s.connect(CLOUD_HOST, username='root', password=CLOUD_RPWD, timeout=15)
    _, o, _ = s.exec_command(
        "docker exec vocab-redis redis-cli -a redis_prod_2024 --no-auth-warning GET '%s'" % key)
    v = o.read().decode(errors='replace').strip().strip('"')
    s.close()
    return v


def redis_get(key):
    return redis_get_local(key) if IS_LOCAL else redis_get_cloud(key)


def main():
    email = sys.argv[1]; pwd = sys.argv[2]; out = sys.argv[3]

    # send-code -> read code -> register (skip if already exists)
    r = post('/auth/send-code', {'type': 'email', 'identifier': email, 'scene': 'register'})
    if r.get('code') != 0:
        sys.exit('send-code failed: ' + json.dumps(r, ensure_ascii=False))
    time.sleep(1.2)
    code = redis_get('code:register:email:%s' % email)
    if not code:
        sys.exit('no code in redis for ' + email)
    r = post('/auth/register', {'type': 'email', 'identifier': email, 'password': pwd,
                                'code': code, 'nickname': 'e2e'})
    if r.get('code') != 0:
        # already registered -> fall through to login (idempotent reuse)
        sys.stderr.write('register skipped: ' + json.dumps(r, ensure_ascii=False) + '\n')

    # always login (confirms creds + captures fresh tokens)
    r = post('/auth/login', {'type': 'email', 'identifier': email, 'password': pwd})
    if r.get('code') != 0:
        sys.exit('login failed: ' + json.dumps(r, ensure_ascii=False))
    data = r['data']
    json.dump({'email': email, 'password': pwd,
               'access_token': data['access_token'], 'refresh_token': data['refresh_token'],
               'user': data['user']}, open(out, 'w', encoding='utf-8'), ensure_ascii=False)
    print('OK ' + email + ' target=' + ('local' if IS_LOCAL else 'cloud'))


if __name__ == '__main__':
    main()
