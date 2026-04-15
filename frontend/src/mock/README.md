# Mock 使用说明

本项目前端开发可使用 mockjs 提供测试数据。

## 启用方式（默认已关闭）

1. 在 `src/main.js` 中恢复：`import { setupMock } from './mock'` 与 `setupMock()`。
2. 将 `.env.development` 中 `VITE_USE_MOCK` 设为 `true`，或启动时设置环境变量：

- Windows PowerShell：`$env:VITE_USE_MOCK="true"; npm run dev`

## 说明

- 启用后，会拦截 `/api/*` 的请求并返回模拟数据。
- 关闭后，`/api` 通过 `vite.config.js` 代理到后端（默认 `http://localhost:8081`）。

