# SyncSMS

SyncSMS 是一个“手机短信 → Web 可视化查看”的同步系统，适用于**收验证码专用机**场景：手机端持续上报在线/电量并上传收到的短信，Web 端提供短信列表、详情、已读标记与简单后台管理。

## 组成模块

- **Android 客户端**：`android/`
  - 前台服务（常驻通知）上报心跳（电量/在线）
  - 短信上传（Room 队列 + WorkManager 重试）
  - **通知监听**（NotificationListenerService）捕获验证码类短信（华为/荣耀等 ROM 重要）
- **后端**：`backend/`（Spring Boot）
  - 设备认证（deviceKey → JWT）
  - 心跳上报、短信批量上传、短信查询、标记已读
  - 用户登录 + 风控（失败计数、验证码、锁定）+ 审计日志
- **前端**：`frontend/`（Vue）
  - 登录页（按需验证码）
  - 设备在线/电量展示
  - 短信列表（自动刷新）、详情、已读追踪

## 技术栈与版本

请先阅读并遵循：`docs/tech-stack-with-versions.md`

## 快速启动（本地开发）

### 后端

前置：Java 17、MySQL 8.0

1. 创建数据库 `syncsms`
2. 配置数据源（推荐用环境变量）：
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
3. 启动：

```bash
cd backend
mvn spring-boot:run
```

### 前端

前置：Node.js（建议 18/20 LTS）

```bash
cd frontend
npm ci
npm run dev
```

开发环境默认通过 Nginx/代理配置访问 `/api`。

## 宝塔部署（生产）

本项目推荐部署形态：

- Web：`https://<domain>/`（静态站点）
- API：`https://<domain>/api/*`（Nginx 反代到后端 `127.0.0.1:8081`）
- 后端：宝塔 Java 项目管理器托管 jar（常驻/自启）

部署细节请参考：`docs/deploy-bt.md`

## Android 端使用要点

1. 在 App 中设置 `server` 为域名根地址，例如：
   - ✅ `https://syncsms.menghengli.cn`
   - ❌ `https://syncsms.menghengli.cn/api`（会导致路径变成 `/api/api/...`）
2. **务必授予通知监听权限**（否则部分 ROM 无法同步验证码类短信）
3. 建议把 App 加入电池白名单，允许后台运行，避免息屏后被系统杀进程

## 安全建议

- 生产环境务必通过环境变量设置：
  - `SYNCSMS_JWT_SECRET`（足够长的随机字符串）
  - `SPRING_DATASOURCE_*`（不要把数据库密码写死在仓库）
- 建议保留 `admin` 作为兜底管理员账号，但设置强密码，并创建日常管理员账号使用。

