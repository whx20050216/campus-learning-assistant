<template>
  <div class="app">
    <nav class="navbar">
      <div class="nav-brand">
        <el-icon class="nav-brand-icon" :size="22"><Collection /></el-icon>
        <span>校园智能学习助手</span>
      </div>
      <div class="nav-links" @click.capture="handleNavClick">
        <RouterLink v-if="!isAdmin" to="/" class="nav-link">
          <el-icon :size="18"><Collection /></el-icon>
          <span>首页</span>
        </RouterLink>
        <template v-if="!isAdmin">
          <RouterLink to="/upload" class="nav-link">
            <el-icon :size="18"><Upload /></el-icon>
            <span>上传资料</span>
          </RouterLink>
          <RouterLink to="/materials" class="nav-link">
            <el-icon :size="18"><Document /></el-icon>
            <span>我的资料</span>
          </RouterLink>
          <RouterLink to="/search" class="nav-link">
            <el-icon :size="18"><Search /></el-icon>
            <span>智能搜索</span>
          </RouterLink>
          <RouterLink to="/plans" class="nav-link">
            <el-icon :size="18"><Document /></el-icon>
            <span>学习计划</span>
          </RouterLink>
          <RouterLink to="/analysis" class="nav-link">
            <el-icon :size="18"><TrendCharts /></el-icon>
            <span>数据分析</span>
          </RouterLink>
          <RouterLink to="/trash" class="nav-link">
            <el-icon :size="18"><Delete /></el-icon>
            <span>回收站</span>
          </RouterLink>
        </template>
        <RouterLink v-if="isAdmin" to="/admin" class="nav-link">
          <el-icon :size="18"><Setting /></el-icon>
          <span>系统管理</span>
        </RouterLink>
      </div>
      <div class="nav-user">
        <template v-if="currentUser">
          <el-dropdown trigger="click" @visible-change="onReminderVisibleChange">
            <span class="nav-link" style="padding: var(--space-1) var(--space-3); cursor: pointer;">
              <el-badge :value="reminders.length" :hidden="reminders.length === 0">
                <el-icon :size="20"><Bell /></el-icon>
              </el-badge>
            </span>
            <template #dropdown>
              <el-dropdown-menu style="min-width: 280px; max-width: 360px;">
                <div v-if="reminders.length === 0" class="reminder-empty">暂无提醒</div>
                <div v-else class="reminder-list">
                  <div v-for="item in reminders" :key="item.planId" class="reminder-item">
                    <div class="reminder-content">
                      <div class="reminder-title">计划《{{ item.planName }}》</div>
                      <div class="reminder-meta">
                        {{ item.daysLeft <= 0 ? '今天到期' : `将在 ${item.daysLeft} 天后到期` }}
                      </div>
                    </div>
                    <el-button type="primary" size="small" text @click="handleReadReminder(item.planId)">
                      知道了
                    </el-button>
                  </div>
                </div>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <RouterLink to="/profile" class="nav-link" style="padding: var(--space-1) var(--space-3);">
            <el-icon :size="18"><User /></el-icon>
            <span>个人中心</span>
          </RouterLink>
          <span class="user-name">{{ currentUser.username }}</span>
          <el-button type="primary" link size="small" @click="logout">退出</el-button>
        </template>
        <RouterLink v-else to="/login" class="login-btn">登录 / 注册</RouterLink>
      </div>
    </nav>

    <main class="main-content">
      <transition name="fade" mode="out-in">
        <RouterView />
      </transition>
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router'
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Bell,
  Collection,
  Upload,
  Search,
  Document,
  TrendCharts,
  Delete,
  Setting,
  User,
} from '@element-plus/icons-vue'
import { getCurrentUser } from '@/api/auth'
import { usePlanStore } from '@/stores/plan'
import type { UserVO } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const isAdmin = ref(false)
const currentUser = ref<UserVO | null>(null)
const planStore = usePlanStore()
const reminders = computed(() => planStore.reminders)
const reminderLoaded = ref(false)

function checkAdmin() {
  const token = localStorage.getItem('token')
  if (!token) return false
  const parts = token.split('.')
  if (parts.length < 2) return false
  try {
    const base64 = parts[1]
    const json = atob(base64.replace(/-/g, '+').replace(/_/g, '/'))
    const payload = JSON.parse(json)
    return payload.role === 'ADMIN'
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
      isAdmin.value = res.data?.role === 'ADMIN'
      if (isAdmin.value) {
        router.push('/admin')
      }
      await planStore.fetchReminders()
      reminderLoaded.value = true
    } else {
      currentUser.value = null
      isAdmin.value = false
    }
  } catch {
    currentUser.value = null
    isAdmin.value = false
  }
}

function handleNavClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (target.closest('.nav-link') && localStorage.getItem('uploading') === 'true') {
    e.preventDefault()
    e.stopPropagation()
    ElMessage.warning('资料上传与识别中，请勿离开页面')
  }
}

function logout() {
  localStorage.removeItem('token')
  currentUser.value = null
  isAdmin.value = false
  ElMessage.success('已退出登录')
  window.location.reload()
}

async function handleReadReminder(planId: number) {
  const res = await planStore.readReminder(planId)
  if (res.code === 200) {
    ElMessage.success('已标记为已读')
  } else {
    ElMessage.error(res.msg || '操作失败')
  }
}

function onReminderVisibleChange(visible: boolean) {
  if (visible && reminders.value.length === 0) {
    planStore.fetchReminders()
  }
}

onMounted(async () => {
  await fetchUserInfo()
  if (isAdmin.value && route.path === '/') {
    router.push('/admin')
  }
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
  box-shadow: var(--shadow-sm);
  border-bottom: none;
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
  display: inline-flex;
  align-items: center;
  justify-content: center;
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
  transition: all 0.2s var(--ease-out);
  font-size: var(--text-md);
  font-weight: 500;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  position: relative;
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

.nav-link.router-link-active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: var(--space-3);
  right: var(--space-3);
  height: 2px;
  background: var(--primary-500);
  border-radius: var(--radius-full);
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
  transition: all 0.2s var(--ease-out);
}

.login-btn:hover {
  background: var(--primary-600);
  text-decoration: none;
  transform: translateY(-1px);
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

/* 提醒下拉 */
.reminder-empty {
  padding: var(--space-4);
  text-align: center;
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

.reminder-list {
  max-height: 300px;
  overflow-y: auto;
}

.reminder-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--border-color);
}

.reminder-item:last-child {
  border-bottom: none;
}

.reminder-content {
  flex: 1;
  min-width: 0;
}

.reminder-title {
  font-size: var(--text-sm);
  color: var(--text-primary);
  font-weight: 500;
  margin-bottom: 2px;
}

.reminder-meta {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
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
