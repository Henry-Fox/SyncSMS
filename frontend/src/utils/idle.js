import { useAuthStore } from '../stores/auth'
import router from '../router'
import { ElMessage } from 'element-plus'

/**
 * @description 空闲自动退出（前端实现）
 * @param {number} idleMs - 空闲毫秒数（默认 30 分钟）
 */
export function setupIdleLogout(idleMs = 30 * 60 * 1000) {
  const auth = useAuthStore()

  const tick = () => {
    if (!auth.isAuthed) return
    const last = auth.lastActiveAt || 0
    if (Date.now() - last > idleMs) {
      auth.logout()
      ElMessage.warning('长时间未操作，已自动退出登录')
      router.replace('/login')
    }
  }

  const onActive = () => {
    if (!auth.isAuthed) return
    auth.touch()
  }

  ;['click', 'keydown', 'mousemove', 'scroll', 'touchstart'].forEach((evt) => {
    window.addEventListener(evt, onActive, { passive: true })
  })

  document.addEventListener('visibilitychange', () => {
    if (!document.hidden) onActive()
  })

  setInterval(tick, 5000)
}

