# 10 — 部署方案

## 开发环境（本地一键启动）

所有依赖都通过 Docker Compose 拉起，后端本地 IDE 运行（便于断点调试）。

### `docker-compose.yml`（根目录）

```yaml
version: "3.8"

services:
  mysql:
    image: mysql:8.0
    container_name: vocab-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: rootpass
      MYSQL_DATABASE: vocabmaster
      MYSQL_USER: vocab
      MYSQL_PASSWORD: vocab123
      TZ: UTC
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql/init.sql:/docker-entrypoint-initdb.d/01-init.sql:ro
      - ./seed/words_sample.sql:/docker-entrypoint-initdb.d/02-seed.sql:ro
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_0900_ai_ci
      - --default-time-zone=+00:00
      - --max_connections=300
      - --innodb_buffer_pool_size=512M
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "127.0.0.1", "-uroot", "-prootpass"]
      interval: 5s
      timeout: 3s
      retries: 20

  redis:
    image: redis:7-alpine
    container_name: vocab-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 10

  # 可选：Adminer 管理 MySQL 的 Web UI
  adminer:
    image: adminer:latest
    container_name: vocab-adminer
    restart: unless-stopped
    ports:
      - "8081:8080"
    depends_on:
      - mysql

  # 可选：Redis Insight
  redis-insight:
    image: redislabs/redisinsight:latest
    container_name: vocab-redis-insight
    restart: unless-stopped
    ports:
      - "8082:8001"

volumes:
  mysql_data:
  redis_data:
```

### 启动

```bash
cd vocabmaster/
docker compose up -d

# 验证
docker compose ps
docker compose logs -f mysql | head -30
```

后端和前端用 IDE 运行：

```bash
# Java 后端
cd backend-java
./mvnw spring-boot:run

# Web 前端
cd frontend-web
pnpm install && pnpm dev

# Uni-app
cd frontend-uniapp
pnpm install
# 然后用 HBuilderX 打开项目，或 pnpm run dev:h5 / dev:mp-weixin
```

## 生产环境架构

```
                         ┌──────────────┐
                         │  CloudFlare  │  CDN + DDoS
                         └──────┬───────┘
                                │
                         ┌──────▼───────┐
                         │  Nginx :443  │  SSL 终止 + 反向代理
                         └──────┬───────┘
                     ┌──────────┼──────────┐
                     │          │          │
              ┌──────▼──┐  ┌────▼─────┐ ┌──▼───────┐
              │  静态   │  │ Backend  │ │  /admin  │
              │  frontend│ │  ×2 实例 │ │  (同 web)│
              │  web    │  └────┬─────┘ └──────────┘
              └─────────┘       │
                                │
                      ┌─────────┼─────────┐
                      │         │         │
                ┌─────▼──┐ ┌────▼────┐ ┌──▼────┐
                │ MySQL  │ │  Redis  │ │  OSS  │
                │主+从   │ │ 主+哨兵 │ │词库音频/图│
                └────────┘ └─────────┘ └───────┘
```

### 资源规划（万级 DAU）

| 组件 | 规格 | 数量 |
|------|------|------|
| Nginx | 2C4G | 1 |
| Backend（Spring Boot） | 4C8G | 2 |
| MySQL | 8C16G + 500G SSD | 1 主 + 1 从 |
| Redis | 2C4G | 1 主 + 2 哨兵 |
| 对象存储 | 按量计费 | — |

月成本估算（阿里云或腾讯云）：约 ¥2500~3500。

### Nginx 配置示例

```nginx
# /etc/nginx/sites-enabled/vocabmaster.conf
upstream vocab_backend {
    server backend1:8080 max_fails=3 fail_timeout=30s;
    server backend2:8080 max_fails=3 fail_timeout=30s;
    keepalive 32;
}

server {
    listen 443 ssl http2;
    server_name vocabmaster.com;
    
    ssl_certificate /etc/ssl/vocabmaster.com.crt;
    ssl_certificate_key /etc/ssl/vocabmaster.com.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    # 安全头
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    
    # Web 前端静态文件
    root /var/www/vocabmaster/dist;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
        add_header Cache-Control "no-cache";
    }
    
    location ~* \.(js|css|woff2?|svg|png|jpg|webp)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # API 反代
    location /api/ {
        proxy_pass http://vocab_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        
        proxy_connect_timeout 5s;
        proxy_read_timeout 30s;
        proxy_send_timeout 30s;
        
        client_max_body_size 10M;   # 管理员上传词库 CSV
    }
    
    # 限流（接口层面还有应用限流，这里做保护）
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=30r/s;
    location /api/v1/auth/ {
        limit_req zone=api_limit burst=20 nodelay;
        proxy_pass http://vocab_backend;
    }
}

server {
    listen 80;
    server_name vocabmaster.com;
    return 301 https://$host$request_uri;
}
```

### 后端容器化（Java）

```dockerfile
# backend-java/Dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline
COPY src ./src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN addgroup --system --gid 1001 vocab && adduser --system --uid 1001 --ingroup vocab vocab
COPY --from=build /build/target/*.jar app.jar
USER vocab
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Xms512m -Xmx1536m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --spider -q http://localhost:8080/actuator/health || exit 1
```

### 生产 docker-compose

```yaml
# docker-compose.prod.yml
version: "3.8"

services:
  backend1:
    image: vocabmaster/backend:${VERSION:-latest}
    restart: always
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/vocabmaster?serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: vocab
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD}
      APP_JWT_SECRET: ${JWT_SECRET}
      APP_AES_KEY: ${AES_KEY}
      APP_OAUTH_WECHAT_APP_ID: ${WECHAT_APP_ID}
      APP_OAUTH_WECHAT_APP_SECRET: ${WECHAT_APP_SECRET}
    networks:
      - vocab-net
    deploy:
      resources:
        limits:
          cpus: "2"
          memory: 2G
  
  backend2:
    # 同上
    ...
  
  nginx:
    image: nginx:1.25-alpine
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/vocabmaster.conf:/etc/nginx/conf.d/default.conf:ro
      - ./ssl:/etc/ssl:ro
      - ./frontend-web-dist:/var/www/vocabmaster/dist:ro
    depends_on:
      - backend1
      - backend2
    networks:
      - vocab-net

networks:
  vocab-net:
    driver: bridge
```

## CI/CD

### GitHub Actions 示例（`.github/workflows/backend.yml`）

```yaml
name: Backend CI/CD

on:
  push:
    branches: [main, dev]
    paths: ['backend-java/**']

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'
      
      - name: Run tests
        working-directory: backend-java
        run: ./mvnw -B test
      
      - name: Build JAR
        working-directory: backend-java
        run: ./mvnw -B -DskipTests package
      
      - name: Build Docker image
        if: github.ref == 'refs/heads/main'
        run: |
          docker build -t vocabmaster/backend:${{ github.sha }} backend-java
          docker tag vocabmaster/backend:${{ github.sha }} vocabmaster/backend:latest
      
      - name: Push to registry
        if: github.ref == 'refs/heads/main'
        run: |
          echo "${{ secrets.REGISTRY_PASSWORD }}" | docker login registry.example.com -u "${{ secrets.REGISTRY_USER }}" --password-stdin
          docker push vocabmaster/backend:${{ github.sha }}
          docker push vocabmaster/backend:latest
      
      - name: Deploy
        if: github.ref == 'refs/heads/main'
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.PROD_HOST }}
          username: ${{ secrets.PROD_USER }}
          key: ${{ secrets.PROD_SSH_KEY }}
          script: |
            cd /opt/vocabmaster
            export VERSION=${{ github.sha }}
            docker compose -f docker-compose.prod.yml pull backend1 backend2
            docker compose -f docker-compose.prod.yml up -d --no-deps backend1
            sleep 30
            docker compose -f docker-compose.prod.yml up -d --no-deps backend2
```

前端和 Uni-app 各一个类似的工作流。

## 监控

### 应用指标

- **Spring Boot Actuator** 暴露 `/actuator/prometheus`
- **Prometheus** 抓取 + **Grafana** 面板
- 关键指标：
  - HTTP 请求量 / 延时 / 错误率（按 URI 维度）
  - JVM 内存 / GC 时间 / 线程数
  - 数据库连接池使用率
  - Redis 命中率
  - 业务指标：今日活跃用户数、答题次数、注册数

### 日志

- **应用日志**：JSON 格式输出到 stdout，由 Docker log driver 收集到 Loki 或 ELK
- **访问日志**：Nginx access log 入 Loki
- **错误告警**：Sentry 接收应用层异常

### 告警规则

| 规则 | 阈值 | 动作 |
|------|------|------|
| HTTP 5xx 率 | > 1% / 5min | 钉钉 + 短信 |
| API P99 | > 2s / 5min | 钉钉 |
| DB 连接池使用率 | > 80% / 5min | 钉钉 |
| MySQL 主从延迟 | > 10s | 短信 |
| 磁盘使用率 | > 85% | 钉钉 |
| JVM Old Gen 使用率 | > 85% | 钉钉 |

## 数据备份

- **MySQL**：每日凌晨 2 点 `mysqldump` 全量备份到 OSS，保留 30 天；同时启用 binlog 做到分钟级别 PITR
- **Redis**：AOF 持久化 + 每日 RDB 快照
- **对象存储**：开启版本化，误删可恢复

备份脚本（cron `0 2 * * *`）：

```bash
#!/bin/bash
set -e
DATE=$(date +%Y%m%d)
BACKUP_DIR=/var/backups/mysql
mkdir -p $BACKUP_DIR

docker exec vocab-mysql mysqldump \
  -uroot -p${MYSQL_ROOT_PASSWORD} \
  --single-transaction --routines --triggers \
  --databases vocabmaster \
  | gzip > $BACKUP_DIR/vocabmaster_$DATE.sql.gz

# 上传到 OSS
ossutil cp $BACKUP_DIR/vocabmaster_$DATE.sql.gz oss://vocab-backup/mysql/

# 清理 7 天前的本地备份
find $BACKUP_DIR -name '*.sql.gz' -mtime +7 -delete
```

## 压测

上线前用 **k6** 或 **JMeter** 做压测。典型场景：

```javascript
// k6 脚本：模拟答题接口
import http from 'k6/http'
import { check, sleep } from 'k6'

export const options = {
  stages: [
    { duration: '2m', target: 100 },    // ramp up
    { duration: '5m', target: 500 },    // steady
    { duration: '2m', target: 0 },      // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
}

export default function () {
  const res = http.post(
    'https://vocabmaster.com/api/v1/study/answer',
    JSON.stringify({
      word_id: Math.floor(Math.random() * 5000) + 1,
      level_code: 'CET4',
      result: Math.random() < 0.8 ? 'correct' : 'wrong',
      mode: 'card',
      duration_ms: 3000,
      client_ts: new Date().toISOString(),
    }),
    { headers: { 'Authorization': `Bearer ${__ENV.TOKEN}`, 'Content-Type': 'application/json' } }
  )
  check(res, { 'status 200': r => r.status === 200 })
  sleep(1)
}
```

目标：P95 < 500ms、错误率 < 1%、CPU < 70%、DB 连接数 < 80%。

## 安全检查清单

- [ ] HTTPS 全站 + HSTS
- [ ] JWT secret 生产环境使用 32 字节以上强随机
- [ ] AES 密钥生产环境独立生成，不与开发共用
- [ ] 数据库密码、微信 AppSecret 等敏感信息走环境变量，不入 Git
- [ ] MySQL/Redis 只在内网暴露，不开放公网端口
- [ ] 定期备份 + 恢复演练（至少每季度一次）
- [ ] 接口限流 + 验证码（防爆破）
- [ ] 日志脱敏（手机号、邮箱、token 不完整打印）
- [ ] 依赖安全扫描（Dependabot、trivy）
- [ ] 管理员账号二步验证（开发阶段可用 TOTP）
