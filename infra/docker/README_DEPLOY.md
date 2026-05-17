# 校园智能学习系统 — 部署文档

> 本文档指导如何在 **Ubuntu 22.04** 服务器上通过 Docker Compose 一键部署完整服务栈。

---

## 一、环境准备

### 1.1 服务器要求

| 项目 | 最低配置 | 推荐配置 |
|------|---------|---------|
| 操作系统 | Ubuntu 22.04 LTS | Ubuntu 22.04/24.04 LTS |
| CPU | 2 核 | 4 核及以上 |
| 内存 | 4 GB | 8 GB 及以上 |
| 磁盘 | 40 GB SSD | 100 GB SSD |
| 网络 | 公网 IP + 开放 80/8080/8000 端口 | 域名 + HTTPS |

### 1.2 端口开放清单

部署前请确保以下端口未被占用或已在安全组/防火墙中开放：

| 端口 | 服务 | 说明 |
|------|------|------|
| 80 | Nginx | 前端 + API 统一入口（必须开放） |
| 8080 | Java 后端 | 直接访问调试（可选开放） |
| 8000 | Python AI | 直接访问调试（可选开放） |
| 3306 | MySQL | 数据库（建议仅内网或 localhost） |
| 6379 | Redis | 缓存（建议仅内网或 localhost） |
| 9000 | MinIO API | 对象存储 API（可选开放） |
| 9001 | MinIO Console | 对象存储控制台（可选开放） |

### 1.3 防火墙配置示例（UFW）

```bash
sudo apt update && sudo apt install -y ufw
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 80/tcp
sudo ufw allow 8080/tcp
sudo ufw allow 8000/tcp
# 如需远程管理数据库/MinIO，再开放对应端口
sudo ufw enable
```

---

## 二、快速开始（一键部署）

### 2.1 上传代码到服务器

```bash
# 方式一：Git 克隆（推荐）
git clone <你的仓库地址> /opt/campus-learning-assistant
cd /opt/campus-learning-assistant

# 方式二：本地打包后通过 scp/rsync 上传
# rsync -avz --exclude=node_modules --exclude=.git --exclude=target ./ root@<服务器IP>:/opt/campus-learning-assistant/
```

### 2.2 配置环境变量

```bash
cd /opt/campus-learning-assistant/infra/docker
cp .env.example .env
nano .env   # 或 vim .env
```

**必须修改的项：**

```ini
# MySQL root 密码与业务用户密码（务必修改）
MYSQL_ROOT_PASSWORD=YourStrongRootPassword2024
MYSQL_PASSWORD=YourStrongUserPassword2024

# MinIO 密钥（务必修改）
MINIO_SECRET_KEY=YourStrongMinioPassword2024

# JWT 签名密钥（至少 32 字符，务必修改）
JWT_SECRET=your-very-long-and-random-jwt-secret-key-32chars-plus

# 智谱 AI API Key（如需 OCR 增强功能）
ZHIPU_API_KEY=your.zhipu.api.key.here

# CORS 允许的域名（替换为你的公网 IP 或域名）
CORS_ALLOWED_ORIGINS=http://localhost,http://8.130.171.155,http://your-domain.com
```

### 2.3 执行部署脚本

```bash
chmod +x deploy.sh
./deploy.sh
```

脚本将自动完成：
1. 安装 Docker & Docker Compose（如未安装）
2. 构建前端（如检测到 npm）
3. 拉取基础镜像并构建 Java/Python 镜像
4. 启动全部 6 个服务
5. 执行健康检查并输出访问地址

### 2.4 手动部署（如脚本失败）

```bash
# 进入部署目录
cd /opt/campus-learning-assistant/infra/docker

# 构建并启动
docker compose pull mysql redis minio nginx
docker compose build --no-cache java-app python-app
docker compose up -d

# 查看状态
docker compose ps

# 查看日志
docker compose logs -f java-app
```

---

## 三、验证步骤

### 3.1 服务健康检查

```bash
# Nginx
curl http://localhost/nginx-health

# Java 后端（登录接口应返回 401/200，不应返回 403）
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"account":"test","password":"test123"}'

# Python AI 引擎
curl http://localhost:8000/

# MinIO 控制台（浏览器访问）
# http://<服务器IP>:9001/  （账号/密码见 .env）
```

### 3.2 端到端功能验证

1. **打开前端页面**
   - 浏览器访问 `http://<服务器IP>/`
   - 应能看到 Vue 前端首页

2. **注册/登录**
   - 点击注册，填写学号/用户名/邮箱/密码
   - 注册成功后登录
   - 登录成功应跳转到首页，并带有 Token

3. **文件上传与 OCR**
   - 上传一张图片或 PDF
   - 等待处理完成
   - 检查返回的 OCR 文本和置信度

4. **AI 增强（如配置了 ZHIPU_API_KEY）**
   - 上传低质量/模糊图片
   - 若置信度低于阈值，系统应自动调用智谱 API 增强
   - 在 python-app 日志中可看到 API 调用记录

---

## 四、目录结构说明

```
campus-learning-assistant/
├── backend/
│   ├── java/learning-assistant/    # Spring Boot 后端源码 + Dockerfile
│   └── python/                     # FastAPI AI 引擎源码 + Dockerfile
├── frontend/                       # Vue3 前端
│   └── dist/                       # 构建产物（Nginx 挂载）
├── infra/docker/
│   ├── docker-compose.yml          # 6 服务编排
│   ├── nginx.conf                  # 反向代理配置
│   ├── .env.example                # 环境变量模板
│   ├── deploy.sh                   # 一键部署脚本
│   └── README_DEPLOY.md            # 本文件
└── 开发/                            # 开发看板与文档
```

---

## 五、常用运维命令

### 5.1 查看日志

```bash
docker compose logs -f <服务名>     # 实时跟踪
docker compose logs --tail 100 java-app   # 查看最新 100 行
```

### 5.2 重启/停止服务

```bash
docker compose restart java-app     # 重启单个服务
docker compose down                 # 停止并移除所有容器
docker compose down -v              # 停止并移除容器+数据卷（慎用）
```

### 5.3 进入容器排查

```bash
docker exec -it cla-java-app sh
docker exec -it cla-mysql mysql -u cla_user -p
```

### 5.4 更新部署

```bash
# 拉取最新代码
git pull

# 重新构建并启动
docker compose build --no-cache java-app
docker compose up -d --no-deps java-app
```

---

## 六、常见问题

### Q1: 执行 `./deploy.sh` 提示权限不足

```bash
chmod +x deploy.sh
# 若当前用户不在 docker 组，需加 sudo：
sudo ./deploy.sh
```

### Q2: Java 后端启动后 `/api/auth/login` 返回 403

**原因**：Spring Security filter chain 与 `java -jar` 模式存在兼容性问题。  
**解决**：本批次已修复，请确保代码已更新到最新。如仍出现：

```bash
# 检查 SecurityConfig 是否加载
docker compose logs java-app | grep -i "security\|filter"

# 确认 application.properties 中无 spring.security.user 硬编码
```

### Q3: MySQL 连接失败 / ` Communications link failure`

**原因**：Java 容器启动快于 MySQL 就绪，或 `.env` 中密码不匹配。  
**解决**：

```bash
# 检查 MySQL 是否健康
docker compose ps mysql

# 如不健康，查看日志
docker compose logs mysql

# 手动创建数据库用户（首次部署）
docker exec -it cla-mysql mysql -u root -p -e "
CREATE USER IF NOT EXISTS 'cla_user'@'%' IDENTIFIED BY '你的密码';
GRANT ALL ON learning_assistant.* TO 'cla_user'@'%';
FLUSH PRIVILEGES;
"
```

### Q4: 前端页面空白 / 404

**原因**：`frontend/dist` 目录不存在或未挂载到 Nginx。  
**解决**：

```bash
# 本地构建前端并重新上传 dist/
cd frontend
npm install
npm run build

# 服务器上重启 Nginx
docker compose restart nginx
```

### Q5: OCR 上传 PDF 报错 "poppler 未安装"

**原因**：Python 容器缺少 poppler-utils。  
**解决**：本批次 Dockerfile 已包含 `poppler-utils`，如仍报错：

```bash
docker exec -it cla-python-app bash
which pdftoppm   # 确认路径
# 如不存在，检查 Dockerfile 构建是否成功
```

### Q6: 智谱 API 调用超时 / 无法增强

**原因**：`ZHIPU_API_KEY` 未配置或网络不通。  
**解决**：

```bash
# 检查环境变量是否注入
docker exec cla-python-app env | grep ZHIPU

# 测试网络连通性
docker exec -it cla-python-app bash
curl -I https://open.bigmodel.cn
```

### Q7: 如何配置 HTTPS

**建议**：在 Nginx 前增加一层反向代理（如阿里云 SLB、Cloudflare、或服务器上直接用 certbot）：

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

然后在 `nginx.conf` 中将 `listen 80` 改为 `listen 443 ssl` 并配置证书路径。

---

## 七、安全建议

1. **立即修改所有默认密码**：`MYSQL_ROOT_PASSWORD`、`MYSQL_PASSWORD`、`MINIO_SECRET_KEY`、`JWT_SECRET`
2. **关闭不必要的公网端口**：生产环境仅开放 80/443，数据库和 Redis 不暴露公网
3. **定期备份数据卷**：
   ```bash
   docker run --rm -v cla_mysql_data:/data -v $(pwd):/backup alpine tar czf /backup/mysql_backup_$(date +%F).tar.gz -C /data .
   ```
4. **启用阿里云安全组**：仅允许必要 IP 访问管理端口
5. **定期更新基础镜像**：`docker compose pull && docker compose up -d`

---

## 八、联系与反馈

- 部署问题：请查看 `docker compose logs <服务名>` 并携带日志提交 Issue
- 业务问题：参考 `docs/` 目录下的详细设计文档

---

**最后更新**：2026-05-14  
**适用版本**：批次 4（SecurityConfig 修复 + Docker Compose 补齐）
