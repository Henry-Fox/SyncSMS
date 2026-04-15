import { defineStore } from 'pinia'
import { getCaptcha as getCaptchaApi, login as loginApi, logout as logoutApi } from '../api/auth'

/**
 * @description 鉴权状态管理
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || '',
    nickname: localStorage.getItem('nickname') || '',
    role: localStorage.getItem('role') || '',
    lastActiveAt: Number(localStorage.getItem('lastActiveAt') || Date.now())
  }),
  getters: {
    isAuthed: (s) => !!s.token,
    isAdmin: (s) => s.role === 'admin'
  },
  actions: {
    /**
     * @description 登录并写入本地缓存
     * @param {{username:string,password:string}} payload
     */
    async login(payload) {
      const res = await loginApi(payload)
      const data = res.data
      this.token = data.token
      this.username = data.username
      this.nickname = data.nickname || ''
      this.role = data.role || 'viewer'
      this.lastActiveAt = Date.now()
      localStorage.setItem('token', this.token)
      localStorage.setItem('username', this.username)
      localStorage.setItem('nickname', this.nickname)
      localStorage.setItem('role', this.role)
      localStorage.setItem('lastActiveAt', String(this.lastActiveAt))
    },
    async logout() {
      try {
        if (this.token) await logoutApi()
      } catch (e) {
        // 忽略注销失败（例如网络中断）；本地仍清理 token
      }
      this.token = ''
      this.username = ''
      this.nickname = ''
      this.role = ''
      this.lastActiveAt = Date.now()
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('nickname')
      localStorage.removeItem('role')
      localStorage.removeItem('lastActiveAt')
    },
    touch() {
      this.lastActiveAt = Date.now()
      localStorage.setItem('lastActiveAt', String(this.lastActiveAt))
    },
    async getCaptcha() {
      const res = await getCaptchaApi()
      return res.data
    }
  }
})

