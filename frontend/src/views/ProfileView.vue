<template>
  <div class="page-container profile-page">
    <!-- 未登录 -->
    <div v-if="!isLoggedIn" class="empty-state" style="margin-top: var(--space-8)">
      <div class="empty-icon">🔒</div>
      <p class="empty-text">请先登录</p>
      <el-button type="primary" @click="goLogin">去登录</el-button>
    </div>

    <template v-else>
      <header class="page-header-center">
        <h1 class="page-title-large">👤 个人中心</h1>
        <p class="page-subtitle">查看并管理您的个人信息</p>
      </header>

      <!-- 加载中 -->
      <div v-if="loading" class="empty-state">
        <el-icon class="is-loading" size="32"><Loading /></el-icon>
        <p class="empty-text">加载中...</p>
      </div>

      <!-- 内容卡片 -->
      <div v-else class="profile-card">
        <!-- 只读信息 -->
        <section class="profile-section">
          <h2 class="section-title">📋 基本信息</h2>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">学号</span>
              <span class="info-value">{{ user?.studentNo || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">用户名</span>
              <span class="info-value">{{ user?.username || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">角色</span>
              <span class="info-value">
                <el-tag :type="user?.role === 'admin' ? 'danger' : 'primary'" size="small">
                  {{ user?.role === 'admin' ? '管理员' : '学生' }}
                </el-tag>
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">注册时间</span>
              <span class="info-value">{{ formatTime(user?.createdAt) }}</span>
            </div>
          </div>
        </section>

        <!-- 可编辑信息 -->
        <section class="profile-section">
          <h2 class="section-title">✏️ 资料编辑</h2>
          <el-form :model="form" label-width="80px" class="profile-form">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="专业">
              <el-input v-model="form.major" placeholder="请输入专业" />
            </el-form-item>
            <el-form-item label="年级">
              <el-select v-model="form.grade" placeholder="请选择年级" style="width: 100%">
                <el-option label="大一" value="大一" />
                <el-option label="大二" value="大二" />
                <el-option label="大三" value="大三" />
                <el-option label="大四" value="大四" />
                <el-option label="研一" value="研一" />
                <el-option label="研二" value="研二" />
                <el-option label="研三" value="研三" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSave">
                💾 保存修改
              </el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
          <el-alert
            v-if="saveTip"
            :title="saveTip"
            type="info"
            :closable="false"
            show-icon
            style="margin-top: var(--space-4)"
          />
        </section>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getCurrentUser } from '@/api/auth'
import type { UserVO } from '@/api/auth'

const router = useRouter()
const isLoggedIn = computed(() => !!localStorage.getItem('token'))

const user = ref<UserVO | null>(null)
const loading = ref(false)
const saving = ref(false)
const saveTip = ref('')

const form = ref({
  email: '',
  major: '',
  grade: ''
})

function goLogin() {
  router.push('/login')
}

function formatTime(time?: string) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

function resetForm() {
  if (user.value) {
    form.value.email = user.value.email || ''
    form.value.major = user.value.major || ''
    form.value.grade = user.value.grade || ''
  }
  saveTip.value = ''
}

async function loadUser() {
  loading.value = true
  try {
    const res = await getCurrentUser()
    if (res.code === 200) {
      user.value = res.data
      resetForm()
    } else {
      ElMessage.error(res.msg || '获取用户信息失败')
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.msg || '获取用户信息失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  // 后端暂无 PUT 更新接口，先前端模拟保存提示
  saving.value = true
  try {
    // TODO: 待后端提供 PUT /api/auth/profile 或 PUT /api/users/me 接口后接入
    // await updateProfile(form.value)

    // 模拟延迟
    await new Promise(r => setTimeout(r, 600))

    saveTip.value = '修改已保存（前端演示模式：后端 PUT 接口待实现，后续批次接入真实保存）'
    ElMessage.success('保存成功（演示模式）')

    // 更新本地 user 显示
    if (user.value) {
      user.value.email = form.value.email
      user.value.major = form.value.major
      user.value.grade = form.value.grade
    }
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (isLoggedIn.value) {
    loadUser()
  }
})
</script>

<style scoped>
.profile-page {
  padding: var(--space-6);
}

.profile-card {
  max-width: 640px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.profile-section {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.section-title {
  font-size: var(--text-lg);
  font-weight: 600;
  margin-bottom: var(--space-5);
  color: var(--text-primary);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-3);
  background: var(--bg-hover);
  border-radius: var(--radius-md);
}

.info-label {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.info-value {
  font-size: var(--text-base);
  font-weight: 500;
  color: var(--text-primary);
}

.profile-form {
  max-width: 400px;
}
</style>
