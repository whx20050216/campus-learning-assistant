import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/search',
      name: 'search',
      component: () => import('../views/SearchView.vue')
    },
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
    },
    {
      path: '/upload',
      name: 'upload',
      component: () => import('../views/UploadView.vue')
    },
    {
      path: '/plans',
      name: 'PlanView',
      component: () => import('../views/PlanView.vue')
    },
    {
      path: '/plans/:id',
      name: 'PlanDetail',
      component: () => import('../views/PlanDetailView.vue')
    },
    {
      path: '/analysis',
      name: 'Analysis',
      component: () => import('../views/AnalysisView.vue')
    },
    {
      path: '/admin',
      name: 'Admin',
      component: () => import('../views/AdminView.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue')
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue')
    },
    {
      path: '/materials',
      name: 'MaterialList',
      component: () => import('../views/MaterialListView.vue')
    },
    {
      path: '/materials/:id',
      name: 'MaterialDetail',
      component: () => import('../views/MaterialDetailView.vue')
    },
    {
      path: '/trash',
      name: 'Trash',
      component: () => import('../views/TrashView.vue')
    },
  ],
})

function getRoleFromToken(): string | null {
  const token = localStorage.getItem('token')
  if (!token) return null
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.role || null
  } catch {
    return null
  }
}

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const publicPaths = ['/login', '/about', '/']

  // 未登录拦截
  if (!token && !publicPaths.includes(to.path) && to.path !== '/') {
    next('/login')
    return
  }

  // admin 路由角色守卫
  if (to.path === '/admin') {
    const role = getRoleFromToken()
    if (role !== 'ADMIN') {
      next('/')
      return
    }
  }

  next()
})

export default router
