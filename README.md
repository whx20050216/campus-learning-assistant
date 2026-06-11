# 校园智能学习助手（Campus Learning Assistant）

> 本项目为校园智能学习助手（Campus Learning Assistant），基于 Vue 3 + Spring Boot + FastAPI 构建，支持学习资料 OCR 识别、AI 关键词提取、智能组卷、AI 问答、学习计划管理与数据分析等功能。本文档涵盖项目结构、技术栈、构建与部署说明。

---

## 项目概述

校园智能学习助手是一个面向高校学生的智能化学习资料管理与知识提取平台。系统核心能力包括：学习资料上传（PDF/图片）、OCR 文字识别、NLP 关键词提取与知识点识别、智能搜索、学习计划制定与追踪、数据分析仪表盘。

项目采用**前后端分离 + AI 引擎独立部署**的三层架构：

- **前端**：Vue 3 单页应用，通过 Nginx 或 Vite DevServer 提供服务
- **Java 后端**：Spring Boot 核心业务服务，负责认证授权、业务编排、数据持久化
- **Python AI 引擎**：FastAPI 服务，负责 OCR、NLP、AI 增强（智谱 API）

---

## 技术栈

### 前端

| 技术         | 版本      | 说明                                       |
| ------------ | --------- | ------------------------------------------ |
| Vue          | `^3.5.30` | Composition API + `<script setup>`         |
| TypeScript   | `~5.9.3`  | 严格模式，`noUncheckedIndexedAccess: true` |
| Vite         | `^7.3.1`  | 构建工具，开发代理                         |
| Vue Router   | `^5.0.3`  | 路由守卫做认证与角色校验                   |
| Pinia        | `^3.0.4`  | 状态管理（仅 `stores/plan.ts` 一个 store） |
| Element Plus | `^2.13.6` | UI 组件库                                  |
| ECharts      | `^6.0.0`  | 数据分析图表                               |
| Axios        | `^1.13.6` | HTTP 客户端，拦截器处理 Token 与刷新       |

### Java 后端

| 技术              | 版本     | 说明                          |
| ----------------- | -------- | ----------------------------- |
| Spring Boot       | `3.2.12` | 主框架，JDK 17                |
| Maven             | 3.9      | 构建工具（`pom.xml`）         |
| MyBatis-Plus      | `3.5.8`  | ORM（`spring-boot3-starter`） |
| Spring Security   | —        | JWT 认证 + 角色鉴权           |
| MySQL Connector/J | —        | MySQL 8.0 驱动                |
| Spring Data Redis | —        | Redis 7.0 缓存/会话           |
| MinIO Java SDK    | `8.5.7`  | 对象存储                      |
| JJWT              | `0.9.1`  | JWT 生成与校验                |
| Lombok            | —        | 实体类简化                    |

### Python AI 引擎

| 技术         | 版本       | 说明                                    |
| ------------ | ---------- | --------------------------------------- |
| FastAPI      | `0.104.1`  | Web 框架                                |
| Uvicorn      | `0.24.0`   | ASGI 服务器                             |
| PaddleOCR    | `2.7.0`    | 本地 OCR（ch_PP-OCRv4）                 |
| PaddlePaddle | `3.0.0`    | 推理框架                                |
| Jieba        | `0.42.1`   | 中文分词                                |
| zhipuai      | `2.0.1`    | 智谱 AI API（GLM-4V-Flash / ChatGLM-4） |
| Pillow       | `10.0.1`   | 图像处理                                |
| OpenCV       | `4.6.0.66` | 图像预处理                              |
| pdf2image    | `1.17.0`   | PDF 多页转图片                          |
| Pydantic     | `2.5.2`    | 请求/响应模型校验                       |
| Redis        | `5.0.1`    | Python Redis 客户端                     |

### 基础设施

| 服务  | 版本/镜像      | 用途                                    |
| ----- | -------------- | --------------------------------------- |
| MySQL | `8.0.34`       | 结构化数据                              |
| Redis | `7.0-alpine`   | 缓存、JWT 黑名单、登录失败计数          |
| MinIO | `minio/minio`  | 对象存储（文件桶 `learning-materials`） |
| Nginx | `nginx:alpine` | 反向代理 + 静态资源                     |

---

## 项目目录结构

```
campus-learning-assistant/
├── frontend/                       # Vue 3 前端
│   ├── src/
│   │   ├── api/                    # Axios 封装 + API 接口定义（按模块拆分）
│   │   ├── assets/                 # 静态资源、CSS 设计令牌（base.css）
│   │   ├── router/                 # Vue Router，含路由守卫
│   │   ├── stores/                 # Pinia store
│   │   ├── styles/                 # 主题覆盖、过渡动画
│   │   ├── views/                  # 14 个页面级组件（所有 UI 在 views 内，components 为空）
│   │   ├── App.vue                 # 根组件（导航栏、用户状态、提醒下拉）
│   │   └── main.ts                 # 入口
│   ├── package.json                # 脚本与依赖
│   ├── vite.config.ts              # Vite 配置（含 /api → 8080, /ai → 8000 代理）
│   ├── tsconfig.app.json           # TS 应用配置
│   ├── eslint.config.ts            # Flat ESLint（Vue + TS + Oxlint + Prettier）
│   ├── .prettierrc.json            # semi: false, singleQuote: true, printWidth: 100
│   └── .editorconfig               # 2 空格缩进, UTF-8, LF
│
├── backend/
│   ├── java/learning-assistant/    # Spring Boot 项目（Maven）
│   │   ├── src/main/java/com/campus/learning/
│   │   │   ├── config/             # CORS、MinIO、MyBatisPlus、Security、定时任务等配置类
│   │   │   ├── controller/         # REST API 端点（Auth/Material/Plan/Search/Analysis/Admin）
│   │   │   ├── service/            # 服务接口
│   │   │   │   └── Impl/           # 服务实现
│   │   │   ├── entity/             # MyBatis-Plus 实体类
│   │   │   ├── mapper/             # Mapper 接口
│   │   │   ├── dto/                # 数据传输对象（LoginDTO、Result 等）
│   │   │   ├── vo/                 # 视图对象（返回前端的数据结构）
│   │   │   ├── security/           # JWT 工具、认证过滤器、当前用户工具
│   │   │   └── task/               # 定时任务（回收站清理、学习提醒、进度刷新）
│   │   ├── src/main/resources/
│   │   │   ├── application.properties   # 主配置（支持环境变量覆盖）
│   │   │   ├── mapper/             # MyBatis XML Mapper 文件
│   │   │   └── db/study_plan.sql   # 学习计划相关 DDL
│   │   ├── src/test/java/          # 单元测试（Spring Boot Test）
│   │   ├── pom.xml
│   │   └── Dockerfile              # 多阶段构建（Maven 编译 → JRE 运行）
│   │
│   └── python/                     # FastAPI AI 引擎
│       ├── app/
│       │   ├── main.py             # FastAPI 入口
│       │   ├── models/schemas.py   # Pydantic 模型
│       │   ├── routers/            # API 路由（ocr, nlp, enhance, health）
│       │   ├── services/           # 业务逻辑（OcrService, NlpService, ZhipuClient, DualTrack）
│       │   └── utils/preprocessor.py   # 图像预处理（灰度、降噪、二值化）
│       ├── requirements.txt
│       ├── Dockerfile
│       └── venv/                   # Python 虚拟环境（本地开发）
│
├── infra/docker/                   # Docker 部署配置
│   ├── docker-compose.yml          # 6 服务编排（mysql/redis/minio/java-app/python-app/nginx）
│   ├── nginx.conf                  # 反向代理配置（SPA fallback、CORS、100MB body）
│   ├── deploy.sh                   # Ubuntu 一键部署脚本
│   ├── .env.example                # 环境变量模板
│   ├── Dockerfile.local            # Java 本地测试镜像
│   └── init-scripts/               # MySQL 初始化 SQL 脚本（按序号执行）
│       ├── 01-init.sql
│       ├── 02-new-tables.sql
│       ├── 03-study-plan-enhance.sql
│       ├── 04-fulltext-index.sql
│       └── 05-keyword-type-fix.sql
│
│
└── AGENTS.md                       # 本文件
```

---

## 构建与运行命令

### 前端

```bash
cd frontend

# 安装依赖
npm install

# 开发服务器（Vite，默认端口 5173，代理 /api→8080 /ai→8000）
npm run dev

# 生产构建（先类型检查再构建）
npm run build

# 仅构建（跳过类型检查）
npm run build-only

# 类型检查
npm run type-check

# 代码检查与修复
npm run lint          # 依次运行 oxlint + eslint
npm run lint:oxlint   # Oxlint 快速检查
npm run lint:eslint   # ESLint 检查
npm run format        # Prettier 格式化
```

**引擎要求**：Node.js `^20.19.0 || >=22.12.0`

### Java 后端

```bash
cd backend/java/learning-assistant

# 编译并打包（跳过测试）
./mvnw clean package -DskipTests
# 或已安装 Maven 时
mvn clean package -DskipTests

# 运行
java -jar target/learning-assistant-0.0.1-SNAPSHOT.jar

# 运行测试
./mvnw test
```

### Python AI 引擎

```bash
cd backend/python

# 建议先创建虚拟环境
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt

# 启动开发服务器
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# 生产启动
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

**注意**：PaddleOCR 首次运行会自动下载模型文件到 `~/.paddleocr/`。

### 一键启动全部服务（Docker）

```bash
cd infra/docker

# 复制并编辑环境变量
cp .env.example .env
# 编辑 .env：修改密码、JWT_SECRET、ZHIPU_API_KEY

# 一键部署（Ubuntu）
chmod +x deploy.sh
./deploy.sh

# 或手动
docker compose pull mysql redis minio nginx
docker compose build --no-cache java-app python-app
docker compose up -d
```

---

## 代码风格指南

### 前端（Vue 3 + TypeScript）

- 使用 **Composition API** + `<script setup>` 语法
- 路径别名 `@/` 映射到 `./src`
- 不使用分号（Prettier `semi: false`），单引号，`printWidth: 100`
- 2 空格缩进，LF 换行
- CSS 使用原生变量（设计令牌在 `assets/base.css`），无预处理器
- API 层按业务模块拆分（`api/auth.ts`、`api/material.ts` 等），每个文件同时导出请求函数和 TS 接口
- JWT 存 `localStorage`（`token`、`refreshToken`）

### Java（Spring Boot）

- 包结构：`com.campus.learning.{controller|service|entity|mapper|dto|vo|security|config|task}`
- 类名 UpperCamelCase，方法/变量 lowerCamelCase，常量 UPPER_SNAKE_CASE
- 实体类使用 MyBatis-Plus `@TableName` + `@TableId` 注解
- Controller 统一返回 `Result<T>` 包装对象
- Service 采用**接口 + 实现类**模式，事务注解 `@Transactional`
- 密码使用 BCrypt 加密（强度 10）
- 所有配置项支持环境变量覆盖（`application.properties` 中使用 `${VAR:default}`）

### Python（FastAPI）

- 目录：`app/{routers,services,models,utils}/`
- 命名：snake_case，类名 PascalCase
- 强制类型注解，Pydantic 模型校验请求/响应
- 路由处理函数使用 `async def`

### 数据库命名

- 表名：snake_case，复数形式（`users`, `materials`, `study_plans`）
- 字段名：snake_case
- 外键：`{关联表}_id`

---

## 测试策略

本项目目前没有 CI/CD 流水线，测试以**本地/手动**方式运行。

### 1. Java 单元测试

位置：`backend/java/learning-assistant/src/test/java/`

- `LearningAssistantApplicationTests.java` — Spring Boot 上下文加载测试
- `EntityFieldPatchTest.java` — 实体字段补丁测试

运行：

```bash
./mvnw test
```

### 2. Python 回归测试脚本

项目根目录包含多个回归/接口测试脚本，用于验证关键链路：

| 脚本                      | 用途              |
| ------------------------- | ----------------- |
| `regression_test.py`      | 基础接口回归      |
| `regression_full_test.py` | 全量接口回归      |
| `regression_api_test.py`  | API 专项回归      |
| `test_ocr_api.py`         | OCR 接口测试      |
| `test_java_ready.py`      | Java 服务就绪检查 |
| `test_overdue.py`         | 计划逾期检测测试  |


---

## 部署架构

```
用户浏览器
    │
    ▼
  Nginx（端口 80）
    ├── /  → frontend/dist（Vue SPA，try_files fallback index.html）
    ├── /api/  → java-app:8080（Spring Boot）
    └── /ai/   → python-app:8000（FastAPI）

内部服务（Docker 网络 cla-network）：
    ├── mysql:3306
    ├── redis:6379
    ├── minio:9000/9001
    ├── java-app:8080
    └── python-app:8000
```

### 关键环境变量

编辑 `infra/docker/.env` 后再部署：

| 变量                   | 默认值                                             | 说明                                |
| ---------------------- | -------------------------------------------------- | ----------------------------------- |
| `MYSQL_ROOT_PASSWORD`  | `CampusLearning2024`                               | **必须修改**                        |
| `MYSQL_PASSWORD`       | `ClaPassword2024`                                  | **必须修改**                        |
| `MINIO_SECRET_KEY`     | `CampusLearning2024`                               | **必须修改**                        |
| `JWT_SECRET`           | `campus-learning-assistant-jwt-secret-key-32chars` | **必须修改，至少 32 字符**          |
| `ZHIPU_API_KEY`        | —                                                  | 智谱 AI API Key（OCR 增强功能需要） |
| `CORS_ALLOWED_ORIGINS` | `http://localhost`                                 | 前端域名                            |

### 已知部署注意事项

1. **Nginx 卷挂载路径**：`docker-compose.yml` 中 Nginx 的 volume 挂载使用了本地绝对路径，在 Linux 服务器部署前需修改为相对路径或服务器实际路径。
2. **前端构建产物**：Nginx 挂载 `frontend/dist`，部署前必须先执行 `npm run build`。
3. **Java Dockerfile**：多阶段构建，从源码编译，无需预置 JAR。
4. **数据库初始化**：`init-scripts/` 中的 SQL 按文件名排序自动执行，用于建表和增量迁移。

---

## 安全注意事项

1. **认证机制**：JWT Access Token（2 小时 / 记住我 7 天）+ Refresh Token（7 天）。Token 存 Redis，支持黑名单。
2. **登录锁定**：连续 5 次登录失败锁定 30 分钟，计数存 Redis。
3. **密码存储**：BCrypt 哈希，强度 10。
4. **敏感词审核**：OCR 文本命中敏感词库后，`audit_status` 设为 `PENDING`，需管理员审核。
5. **文件上传**：限制 50MB，MD5 去重，存储于 MinIO。
6. **SQL 注入**：MyBatis-Plus 参数绑定防护。
7. **CORS**：Spring Security 配置按环境变量 `CORS_ALLOWED_ORIGINS` 动态设置。
8. **生产安全**：
   - 立即修改所有默认密码
   - 仅开放 80/443，数据库和 Redis 不暴露公网
   - 定期备份 Docker 数据卷

---

## 核心业务逻辑速查

### 双轨 OCR/NLP 架构

```
上传文件 → Java 接收 → 保存 MinIO
    ↓
调用 Python /ai/ocr（PaddleOCR，30 秒超时）
    ↓
计算平均置信度
    ├── confidence > 0.85 → 本地 NLP（Jieba + TextRank）
    └── confidence ≤ 0.85 → 调用智谱 GLM-4V-Flash（5 秒超时熔断）
              ├── 成功 → AI 增强结果
              └── 超时/失败 → 降级本地 NLP
    ↓
保存 OcrResult、Keyword、KnowledgePoint → MySQL
更新 Material 状态为 COMPLETED
```

### 学习计划算法

1. `days = (endDate - startDate) + 1`
2. `totalPages = 所选 Material 页数累加`
3. `dailyPages = ceil(totalPages / days)`
4. 逐日生成 `StudyTask`
5. 打卡进度：`progress = (completedTasks / totalTasks) × 100`

### 回收站机制

- 资料删除为**软删除**（`deleted_at` 字段）
- 定时任务每天扫描，30 天后**物理删除** MinIO 文件和数据库记录
- 支持恢复

---

## 常用开发调试

### 端口速查

| 服务          | 端口 | 说明     |
| ------------- | ---- | -------- |
| Nginx         | 80   | 统一入口 |
| Java          | 8080 | REST API |
| Python        | 8000 | AI 引擎  |
| MySQL         | 3306 | 数据库   |
| Redis         | 6379 | 缓存     |
| MinIO API     | 9000 | 对象存储 |
| MinIO Console | 9001 | 管理界面 |
| Vite Dev      | 5173 | 前端开发 |

### 健康检查端点

- Java：`GET http://localhost:8080/api/auth/login`（允许匿名，返回 200/400/401 均视为存活）
- Python：`GET http://localhost:8000/`
- Nginx：`GET http://localhost/nginx-health`

### 日志查看

```bash
# Java
docker compose logs -f java-app

# Python
docker compose logs -f python-app

# 本地 Java
backend/java/learning-assistant/logs/  # Logback 输出目录
```

---
