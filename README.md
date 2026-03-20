# DocsOnline Django - 在线文档编辑系统 (Python/Django 版)

基于 WOPI 协议与 Collabora Online 实现的在线文档编辑系统，使用 Django 框架构建。

[![MIT License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 项目简介

DocsOnline Django 是 Docs4jcolla 项目的 Python/Django 实现版本，使用 Django 5.2 框架构建后端服务，通过 WOPI（Web Application Open Platform Interface）协议与 Collabora Online 文档引擎进行通信。

### 主要特性

- 支持 `.docx`、`.xlsx` 等格式文档的在线编辑
- 实时保存文档内容
- 基于 WOPI 协议的安全文件访问
- RESTful API 设计
- Docker 容器化部署支持

## 技术架构

```
┌─────────────┐     ┌─────────────┐     ┌─────────────────┐
│   Vue.js   │────▶│   Django    │────▶│ Collabora Online│
│   前端      │     │  WOPI API   │     │   文档引擎       │
│  (5231)     │     │  (8000)     │     │   (9980)        │
└─────────────┘     └─────────────┘     └─────────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │   MySQL     │
                    │  (3306)     │
                    └─────────────┘
```

### 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Django | 5.2+ |
| Python | Python | 3.10+ |
| 数据库 | MySQL | 8.x |
| CORS 支持 | django-cors-headers | 4.0+ |
| Office 库 | python-docx | 1.1+ |
| Excel 库 | openpyxl | 3.1+ |
| 文档引擎 | Collabora Online | 24.04 |
| 协议 | WOPI | 2.1 |

## 项目结构

```
docs-editor-wopi/
├── DocsOnlineTest/              # Django 项目配置
│   ├── settings.py               # 项目设置
│   ├── urls.py                   # 根 URL 配置
│   ├── wsgi.py                   # WSGI 配置
│   └── asgi.py                  # ASGI 配置
├── wopi/                        # WOPI 应用
│   ├── views.py                 # WOPI 视图实现
│   ├── models.py                # Document 模型
│   ├── urls.py                  # WOPI URL 路由
│   └── debug_urls.py            # 调试用 URL
├── media/                       # 上传文件存储目录
├── manage.py                    # Django 管理脚本
├── requirements.txt             # Python 依赖
├── mysql_config.py              # MySQL 配置
├── compose.yaml                 # Docker Compose 配置
└── README-PY.md                 # 本文件
```

## 快速开始

### 环境要求

- Python 3.10+
- MySQL 8.0+
- Docker (用于运行 Collabora Online)

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 配置环境变量

```bash
# 数据库配置
export DB_NAME=docs_online
export DB_HOST=localhost
export DB_PORT=3306
export DB_USER=root
export DB_PASSWORD=your_password

# Django 配置
export SECRET_KEY=your-secret-key-change-in-production
export DEBUG=True

# WOPI 配置
export WOPI_SECRET=your-secure-random-string
export WOPI_EXTERNAL_URL=http://localhost:8000

# Collabora 配置
export COLLABORA_URL=http://localhost:9980
```

### 3. 初始化数据库

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS docs_online DEFAULT CHARACTER SET utf8mb4;"

# 执行数据库迁移
python manage.py migrate
```

### 4. 启动后端服务

```bash
# 开发环境
python manage.py runserver 0.0.0.0:8000

# 生产环境 (使用 Gunicorn)
gunicorn DocsOnlineTest.wsgi:application --bind 0.0.0.0:8000
```

### 5. 启动 Collabora Online

```bash
docker-compose -f compose.yaml up -d
```

## API 接口

### WOPI 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/wopi/files/<doc_id>` | 获取文件元数据 (CheckFileInfo) |
| GET | `/wopi/files/<doc_id>/contents` | 获取文件内容 (GetFile) |
| POST | `/wopi/files/<doc_id>/contents` | 保存文件内容 (PutFile) |
| POST | `/wopi/files/<doc_id>/rename` | 重命名文件 |
| POST | `/wopi/files/<doc_id>/lock` | 锁定文件 |
| POST | `/wopi/files/<doc_id>/unlock` | 解锁文件 |

### 文档管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/wopi/documents/` | 获取文档列表 |
| POST | `/wopi/documents/upload` | 上传新文档 |
| GET | `/wopi/documents/<doc_id>` | 获取文档详情 |
| DELETE | `/wopi/documents/<doc_id>` | 删除文档 |
| GET | `/wopi/documents/<doc_id>/wopi-url` | 获取 WOPI 编辑 URL |

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DB_NAME` | 数据库名称 | docs_online |
| `DB_HOST` | 数据库地址 | localhost |
| `DB_PORT` | 数据库端口 | 3306 |
| `DB_USER` | 数据库用户名 | root |
| `DB_PASSWORD` | 数据库密码 | **必须设置** |
| `SECRET_KEY` | Django 密钥 | **必须修改** |
| `DEBUG` | 调试模式 | True |
| `WOPI_SECRET` | WOPI 密钥 | **必须修改** |
| `WOPI_EXTERNAL_URL` | WOPI 外部访问地址 | http://localhost:8000 |
| `COLLABORA_URL` | Collabora 服务地址 | http://localhost:9980 |

### Django 设置文件

主要配置项位于 [`DocsOnlineTest/settings.py`](DocsOnlineTest/settings.py)：

```python
# 数据库配置
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.mysql',
        'NAME': os.environ.get('DB_NAME', 'docs_online'),
        'USER': os.environ.get('DB_USER', 'root'),
        'PASSWORD': os.environ.get('DB_PASSWORD', ''),
        'HOST': os.environ.get('DB_HOST', 'localhost'),
        'PORT': os.environ.get('DB_PORT', '3306'),
    }
}

# Collabora 配置
COLLABORA_URL = os.environ.get('COLLABORA_URL', 'http://localhost:9980')
WOPI_SECRET = os.environ.get('WOPI_SECRET', 'your-secret-key-change-in-production')

# CORS 配置
CORS_ALLOW_ALL_ORIGINS = True  # 开发环境
```

### WOPI Token 安全机制

系统使用 SHA256 签名验证 WOPI 访问令牌：

- Token 生成: `SHA256("{docId}:{userId}:{secret}")`
- 详见 [`wopi/views.py`](wopi/views.py) 中的 `generate_access_token` 函数

## Document 模型

```python
class Document(models.Model):
    id = models.UUIDField(primary_key=True)      # 文档 ID (UUID)
    filename = models.CharField(max_length=255)   # 文件名
    document_type = models.CharField(...)          # 文档类型 (word/excel/other)
    file_path = models.CharField(max_length=500)  # 文件存储路径
    file_size = models.BigIntegerField()          # 文件大小
    version = models.IntegerField()                # 版本号
    owner = models.CharField(max_length=100)      # 所有者
    is_locked = models.BooleanField()             # 是否锁定
    lock_token = models.CharField(...)            # 锁定令牌
    created_at = models.DateTimeField()           # 创建时间
    updated_at = models.DateTimeField()           # 更新时间
```

## Docker 部署

### 使用 Docker Compose

```bash
docker-compose -f compose.yaml up -d
```

### 手动 Docker 部署

```dockerfile
# Dockerfile 示例
FROM python:3.10-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY . .
EXPOSE 8000
CMD ["gunicorn", "DocsOnlineTest.wsgi:application", "--bind", "0.0.0.0:8000"]
```

## 安全建议

1. **生产环境** - 修改 `SECRET_KEY` 为强随机字符串
2. **WOPI_SECRET** - 使用至少 32 位的随机字符串
3. **数据库密码** - 使用强密码，定期更换
4. **DEBUG** - 生产环境务必设置为 `False`
5. **CORS** - 生产环境应限制为实际的前端域名

```python
# 生产环境 CORS 配置示例
CORS_ALLOWED_ORIGINS = [
    "https://your-frontend-domain.com",
]
CORS_ALLOW_ALL_ORIGINS = False
```

## 与 Spring Boot 版本对比

| 特性 | Spring Boot 版 | Django 版 |
|------|---------------|-----------|
| 框架 | Spring Boot 4.0 | Django 5.2 |
| ORM | MyBatis | Django ORM |
| 配置 | YAML + @ConfigurationProperties | settings.py |
| WOPI 过滤器 | WopiTokenFilter | Django 视图装饰器 |
| 数据库迁移 | init_db.sql | Django migrations |
| 部署 | JAR 包 | WSGI/Gunicorn |

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
- [Django 文档](https://docs.djangoproject.com/)
- [django-cors-headers](https://github.com/adamchainz/django-cors-headers)
- [python-docx 文档](https://python-docx.readthedocs.io/)
