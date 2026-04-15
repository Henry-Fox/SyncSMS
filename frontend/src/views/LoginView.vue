<template>
  <div class="page app-shell">
    <div class="card glass-card">
      <div class="title-row">
        <div class="logo">
          <img class="login-logo-img" src="/logo.png" width="44" height="44" alt="SyncSMS" />
        </div>
        <div>
          <div class="title">SyncSMS</div>
          <div class="sub">短信同步 · 安全查看 · 多人登录</div>
        </div>
      </div>
      <el-form :model="form" @submit.prevent class="form">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" placeholder="密码" show-password size="large" />
        </el-form-item>
        <el-form-item v-if="needCaptcha">
          <div class="captcha">
            <el-input v-model="form.captchaCode" placeholder="验证码" size="large" />
            <div class="captcha-img" @click="refreshCaptcha" title="点击刷新">
              <img v-if="captchaBase64" :src="`data:image/png;base64,${captchaBase64}`" alt="captcha" />
              <div v-else class="captcha-placeholder">获取验证码</div>
            </div>
          </div>
        </el-form-item>
        <el-button type="primary" :loading="loading" size="large" style="width: 100%" @click="onLogin">
          登录
        </el-button>
      </el-form>
      <div class="hint">提示：管理员可进入“后台管理”创建用户与设备密钥。</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const needCaptcha = ref(false)
const captchaId = ref('')
const captchaBase64 = ref('')
const form = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

async function refreshCaptcha() {
  const data = await auth.getCaptcha()
  captchaId.value = data.captchaId
  captchaBase64.value = data.imageBase64
}

async function onLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (needCaptcha.value) {
    if (!captchaId.value || !captchaBase64.value) {
      await refreshCaptcha()
    }
    if (!form.captchaCode) {
      ElMessage.warning('请输入验证码')
      return
    }
  }
  loading.value = true
  try {
    await auth.login({
      username: form.username,
      password: form.password,
      captchaId: needCaptcha.value ? captchaId.value : undefined,
      captchaCode: needCaptcha.value ? form.captchaCode : undefined
    })
    ElMessage.success('登录成功')
    router.replace('/sms')
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || ''
    // 仅在后端明确要求验证码时才展示，避免“看起来不校验”的错觉
    if (msg.includes('验证码')) {
      needCaptcha.value = true
      form.captchaCode = ''
      await refreshCaptcha()
    }
    return
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  height: 100%;
  display: grid;
  place-items: center;
}
.card {
  width: 360px;
  padding: 22px 22px 18px;
}
.title-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
}
.logo {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.25);
  display: grid;
  place-items: center;
}
.login-logo-img {
  width: 44px;
  height: 44px;
  display: block;
  object-fit: cover;
}
.title {
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 0.6px;
  color: rgba(15, 23, 42, 0.95);
}
.sub {
  font-size: 12px;
  margin-top: 2px;
  color: rgba(100, 116, 139, 0.95);
}
.form {
  margin-top: 8px;
}
.captcha {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 140px;
  gap: 10px;
  align-items: center;
}
.captcha-img {
  height: 44px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  display: grid;
  place-items: center;
}
.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.captcha-placeholder {
  font-size: 12px;
  color: rgba(100, 116, 139, 0.95);
}
.hint {
  margin-top: 12px;
  font-size: 12px;
  color: rgba(100, 116, 139, 0.95);
}
</style>

