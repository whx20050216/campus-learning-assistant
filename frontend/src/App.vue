<template>
  <div class="app">
    <nav class="navbar">
      <div class="nav-brand">
        <span class="nav-brand-icon">📚</span>
        <span>校园智能学习助手</span>
      </div>
      <div class="nav-links">
        <RouterLink to="/" class="nav-link">首页</RouterLink>
        <RouterLink to="/upload" class="nav-link">📤 上传资料</RouterLink>
        <RouterLink to="/search" class="nav-link">🔍 智能搜索</RouterLink>
        <RouterLink to="/plans" class="nav-link">📝 学习计划</RouterLink>
        <RouterLink to="/analysis" class="nav-link">📊 数据分析</RouterLink>
        <RouterLink v-if="isAdmin" to="/admin" class="nav-link">⚙️ 系统管理</RouterLink>
      </div>
      <div class="nav-user">
        <template v-if="currentUser">
          <RouterLink to="/profile" class="nav-link" style="padding: var(--space-1) var(--space-3);">👤 个人中心</RouterLink>
          <span class="user-name">👋 {{ currentUser.username }}</span>
          <el-button type="primary" link size="small" @click="logout">退出</el-button>
        </template>
        <RouterLink v-else to="/login" class="login-btn">登录 / 注册</RouterLink>
      </div>
    </nav>

    <main class="main-content">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentUser } from '@/api/auth'
import type { UserVO } from '@/api/auth'

const isAdmin = ref(false)
const currentUser = ref<UserVO | null>(null)

function checkAdmin() {
  const token = localStorage.getItem('token')
  if (!token) return false
  const parts = token.split('.')
  if (parts.length < 2) return false
  try {
    const base64 = parts[1]
    const json = atob(base64.replace(/-/g, '+').replace(/_/g, '/'))
    const payload = JSON.parse(json)
    return payload.role === 'admin'
  } catch {
    return false
  }
}

async function fetchUserInfo() {
  const token = localStorage.getItem('token')
  if (!token) {
    currentUser.value = null
    isAdmin.value = false
    return
  }
  try {
    const res = await getCurrentUser()
    if (res.code === 200) {
      currentUser.value = res.data
      isAdmin.value = res.data?.role === 'admin'
    } else {
      currentUser.value = null
      isAdmin.value = false
    }
  } catch {
    currentUser.value = null
    isAdmin.value = false
  }
}

function logout() {
  localStorage.removeItem('token')
  currentUser.value = null
  isAdmin.value = false
  ElMessage.success('已退出登录')
  window.location.reload()
}

onMounted(() => {
  fetchUserInfo()
})

// 监听登录状态变化（从 LoginView 触发）
window.addEventListener('auth-change', () => {
  fetchUserInfo()
})
</script>

<style scoped>
.app {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-body);
}

/* 导航栏 */
.navbar {
  background: var(--bg-card);
  padding: 0 var(--space-6);
  height: var(--navbar-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
  flex-shrink: 0;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-lg);
  font-weight: 700;
  color: var(--primary-500);
  white-space: nowrap;
}

.nav-brand-icon {
  font-size: var(--text-xl);
}

.nav-links {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  flex: 1;
  justify-content: center;
}

.nav-link {
  text-decoration: none;
  color: var(--text-secondary);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  transition: all 0.2s ease;
  font-size: var(--text-md);
  font-weight: 500;
  white-space: nowrap;
}

.nav-link:hover {
  color: var(--primary-500);
  background: var(--bg-primary-subtle);
}

.nav-link.router-link-active {
  color: var(--primary-500);
  background: var(--bg-primary-subtle);
  font-weight: 600;
}

.nav-user {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  font-size: var(--text-sm);
}

.user-name {
  color: var(--text-primary);
  font-weight: 500;
}

.login-btn {
  text-decoration: none;
  color: var(--text-on-primary);
  background: var(--primary-500);
  padding: 6px var(--space-4);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: 500;
  transition: background 0.2s ease;
}

.login-btn:hover {
  background: var(--primary-600);
  text-decoration: none;
}

/* 内容区 */
.main-content {
  flex: 1;
  width: 100%;
  max-width: 100%;
  min-height: calc(100vh - var(--navbar-height));
}

/* 覆盖 #app 的默认样式 */
:global(#app) {
  display: flex !important;
  flex-direction: column;
  grid-template-columns: none !important;
  max-width: 100% !important;
  width: 100%;
  margin: 0 !important;
  padding: 0 !important;
}

/* 响应式 */
@media (max-width: 768px) {
  .navbar {
    padding: 0 var(--space-4);
    flex-wrap: wrap;
    height: auto;
    min-height: var(--navbar-height);
    gap: var(--space-2);
  }

  .nav-links {
    order: 3;
    width: 100%;
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: var(--space-2);
  }

  .nav-link {
    font-size: var(--text-sm);
    padding: var(--space-1) var(--space-2);
  }
}
</style>
