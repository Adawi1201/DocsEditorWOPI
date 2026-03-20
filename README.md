# Docs4jcolla - 在线文档编辑系统

基于 WOPI 协议与 Collabora Online 实现的在线文档编辑系统。

[![MIT License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 项目简介

Docs4jcolla 是一个开源的在线文档编辑解决方案，使用 Spring Boot 构建后端服务，通过 WOPI（Web Application Open Platform Interface）协议与 Collabora Online 文档引擎进行通信，前端采用 Vue.js 构建用户界面。

### 主要特性

- 支持 `.docx` 格式文档的在线编辑
- 实时保存文档内容
- 基于 WOPI 协议的安全文件访问
- 直观的 Web 用户界面
- Docker 容器化部署支持

## 技术架构

```
┌─────────────┐     ┌─────────────┐     ┌─────────────────┐
│   Vue.js    │────▶│ Spring Boot │────▶│ Collabora Online │
│   前端      │     │  WOPI 主机  │     │   文档引擎       │
│  (5231)    │     │  (8808)     │     │   (9980)        │
└─────────────┘     └─────────────┘     └─────────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │   MySQL    │
                    │  (3306)    │
                    └─────────────┘
```

### 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 4.0.3 |
| 数据库 | MySQL | 8.x |
| ORM | MyBatis | 3.0.3 |
| 前端框架 | Vue.js | 3.x |
| 构建工具 | Vite | - |
| 文档引擎 | Collabora Online | 24.04 |
| 协议 | WOPI | 2.1 |

## 项目结构

```
docs-editor-wopi/
├── src/main/java/com/docmindfusion/onlinedoc4j/docs4jcolla/
│   ├── config/              # 配置类
│   │   ├── CorsConfig.java       # CORS 跨域配置
│   │   ├── MyBatisConfig.java    # MyBatis 配置
│   │   └── WopiTokenFilter.java  # WOPI 令牌验证过滤器
│   ├── controller/          # 控制器
│   │   ├── DocumentController.java  # 文档管理接口
│   │   └── WopiController.java      # WOPI 协议接口
│   ├── entity/              # 实体类
│   │   └── WopiDocument.java       # 文档实体
│   ├── mapper/              # MyBatis Mapper
│   │   └── WopiDocumentMapper.java
│   └── Docs4jcollaApplication.java  # 应用入口
├── src/main/resources/
│   ├── application.yml      # 应用配置
│   └── mapper/              # MyBatis XML 映射文件
├── vue-4jcolla/             # Vue.js 前端项目
│   ├── src/
│   │   ├── views/
│   │   │   ├── DocList.vue     # 文档列表页面
│   │   │   └── Editor.vue      # 编辑器页面
│   │   ├── api/               # API 调用
│   │   └── router/            # 路由配置
│   └── vite.config.js        # Vite 配置
├── compose.yaml              # Docker Compose 配置
├── init_db.sql               # 数据库初始化脚本
└── .env.example              # 环境变量示例
```

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Docker (用于运行 Collabora Online)

### 1. 配置环境变量

复制环境变量示例文件并修改必要配置：

```bash
# Windows
copy .env.example .env

# Linux/Mac
cp .env.example .env
```

编辑 `.env` 文件，修改以下必填项：

```env
# 数据库密码 - 必须修改为强密码
DB_PASSWORD=your_secure_password_here

# WOPI 访问密钥 - 必须修改为长随机字符串（建议32位以上）
WOPI_ACCESS_TOKEN_SECRET=CHANGE_ME_TO_A_SECURE_RANDOM_STRING

# Collabora 管理员密码 - 必须修改为强密码
COLLABORA_PASSWORD=CHANGE_ME_TO_SECURE_PASSWORD
```

### 2. 初始化数据库

创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS docs_online DEFAULT CHARACTER SET utf8mb4;
```

或使用初始化脚本：

```bash
mysql -u root -p < init_db.sql
```

### 3. 启动后端服务

使用 Maven 启动：

```bash
# Windows CMD
set DB_PASSWORD=your_password && set WOPI_ACCESS_TOKEN_SECRET=your_secret && mvnw spring-boot:run -DskipTests

# Windows PowerShell
$env:DB_PASSWORD="your_password"; $env:WOPI_ACCESS_TOKEN_SECRET="your_secret"; mvnw spring-boot:run -DskipTests

# Linux/Mac
export DB_PASSWORD=your_password WOPI_ACCESS_TOKEN_SECRET=your_secret && ./mvnw spring-boot:run -DskipTests
```

### 4. 启动前端

```bash
cd vue-4jcolla
npm install
npm run dev
```

### 5. 启动 Collabora Online

```bash
docker-compose -f compose.yaml up -d
```

### 6. 访问应用

打开浏览器访问 http://localhost:5231

## API 接口

### 文档管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/docs` | 获取文档列表 |
| POST | `/api/docs/upload` | 上传新文档 |
| GET | `/api/docs/{id}/edit` | 获取编辑 URL |
| DELETE | `/api/docs/{id}` | 删除文档 |

### WOPI 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/wopi/files/{fileId}` | 获取文件元数据 |
| GET | `/wopi/files/{fileId}/contents` | 获取文件内容 |
| POST | `/wopi/files/{fileId}/contents` | 保存文件内容 |

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DB_HOST` | 数据库地址 | localhost |
| `DB_PORT` | 数据库端口 | 3306 |
| `DB_NAME` | 数据库名称 | docs_online |
| `DB_USERNAME` | 数据库用户名 | root |
| `DB_PASSWORD` | 数据库密码 | **必须修改** |
| `SERVER_PORT` | 后端服务端口 | 8808 |
| `WOPI_ACCESS_TOKEN_SECRET` | WOPI 令牌密钥 | **必须修改** |
| `WOPI_DEFAULT_USER` | 默认用户名 | admin |
| `COLLABORA_HOST` | Collabora 服务地址 | http://localhost:9980 |
| `COLLABORA_USERNAME` | Collabora 用户名 | admin |
| `COLLABORA_PASSWORD` | Collabora 密码 | **必须修改** |
| `VITE_FRONTEND_PORT` | 前端端口 | 5231 |

### WOPI Token 安全机制

系统使用 SHA256 签名验证 WOPI 访问令牌：

- Token 格式: `SHA256("{docId}:{user}:{secret}")`
- 密钥从 `WOPI_ACCESS_TOKEN_SECRET` 环境变量读取
- 详见 [`WopiTokenFilter.java`](src/main/java/com/docmindfusion/onlinedoc4j/docs4jcolla/config/WopiTokenFilter.java)

## 使用流程

1. 打开浏览器访问 http://localhost:5231
2. 点击"选择文件上传"按钮，上传一个 `.docx` 文档
3. 上传成功后，点击文档卡片上的"编辑"按钮
4. 系统调用后端生成 WOPI URL 并打开 Collabora 编辑器
5. 在 Collabora 中编辑文档后，保存会自动同步到后端

## 安全建议

1. **生产环境** - 不要使用 `.env` 文件，使用专业的密钥管理服务
2. **WOPI_ACCESS_TOKEN_SECRET** - 使用至少 32 位的随机字符串
3. **数据库密码** - 使用强密码，定期更换
4. **Collabora 密码** - 使用强密码，避免使用默认密码
5. **CORS 配置** - 生产环境应限制为实际的前端域名

## 注意事项

1. 确保端口 5231、8808、9980、3306 未被占用
2. **首次使用必须修改 `.env` 中的敏感配置**
3. 上传的文档保存在后端配置的 `./docs` 目录下
4. Collabora 需要较长时间启动，首次访问可能需要等待 30-60 秒
5. `.env` 文件不应提交到版本控制系统（已在 `.gitignore` 中排除）

## 许可证

本项目采用 [MIT 许可证](LICENSE)。

```
MIT License

Copyright (c) 2024 DocMindFusion

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 相关资源

- [WOPI 协议文档](https://learn.microsoft.com/en-us/microsoft-365/cloud-storage-partner-program/online/)
- [Collabora Online 文档](https://www.collaboraoffice.com/online/)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [Vue.js 文档](https://vuejs.org/)
