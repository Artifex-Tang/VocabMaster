#!/usr/bin/env bash
# ══════════════════════════════════════════════════
# VocabMaster 一键部署脚本
# 用法：bash deploy/deploy.sh
# 前提：服务器已安装 Docker + Docker Compose + Git
# ══════════════════════════════════════════════════
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_DIR"

echo "========================================="
echo "  VocabMaster 部署脚本"
echo "========================================="

# ── 1. 检查环境 ──
echo ""
echo "[1/6] 检查环境..."
for cmd in docker git; do
    if ! command -v $cmd &> /dev/null; then
        echo "ERROR: $cmd 未安装"
        exit 1
    fi
done

if ! docker compose version &> /dev/null; then
    echo "ERROR: docker compose 未安装 (需要 Docker Compose V2)"
    exit 1
fi

echo "  Docker: $(docker --version)"
echo "  Compose: $(docker compose version)"

# ── 2. .env 文件 ──
echo ""
echo "[2/6] 检查 .env 文件..."
if [ ! -f .env ]; then
    cp deploy/.env.example .env
    echo "  已创建 .env，请编辑后重新运行："
    echo "    vim .env"
    echo ""
    echo "  必须修改：JWT_SECRET, DB_PASSWORD, REDIS_PASSWORD"
    exit 0
else
    echo "  .env 已存在"
fi

# ── 3. 编译后端 ──
echo ""
echo "[3/6] 编译 Java 后端..."
if [ ! -f backend-java/target/vocabmaster-backend.jar ]; then
    echo "  正在 Maven 构建..."
    cd backend-java
    if [ -f mvnw ]; then
        ./mvnw -B -q package -DskipTests
    else
        mvn -B -q package -DskipTests
    fi
    cd "$PROJECT_DIR"
    echo "  编译完成"
else
    echo "  JAR 已存在，跳过编译"
fi

# ── 4. 编译前端 ──
echo ""
echo "[4/6] 编译 Web 前端..."
if [ ! -d wordmate-web/dist ] || [ -z "$(ls -A wordmate-web/dist 2>/dev/null)" ]; then
    echo "  正在 Vite 构建..."
    cd wordmate-web
    npm install --prefer-offline 2>/dev/null || pnpm install 2>/dev/null
    npm run build 2>/dev/null || pnpm build
    cd "$PROJECT_DIR"
    echo "  编译完成"
else
    echo "  dist 已存在，跳过编译"
fi

# ── 5. 启动服务 ──
echo ""
echo "[5/6] 启动 Docker 服务..."
docker compose up -d
echo "  服务启动中..."

# ── 6. 健康检查 ──
echo ""
echo "[6/6] 等待服务就绪..."
MAX_WAIT=120
ELAPSED=0
while [ $ELAPSED -lt $MAX_WAIT ]; do
    if curl -sf http://localhost:8080/api/v1/actuator/health > /dev/null 2>&1; then
        echo ""
        echo "========================================="
        echo "  部署成功！"
        echo "========================================="
        echo ""
        echo "  Web 前端:  http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'YOUR_SERVER_IP')"
        echo "  API:       http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'YOUR_SERVER_IP')/api/v1"
        echo "  Swagger:   http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'YOUR_SERVER_IP')/swagger-ui/"
        echo ""
        echo "  查看日志:  docker compose logs -f"
        echo "  停止服务:  docker compose down"
        echo "========================================="
        exit 0
    fi
    sleep 3
    ELAPSED=$((ELAPSED + 3))
    echo -n "."
done

echo ""
echo "WARNING: 后端未在 ${MAX_WAIT}s 内就绪，请检查日志："
echo "  docker compose logs backend-java"
