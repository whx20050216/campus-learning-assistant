#!/bin/bash
# =============================================================================
# 校园智能学习系统 — 一键部署脚本（Ubuntu 22.04）
# 用法：
#   chmod +x deploy.sh
#   ./deploy.sh
# =============================================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info()  { echo -e "${GREEN}[INFO]${NC}  $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="${PROJECT_DIR}/.env"

# ---------------------------
# 0. 前置检查
# ---------------------------
log_info "开始环境检查..."

# 检查是否为 Ubuntu/Debian
if ! grep -qiE "ubuntu|debian" /etc/os-release 2>/dev/null; then
    log_warn "非 Ubuntu/Debian 系统，部分命令可能不适用"
fi

# 检查内存（建议 ≥2G）
MEM_KB=$(grep MemTotal /proc/meminfo | awk '{print $2}')
MEM_GB=$(awk "BEGIN {printf \"%.0f\", $MEM_KB / 1024 / 1024}")
if [ "$MEM_GB" -lt 2 ]; then
    log_warn "内存不足 2G（当前约 ${MEM_GB}G），建议升级配置"
else
    log_info "内存检查通过：约 ${MEM_GB}G"
fi

# 检查磁盘（建议 ≥20G）
DISK_GB=$(df -BG "$PROJECT_DIR" | tail -1 | awk '{print $4}' | tr -d 'G')
if [ "$DISK_GB" -lt 20 ]; then
    log_warn "磁盘空间不足 20G（当前约 ${DISK_GB}G）"
else
    log_info "磁盘检查通过：约 ${DISK_GB}G 可用"
fi

# 检查端口占用
for PORT in 80 8080 8000 3306 6379 9000 9001; do
    if ss -tlnp | grep -q ":${PORT} "; then
        log_warn "端口 ${PORT} 已被占用，请确认是否为旧实例或冲突服务"
    fi
done

# ---------------------------
# 1. 安装 Docker & Docker Compose
# ---------------------------
log_info "检查 Docker 环境..."

if ! command -v docker &> /dev/null; then
    log_info "正在安装 Docker..."
    sudo apt-get update
    sudo apt-get install -y ca-certificates curl gnupg lsb-release
    sudo mkdir -p /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    sudo usermod -aG docker "$USER" || true
    log_info "Docker 安装完成，建议重新登录以生效用户组权限"
else
    log_info "Docker 已安装"
fi

# 确保 bc 等基础工具存在（部分精简镜像可能缺失）
if ! command -v awk &> /dev/null; then
    sudo apt-get install -y gawk
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    log_info "正在安装 Docker Compose..."
    sudo apt-get install -y docker-compose-plugin
fi

DOCKER_COMPOSE="docker compose"
if ! docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
fi
log_info "Docker Compose 命令: ${DOCKER_COMPOSE}"

# ---------------------------
# 2. 检查 .env 文件
# ---------------------------
if [ ! -f "$ENV_FILE" ]; then
    log_warn "未找到 .env 文件，将从 .env.example 复制"
    if [ -f "${PROJECT_DIR}/.env.example" ]; then
        cp "${PROJECT_DIR}/.env.example" "$ENV_FILE"
        log_warn "请编辑 ${ENV_FILE} 填入真实配置后再运行此脚本"
        exit 1
    else
        log_error "未找到 .env.example，无法创建默认配置"
        exit 1
    fi
fi

log_info ".env 文件已加载"

# ---------------------------
# 3. 加载环境变量（用于构建）
# ---------------------------
export $(grep -v '^#' "$ENV_FILE" | xargs -d '\n')

# ---------------------------
# 4. 构建前端（如存在源码）
# ---------------------------
FRONTEND_DIR="${PROJECT_DIR}/../../frontend"
if [ -d "$FRONTEND_DIR" ] && [ -f "${FRONTEND_DIR}/package.json" ]; then
    log_info "检测到前端源码，尝试构建..."
    if command -v npm &> /dev/null; then
        cd "$FRONTEND_DIR"
        npm install
        npm run build
        cd "$PROJECT_DIR"
        log_info "前端构建完成"
    else
        log_warn "未找到 npm，跳过前端构建。请手动构建并确保 dist/ 目录存在"
    fi
else
    log_warn "未检测到前端源码，请确保 ${FRONTEND_DIR}/dist 目录存在"
fi

if [ ! -d "${FRONTEND_DIR}/dist" ]; then
    log_error "前端构建产物 dist/ 目录不存在，Nginx 将无法提供静态资源"
    log_error "请在前端目录执行：npm install && npm run build"
    exit 1
fi

# ---------------------------
# 5. 拉取/构建镜像并启动
# ---------------------------
log_info "开始拉取基础镜像并构建应用镜像..."
cd "$PROJECT_DIR"
$DOCKER_COMPOSE pull mysql redis minio nginx
$DOCKER_COMPOSE build --no-cache java-app python-app

log_info "启动所有服务..."
$DOCKER_COMPOSE up -d

# ---------------------------
# 6. 等待并健康检查
# ---------------------------
log_info "等待服务初始化（约 30 秒）..."
sleep 10

# MySQL 健康检查
for i in {1..12}; do
    if $DOCKER_COMPOSE ps mysql | grep -qi "healthy\|running"; then
        log_info "MySQL 健康检查通过"
        break
    fi
    if [ "$i" -eq 12 ]; then
        log_error "MySQL 未在 2 分钟内就绪，请检查日志: ${DOCKER_COMPOSE} logs mysql"
        exit 1
    fi
    sleep 10
done

# Java 应用健康检查
for i in {1..18}; do
    if curl -sf http://localhost:8080/api/auth/login -X POST > /dev/null 2>&1 || curl -sf http://localhost:8080/error > /dev/null 2>&1; then
        log_info "Java 后端已启动"
        break
    fi
    if [ "$i" -eq 18 ]; then
        log_error "Java 后端未在 3 分钟内就绪，请检查日志: ${DOCKER_COMPOSE} logs java-app"
        exit 1
    fi
    sleep 10
done

# Python 应用健康检查
for i in {1..12}; do
    if curl -sf http://localhost:8000/ > /dev/null 2>&1; then
        log_info "Python AI 引擎已启动"
        break
    fi
    if [ "$i" -eq 12 ]; then
        log_error "Python AI 引擎未在 2 分钟内就绪，请检查日志: ${DOCKER_COMPOSE} logs python-app"
        exit 1
    fi
    sleep 10
done

# Nginx 健康检查
if curl -sf http://localhost/nginx-health > /dev/null 2>&1; then
    log_info "Nginx 已启动"
else
    log_warn "Nginx 健康检查未通过，请检查日志: ${DOCKER_COMPOSE} logs nginx"
fi

# ---------------------------
# 7. 最终状态
# ---------------------------
log_info "============================================"
log_info "部署完成！服务状态如下："
$DOCKER_COMPOSE ps
log_info "============================================"
log_info "访问地址："
log_info "  前端页面: http://<服务器IP>/"
log_info "  Java API: http://<服务器IP>:8080/api/"
log_info "  Python AI: http://<服务器IP>:8000/ai/"
log_info "  MinIO 控制台: http://<服务器IP>:9001/"
log_info "============================================"
log_info "常用命令："
log_info "  查看日志: ${DOCKER_COMPOSE} logs -f <服务名>"
log_info "  重启服务: ${DOCKER_COMPOSE} restart <服务名>"
log_info "  停止全部: ${DOCKER_COMPOSE} down"
log_info "============================================"
