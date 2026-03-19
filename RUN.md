# 在线文档编辑系统运行指南

本项目实现了一个基于 Collabora Online + Vue + Spring Boot 的在线文档编辑功能。

## 技术架构

| 组件 | 端口 | 说明 |
|------|------|------|
| Vue 前端 | 5231 | 用户界面 |
| Spring Boot 后端 | 8808 | WOPI 主机接口 |
| Collabora Online | 9980 | 文档编辑引擎 |

## 首次配置（重要）

### 1. 创建环境变量文件

复制 `.env.example` 为 `.env` 并配置实际值：

```bash
# Windows
copy .env.example .env

# Linux/Mac
cp .env.example .env
```

### 2. 必须修改的配置项

打开 `.env` 文件，修改以下必填项：

```env
# 数据库密码 - 必须修改为强密码
DB_PASSWORD=your_secure_password_here

# WOPI 访问密钥 - 必须修改为长随机字符串（建议32位以上）
WOPI_ACCESS_TOKEN_SECRET=CHANGE_ME_TO_A_SECURE_RANDOM_STRING

# Collabora 管理员密码 - 必须修改为强密码
COLLABORA_PASSWORD=CHANGE_ME_TO_SECURE_PASSWORD
```

### 3. 启动 MySQL 数据库

确保 MySQL 服务运行在 3306 端口，数据库名称为 `docs_online`：

```sql
CREATE DATABASE IF NOT EXISTS docs_online DEFAULT CHARACTER SET utf8mb4;
```

## 快速启动

### 1. 启动后端（方式一：使用 Maven）

```bash
cd D:/ProJect/Docs4jcolla

# Windows PowerShell - 设置环境变量后启动
$env:DB_PASSWORD="your_password"; $env:WOPI_ACCESS_TOKEN_SECRET="your_secret"; mvnw spring-boot:run -DskipTests

# Windows CMD
set DB_PASSWORD=your_password && set WOPI_ACCESS_TOKEN_SECRET=your_secret && mvnw spring-boot:run -DskipTests
```

### 2. 启动后端（方式二：使用 IDE）

在 IntelliJ IDEA 或 Eclipse 中配置环境变量后运行 `Docs4jcollaApplication`。

### 3. 启动 Vue 前端

```bash
cd D:/ProJect/Docs4jcolla/vue-4jcolla
npm install
npm run dev
```

前端启动后访问 http://localhost:5231

### 4. 启动 Collabora Online

使用 Docker Compose 启动（会自动读取 .env 文件中的配置）：

```bash
docker-compose -f compose.yaml up -d
```

## 功能使用

1. 打开浏览器访问 http://localhost:5231
2. 点击"选择文件上传"按钮，上传一个 .docx 文档
3. 上传成功后，点击文档卡片上的"编辑"按钮
4. 系统会调用后端生成 WOPI URL，并打开 Collabora 编辑器
5. 在 Collabora 中编辑文档后，保存会自动同步到后端

## API 接口

### 后端 API (端口 8808)

- `GET /api/docs` - 获取文档列表
- `POST /api/docs/upload` - 上传新文档
- `GET /api/docs/{id}/edit` - 获取编辑 URL
- `DELETE /api/docs/{id}` - 删除文档

### WOPI 接口 (端口 8808)

- `GET /wopi/files/{fileId}` - 获取文件元数据
- `GET /wopi/files/{fileId}/contents` - 获取文件内容
- `POST /wopi/files/{fileId}/contents` - 保存文件内容

## 配置说明

### 环境变量配置 (.env)

所有敏感配置都通过环境变量管理，详见 `.env.example` 文件：

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
| `CORS_ORIGINS` | 允许的 CORS 来源 | 见 .env.example |

### 前端代理配置 (vite.config.js)

开发服务器代理从环境变量读取配置：
- `/cool`, `/lool` → Collabora 服务
- `/api` → 后端 API
- `/wopi` → 后端 WOPI 接口

### WOPI Token 验证

系统使用 SHA256 签名验证：
- Token 格式: `SHA256("{docId}:{user}:{secret}")`
- 密钥从 `WOPI_ACCESS_TOKEN_SECRET` 环境变量读取
- 实现见 `WopiTokenFilter.java`

## 注意事项

1. 确保端口 5231、8808、9980 未被占用
2. **首次使用必须修改 `.env` 中的敏感配置**
3. 上传的文档保存在后端配置的 `./docs` 目录下
4. Collabora 需要较长时间启动，首次访问可能需要等待 30-60 秒
5. `.env` 文件不应提交到版本控制系统（已在 .gitignore 中排除）

## 安全建议

1. **生产环境** - 不要使用 `.env` 文件，使用专业的密钥管理服务
2. **WOPI_SECRET** - 使用至少 32 位的随机字符串
3. **数据库密码** - 使用强密码，定期更换
4. **Collabora 密码** - 使用强密码，避免使用默认密码
5. **CORS 配置** - 生产环境应限制为实际的前端域名
