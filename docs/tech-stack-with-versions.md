# SyncSMS 技术栈与版本（固化）

> 本文用于固化项目技术栈与版本。后续开发如需升级大版本，必须同步更新本文档。

## 总览

- **前端**：Vue 3 + Vite + Element Plus + Axios + MockJS（测试数据）
- **后端**：Spring Boot + Spring Security + JWT + MyBatis-Plus + Flyway
- **数据库**：MySQL 8.0
- **安卓端**：Kotlin + Foreground Service + WorkManager + Room + NotificationListenerService
- **部署**：宝塔（Nginx 反代 + HTTPS + Java 项目管理器）

## 前端（frontend）

- **Node.js**：建议 18 LTS 或 20 LTS（以 CI/服务器实际安装为准）
- **Vue**：3.x（Composition API）
- **Vite**：6.4.2（构建日志显示）
- **UI**：Element Plus（版本以 `frontend/package.json` 为准）
- **HTTP**：axios（版本以 `frontend/package.json` 为准）
- **Mock**：mockjs（测试数据使用；生产环境关闭）

## 后端（backend）

- **Java**：17（宝塔部署使用 JDK 17；Spring Boot 3.x 要求 Java 17+）
- **Spring Boot**：3.5.12（Maven 打包日志显示）
- **Spring Security**：随 Spring Boot 3.5.12 BOM
- **JWT**：项目内 `JwtUtil`（实现依赖以 `backend/pom.xml` 为准）
- **MyBatis-Plus**：版本以 `backend/pom.xml` 为准
- **Flyway**：11.7.2（启动日志显示）
- **MySQL Connector/J**：9.6.0（启动日志显示；本地也可能存在 8.0.33）

## 数据库

- **MySQL**：8.0（宝塔软件商店常见版本）
- **迁移**：Flyway `backend/src/main/resources/db/migration`

## 安卓端（android）

- **语言**：Kotlin
- **后台运行**：Foreground Service（常驻通知） + WorkManager（重试/离线）
- **本地存储**：Room（待上传短信队列，去重）
- **短信获取**：
  - `content://sms` 轮询（在部分 ROM 受限）
  - **NotificationListenerService** 监听短信通知（华为/荣耀上用于捕获验证码类短信）

## 部署与访问约定

- **域名**：`syncsms.menghengli.cn`
- **Web**：`https://syncsms.menghengli.cn/`
- **API**：`https://syncsms.menghengli.cn/api/*`（Nginx 反代到后端 `127.0.0.1:8081`）

# SyncSMS 技术栈与版本

> 本文档为项目技术栈的唯一权威来源，所有开发工作必须遵循此文档中的技术选型和版本约束。

## 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | LTS 版本 |
| Spring Boot | 3.5.12 | 主框架 |
| Spring Security | 6.x (随 Spring Boot) | 安全框架 |
| MyBatis-Plus | 3.5.16 | ORM 增强框架 |
| MySQL Connector/J | 8.x (随 Spring Boot) | 数据库驱动 |
| JJWT | 0.12.6 | JWT Token 生成与验证 |
| Lombok | 1.18.x (随 Spring Boot) | 简化 Java 代码 |
| Maven | 3.9+ | 构建工具 |

## 前端 (Web)

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5.x | 前端框架 |
| Vite | 6.x | 构建工具 |
| Element Plus | 2.13.x | UI 组件库 |
| Vue Router | 4.x | 路由管理 |
| Axios | 1.x | HTTP 请求库 |
| Pinia | 2.x | 状态管理 |

## Android APP

| 技术 | 版本 | 说明 |
|------|------|------|
| Kotlin | 2.0.x | 开发语言 |
| Android SDK | minSdk 26 / targetSdk 34 | Android 8.0+ |
| Retrofit | 2.11.x | HTTP 客户端 |
| OkHttp | 4.12.x | 网络层 |
| Room | 2.6.x | 本地数据库（缓存未上传短信） |
| Gradle | 8.x | 构建工具 |
| AGP (Android Gradle Plugin) | 8.x | Android 构建插件 |

## 数据库

| 技术 | 版本 | 说明 |
|------|------|------|
| MySQL | 8.0 | 关系型数据库 |

## 部署环境

| 技术 | 说明 |
|------|------|
| 腾讯云服务器 | 宝塔面板管理 |
| Nginx | 反向代理 + 静态资源 |
| 宝塔 Java 项目管理器 | 运行 Spring Boot jar |
| SSL 证书 | 宝塔免费 Let's Encrypt |

## 参考文档

- Spring Boot: https://docs.spring.io/spring-boot/
- MyBatis-Plus: https://baomidou.com/
- Vue 3: https://vuejs.org/
- Element Plus: https://element-plus.org/
- Kotlin: https://kotlinlang.org/
- Android Developer: https://developer.android.com/
