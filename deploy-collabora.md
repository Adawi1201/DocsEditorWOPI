# Collabora Online 部署指南

## 快速启动（推荐）

使用 docker-compose 快速启动：

```bash
docker-compose up -d
```

## 手动启动

使用 docker 命令直接运行：

```bash
docker pull collabora/code:latest
docker run -d -p 9980:9980 --name collabora \
  -e "domain=localhost" \
  -e "username=admin" \
  -e "password=admin123" \
  -e "extra_params=--o:ssl.enable=false" \
  --restart unless-stopped \
  collabora/code
```

## 访问地址

- 管理界面：http://localhost:9980/browser/dist/admin/admin.html
- 用户名：admin
- 密码：admin123

## 常用命令

```bash
# 启动容器
docker-compose up -d

# 停止容器
docker-compose down

# 查看日志
docker-compose logs -f collabora

# 重启容器
docker-compose restart collabora