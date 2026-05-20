import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
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
      path: '/materials/:id',
      name: 'MaterialDetail',
      component: () => import('../views/MaterialDetailView.vue')
    },
  ],
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const publicPaths = ['/login', '/about']
  if (!token && !publicPaths.includes(to.path) && to.path !== '/') {
    next('/login')
  } else {
    next()
  }
})

export default router
