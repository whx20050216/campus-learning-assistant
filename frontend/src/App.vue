<template>
  <div class="app">
    <nav class="navbar">
      <div class="nav-brand">📚 校园智能学习助手</div>
      <div class="nav-links">
        <RouterLink to="/" class="nav-link">首页</RouterLink>
        <RouterLink to="/upload" class="nav-link">📤 上传资料</RouterLink>
        <RouterLink to="/search" class="nav-link">🔍 智能搜索</RouterLink>
        <RouterLink to="/plans" class="nav-link">📝 学习计划</RouterLink>
        <RouterLink to="/analysis" class="nav-link">📊 数据分析</RouterLink>
        <RouterLink v-if="isAdmin" to="/admin" class="nav-link">⚙️ 系统管理</RouterLink>
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

const isAdmin = ref(false)

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

onMounted(() => {
  isAdmin.value = checkAdmin()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #f5f7fa;
  width: 100%;
  height: 100%;
}

/* 关键修复：确保整体布局占满宽度 */
.app {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.navbar {
  background: white;
  padding: 0 40px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  position: sticky;
  top: 0;
  z-index: 100;
  flex-shrink: 0; /* 防止导航栏被压缩 */
}

.nav-brand {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}

.nav-links {
  display: flex;
  gap: 8px;
}

.nav-link {
  text-decoration: none;
  color: #606266;
  padding: 8px 16px;
  border-radius: 6px;
  transition: all 0.3s;
  font-size: 15px;
}

.nav-link:hover {
  color: #409eff;
  background: #f0f9ff;
}

.nav-link.router-link-active {
  color: #409eff;
  background: #ecf5ff;
  font-weight: 500;
}

/* 关键修复：内容区占满剩余宽度和高度 */
.main-content {
  flex: 1;
  width: 100%;
  max-width: 100%;
  min-height: calc(100vh - 64px);
  display: block; /* 覆盖可能继承的 flex 设置 */
}
#app {
  display: flex !important;
  flex-direction: column;
  grid-template-columns: none !important;
  max-width: 100% !important;
  width: 100%;
  margin: 0 !important;
  padding: 0 !important;
}
</style>