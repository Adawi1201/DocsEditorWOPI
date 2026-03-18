# Collabora Online 部署指南

## 问题诊断

如果访问 `http://localhost:9980/browser/cool.html` 返回404，说明容器未正确启动。

---

## 解决方案

### 步骤1：停止并删除旧容器

```cmd
docker-compose down
docker rm -f collabora
```

### 步骤2：重新启动

```cmd
docker-compose up -d
```

### 步骤3：等待启动（约60秒）

```cmd
docker-compose logs -f collabora
```

看到类似以下日志表示启动成功：
```
Ready to accept connections
```

### 步骤4：验证服务

```cmd
curl http://localhost:9980/browser/cool.html
```

如果返回HTML内容，说明服务正常。

---

## 常用命令

```cmd
# 启动容器
docker-compose up -d

# 停止容器
docker-compose down

# 查看日志
docker-compose logs -f collabora

# 重启容器
docker-compose restart collabora

# 查看容器状态
docker ps -a | findstr collabora
```

---

## 访问地址

- 管理界面：http://localhost:9980/browser/dist/admin/admin.html
- 用户名：admin
- 密码：admin123
- 编辑器页面：http://localhost:9980/browser/cool.html

---

## 故障排除

### 如果仍然404

尝试手动运行：
```cmd
docker pull collabora/code:24.04.6.5.1
docker run -d -p 9980:9980 --name collabora \
  -e "domain=localhost" \
  -e "username=admin" \
  -e "password=admin123" \
  -e "extra_params=--o:ssl.enable=false --o:net.post_allow=yes" \
  --cap-add MKNOD \
  collabora/code:24.04.6.5.1
```

### 如果端口被占用

```cmd
# 查找占用9980端口的进程
netstat -ano | findstr :9980
# 终止进程
taskkill /PID <进程ID> /F