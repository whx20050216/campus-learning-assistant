<template>
  <div class="page-container profile-page">
    <!-- 未登录 -->
    <div v-if="!isLoggedIn" class="empty-state" style="margin-top: var(--space-8)">
      <div class="empty-icon"><el-icon :size="28"><Lock /></el-icon></div>
      <p class="empty-text">请先登录</p>
      <el-button type="primary" @click="goLogin">去登录</el-button>
    </div>

    <template v-else>
      <!-- 加载中 -->
      <div v-if="loading" class="empty-state">
        <el-icon class="is-loading" size="32"><Loading /></el-icon>
        <p class="empty-text">加载中...</p>
      </div>

      <!-- 内容卡片 -->
      <div v-else class="profile-card">
        <!-- 顶部头像区 -->
        <div class="profile-avatar-section">
          <div class="profile-avatar">
            <el-icon :size="32"><Avatar /></el-icon>
          </div>
          <div class="profile-name">{{ user?.username || '用户' }}</div>
          <el-tag :type="user?.role === 'ADMIN' ? 'danger' : 'primary'" size="small" effect="light" class="profile-role">
            {{ user?.role === 'ADMIN' ? '管理员' : '学生' }}
          </el-tag>
        </div>

        <!-- 只读信息 -->
        <section class="profile-section">
          <div class="section-header">
            <el-icon><User /></el-icon>
            <h2 class="section-title">基本信息</h2>
          </div>
          <div class="info-list">
            <div v-if="user?.role !== 'ADMIN'" class="info-row">
              <span class="info-label">学号</span>
              <el-input v-model="readonlyStudentNo" readonly class="readonly-input">
                <template #prefix>
                  <el-icon><Document /></el-icon>
                </template>
              </el-input>
            </div>
            <div v-else class="info-row">
              <span class="info-label">管理员编号</span>
              <el-input v-model="readonlyAdminId" readonly class="readonly-input">
                <template #prefix>
                  <el-icon><Document /></el-icon>
                </template>
              </el-input>
            </div>
            <div class="info-row">
              <span class="info-label">用户名</span>
              <el-input v-model="readonlyUsername" readonly class="readonly-input">
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </div>
            <div class="info-row">
              <span class="info-label">角色</span>
              <el-input v-model="readonlyRole" readonly class="readonly-input">
                <template #prefix>
                  <el-icon><Medal /></el-icon>
                </template>
              </el-input>
            </div>
            <div class="info-row">
              <span class="info-label">注册时间</span>
              <el-input v-model="readonlyCreatedAt" readonly class="readonly-input">
                <template #prefix>
                  <el-icon><Calendar /></el-icon>
                </template>
              </el-input>
            </div>
          </div>
        </section>

        <!-- 可编辑信息 -->
        <section class="profile-section">
          <div class="section-header">
            <el-icon><Edit /></el-icon>
            <h2 class="section-title">资料编辑</h2>
          </div>
          <el-form :model="form" label-width="80px" class="profile-form">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱">
                <template #prefix>
                  <el-icon><Message /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <template v-if="user?.role !== 'ADMIN'">
              <el-form-item label="专业">
                <el-input v-model="form.major" placeholder="请输入专业">
                  <template #prefix>
                    <el-icon><School /></el-icon>
                  </template>
                </el-input>
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
            </template>
          </el-form>
        </section>

        <!-- 保存按钮 -->
        <div class="profile-actions">
          <el-button type="primary" size="large" :loading="saving" class="save-btn" @click="handleSave">
            <el-icon><Check /></el-icon>
            保存修改
          </el-button>
          <el-button size="large" @click="resetForm">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </div>

        <el-alert
          v-if="saveTip"
          :title="saveTip"
          type="info"
          :closable="false"
          show-icon
          style="margin-top: var(--space-4)"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, User, Edit, Document, Medal, Calendar, Message, School, Check, RefreshLeft } from '@element-plus/icons-vue'
import { getCurrentUser, updateProfile } from '@/api/auth'
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

// 只读字段绑定
const readonlyStudentNo = computed(() => user.value?.studentNo || '-')
const readonlyUsername = computed(() => user.value?.username || '-')
const readonlyAdminId = computed(() => user.value?.id?.toString() || '-')
const readonlyRole = computed(() => user.value?.role === 'ADMIN' ? '管理员' : '学生')
const readonlyCreatedAt = computed(() => formatTime(user.value?.createdAt))

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
    ElMessage.error(err.response?.data?.msg || err.message || '获取用户信息失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  saveTip.value = ''
  try {
    const res = await updateProfile({
      email: form.value.email || undefined,
      major: form.value.major || undefined,
      grade: form.value.grade || undefined,
    })

    if (res.code === 200) {
      user.value = res.data
      ElMessage.success('保存成功')
      saveTip.value = ''
    } else {
      ElMessage.error(res.msg || '保存失败')
      saveTip.value = res.msg || '保存失败'
    }
  } catch (err: any) {
    const msg = err.response?.data?.msg || err.message || '保存失败'
    ElMessage.error(msg)
    saveTip.value = msg
    await loadUser()
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
  max-width: 600px;
  margin: 0 auto;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-color);
  overflow: hidden;
}

/* 顶部头像区 */
.profile-avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-8) var(--space-6);
  background: linear-gradient(135deg, var(--primary-50) 0%, var(--bg-hover) 100%);
  border-bottom: 1px solid var(--border-color);
}

.profile-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: var(--primary-50);
  color: var(--primary-500);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-3);
  box-shadow: var(--shadow-md);
}

.profile-name {
  font-size: var(--text-xl);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-1);
}

.profile-role {
  font-weight: 500;
}

/* 信息分块 */
.profile-section {
  padding: var(--space-6);
  border-bottom: 1px solid var(--border-color);
}

.profile-section:last-of-type {
  border-bottom: none;
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-5);
}

.section-header .el-icon {
  color: var(--primary-500);
  font-size: var(--text-lg);
}

.section-title {
  font-size: var(--text-lg);
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

/* 信息列表 */
.info-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.info-row {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.info-label {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  font-weight: 500;
}

.readonly-input :deep(.el-input__wrapper) {
  background: var(--bg-hover);
}

.readonly-input :deep(.el-input__inner) {
  color: var(--text-secondary);
}

/* 保存按钮 */
.profile-actions {
  padding: var(--space-4) var(--space-6) var(--space-6);
  display: flex;
  gap: var(--space-3);
}

.save-btn {
  flex: 1;
}

@media (max-width: 600px) {
  .profile-card {
    width: 100%;
    border-radius: 0;
    border-left: none;
    border-right: none;
  }

  .profile-page {
    padding: 0;
  }

  .profile-section {
    padding: var(--space-5) var(--space-4);
  }

  .profile-actions {
    padding: var(--space-4);
  }
}
</style>
