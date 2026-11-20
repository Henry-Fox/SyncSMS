# SyncSMS 自动化发布（云效 Codeup + Flow）

目标：工程师只改代码、在云效点「运行」，服务器上的服务自动换版本；工程师**不需要**登录服务器、宝塔或阿里云账号。

## 链路

```
Codeup(syncsms master) → 云效流水线 → 构建出制品(syncsms-package.tgz)
                                        ↓
                              云效主机组 Runner（装在 39.107.13.114）
                                        ↓
                              /www/wwwroot/syncsms/deploy-syncsms.sh
                                        ↓
                              systemd 重启 syncsms，健康检查失败自动回滚
```

## 制品约定（必须严格一致）

`syncsms-package.tgz` 解包后必须包含：

```
app.jar            # 后端 fat jar: backend/target/syncsms-backend-1.0.0.jar
web/index.html     # 前端构建产物: frontend/dist/
web/assets/...
web/logo.png
```

打包命令（在流水线构建步骤里执行）：

```bash
# 后端
cd backend && mvn -DskipTests -B clean package
# 前端
cd ../frontend && npm ci && npm run build
# 组制品
cd .. && rm -rf out && mkdir -p out/web
cp -r frontend/dist/* out/web/
cp backend/target/syncsms-backend-1.0.0.jar out/app.jar
tar -czf syncsms-package.tgz -C out .
```

> 注意：`web/` 必须是**真实文件**，空目录会导致部署脚本直接中止（防呆校验）。

## 流水线步骤（云效 Flow 页面配置）

1. **流水线源**：Codeup / `cgtnc/syncsms` / `master`，触发方式选「代码提交自动触发」+ 手动。
2. **构建**（Linux 构建集群，JDK 17 + Node 20 镜像）：
   - 执行上面的打包命令
   - 产出物：`syncsms-package.tgz`
3. **主机部署**：
   - 制品：上一步的 `syncsms-package.tgz`
   - 部署脚本：`deploy/deploy-syncsms.sh`
   - 部署参数（脚本按位置入参）：
     ```
     /www/wwwroot/syncsms $ARTIFACT_PATH
     ```
   - 目标主机组：需先在「主机组」里建好并装上 Runner（见下）

## 服务器侧一次性的东西（已就绪，不用再动）

| 项 | 位置 | 状态 |
|---|---|---|
| 运行目录 | `/www/wwwroot/syncsms/` | `current` 软链 + `releases/` |
| 发布脚本 | `/www/wwwroot/syncsms/deploy-syncsms.sh` | 已存在 |
| systemd 单元 | `/etc/systemd/system/syncsms.service` | `enabled`，`active` |
| 数据库凭据 | `/root/.syncsms-db.env` (600) | MariaDB `sync_sms` |
| 后端端口 | `127.0.0.1:8081` | 只监听本机 |
| 对外入口 | nginx `sms.cgtnc-ditd.top` | 静态 + `/api` 反代 |

发布脚本会：建 `releases/<时间戳>` → 解包 → 校验 `app.jar` + `web/index.html` → 原子切 `current` → 重启服务 → 轮询 `/api/user/captcha` 最多 60s → 失败自动回滚上一版 → 只保留最近 5 版。

## 工程师权限边界

- **给**：Codeup 仓库开发者权限（可推 `master` 或走分支合并）、仓库 SSH key。
- **不给**：云效个人访问令牌（能改流水线 = 能改部署脚本 = root）、服务器 root 私钥、宝塔账号、阿里云 RAM。
- 工程师只能通过「推代码 → 流水线自动跑」发布，接触不到服务器。

## 手工兜底

任何一步卡住时，运维可在本地直接执行（绕过流水线）：

```powershell
scp -i $env:USERPROFILE\.ssh\BAOTA_id_rsa.pem syncsms-package.tgz root@39.107.13.114:/tmp/
ssh -i $env:USERPROFILE\.ssh\BAOTA_id_rsa.pem root@39.107.13.114 "bash /www/wwwroot/syncsms/deploy-syncsms.sh /www/wwwroot/syncsms /tmp/syncsms-package.tgz"
```
