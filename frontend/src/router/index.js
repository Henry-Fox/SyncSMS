import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const LoginView = () => import('../views/LoginView.vue')
const SmsListView = () => import('../views/SmsListView.vue')
const AdminView = () => import('../views/AdminView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/sms' },
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/sms', component: SmsListView },
    { path: '/admin', component: AdminView, meta: { requireAdmin: true } }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.public) return true
  if (!auth.isAuthed) return '/login'
  if (to.meta.requireAdmin && !auth.isAdmin) return '/sms'
  return true
})

export default router

