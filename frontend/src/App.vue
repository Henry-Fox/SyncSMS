<template>
  <router-view />
</template>

<script setup>
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC',
    'Microsoft YaHei', sans-serif;
}

body {
  background: #0b1220;
}

:root {
  --app-bg: #0b1220;
  --panel-bg: rgba(255, 255, 255, 0.92);
  --panel-border: rgba(255, 255, 255, 0.18);
  --text-strong: #0f172a;
  --text-muted: #64748b;
}

.app-shell {
  min-height: 100%;
  background: radial-gradient(1200px 600px at 10% 10%, rgba(59, 130, 246, 0.18), transparent 60%),
    radial-gradient(900px 500px at 80% 20%, rgba(16, 185, 129, 0.14), transparent 55%),
    radial-gradient(900px 500px at 50% 90%, rgba(168, 85, 247, 0.12), transparent 60%),
    var(--app-bg);
}

.app-topbar {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  color: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.04));
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.app-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 900;
  letter-spacing: 0.6px;
}

.app-brand-logo {
  width: 28px;
  height: 28px;
  border-radius: 10px;
  display: block;
  object-fit: contain;
  box-shadow: 0 12px 26px rgba(0, 0, 0, 0.25);
}

.app-content {
  padding: 18px;
  max-width: 1200px;
  margin: 0 auto;
}

.glass-card {
  background: var(--panel-bg);
  border: 1px solid var(--panel-border);
  border-radius: 16px;
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.35);
}
</style>
