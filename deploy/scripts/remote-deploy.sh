#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="/www/server/relax"
NGINX_VHOST_DIR="/www/server/panel/vhost/nginx"

echo "=== [1/5] 检查并更新代码与配置文件 ==="
cd "${DEPLOY_DIR}"

if [ ! -f "deploy/.env" ]; then
    echo "初始化 deploy/.env 配置文件..."
    cp .env.example deploy/.env
fi

echo "=== [2/5] 检查宿主机 Nginx 反向代理配置 ==="
if [ -d "${NGINX_VHOST_DIR}" ]; then
    rm -f "${NGINX_VHOST_DIR}/relaxback.lyhlz.cn.conf"
    cp deploy/nginx/realxback.lyhlz.cn.conf "${NGINX_VHOST_DIR}/realxback.lyhlz.cn.conf"
    nginx -t && nginx -s reload || echo "Nginx 重新加载跳过"
fi

echo "=== [3/5] 构建并启动 Docker Compose 服务 ==="
cd "${DEPLOY_DIR}/deploy"

# 如果 relax-server:latest 镜像不存在，则使用 Dockerfile.slim 构建基础运行镜像
if ! docker image inspect relax-server:latest >/dev/null 2>&1; then
    echo "本地未发现 relax-server:latest 镜像，开始初始化镜像..."
    docker build -f ../server/Dockerfile.slim -t relax-server:latest ../server
fi

docker compose up -d --remove-orphans mysql redis
docker compose up -d --remove-orphans --force-recreate server
docker compose up -d --remove-orphans nginx

echo "=== [4/5] 等待服务健康检查 ==="
MAX_RETRIES=20
COUNT=0
HEALTH_URL="http://127.0.0.1:8080/actuator/health"

while [ $COUNT -lt $MAX_RETRIES ]; do
    if curl -s -f "${HEALTH_URL}" | grep -q '"status":"UP"'; then
        echo "✅ 后端服务健康检查通过！状态: UP"
        break
    fi
    echo "等待服务启动中 ($((COUNT+1))/${MAX_RETRIES})..."
    sleep 3
    COUNT=$((COUNT+1))
done

if [ $COUNT -ge $MAX_RETRIES ]; then
    echo "❌ 服务在预期时间内未就绪，输出容器日志："
    docker compose logs --tail=50 server
    exit 1
fi

echo "=== [5/5] 部署完成！==="
docker compose ps
