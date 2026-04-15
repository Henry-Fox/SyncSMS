# 宝塔部署指南（腾讯云 / Nginx + Spring Boot + Vue）

本文档用于把 SyncSMS 部署到你已有的腾讯云宝塔服务器与已备案域名。

## 1. 数据库准备（MySQL 8.0）

- 在宝塔面板创建数据库：`sync_sms`
- 导入建表脚本：`/sql/init.sql`
- 记录数据库账号/密码/端口

## 2. 后端部署（Spring Boot Jar）

### 2.1 构建

在本地构建：

```bash
cd backend
mvn -DskipTests clean package
```

构建产物：`backend/target/syncsms-backend-1.0.0.jar`

### 2.2 上传与运行

推荐使用宝塔的 **Java 项目管理器**：

- 新建项目：选择 `Jar` 运行方式
- 运行参数建议：
  - `--server.port=8080`
  - `--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/sync_sms?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai`
  - `--spring.datasource.username=<你的账号>`
  - `--spring.datasource.password=<你的密码>`
  - `--syncsms.jwt.secret=<你自定义的长字符串>`

注意：
- `backend/src/main/resources/application.yml` 里的 `spring.datasource.password` 只是占位，线上务必用启动参数/环境变量覆盖。
- 端口默认按计划使用 `8080`，由 Nginx 反代对外提供 HTTPS。

## 3. 前端部署（Vue 静态资源）

### 3.1 构建

```bash
cd frontend
npm install
npm run build
```

构建产物：`frontend/dist/`

### 3.2 上传

把 `dist` 目录内容上传到服务器目录，例如：
- `/www/wwwroot/syncsms-web/`

## 4. Nginx 反向代理配置

在宝塔网站中配置域名与 SSL（Let's Encrypt），然后在配置文件中添加：

```nginx
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    # 宝塔证书路径（示例）
    ssl_certificate     /www/server/panel/vhost/cert/your-domain.com/fullchain.pem;
    ssl_certificate_key /www/server/panel/vhost/cert/your-domain.com/privkey.pem;

    root /www/wwwroot/syncsms-web;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 5. Android APP 配置与使用

### 5.1 后台新增设备生成密钥

- Web 登录管理员账号
- 进入“后台管理 -> 设备管理 -> 新增设备”
- 复制设备密钥（deviceKey）

### 5.2 APP 端填写配置

- 服务器地址：`https://your-domain.com`
- 设备密钥：后台生成的 key

APP 收到短信后将把短信内容上传到后端接口（后续可加 Room 缓存与断网重试）。

## 6. 运行检查清单

- **HTTPS**：浏览器访问 `https://your-domain.com` 正常打开前端
- **后端健康**：确认 `8080` 端口的 Java 服务运行正常（宝塔进程管理）
- **数据库**：`sync_sms` 表已创建，`sys_user` 有 `admin` 初始账号
- **登录**：前端可登录并查看短信列表
- **设备密钥**：设备认证能拿到 token（APP 使用）

