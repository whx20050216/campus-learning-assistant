<template>
  <div class="page-container admin-view">
    <h1 class="page-title">⚙️ 系统管理</h1>

    <div v-if="loading && !users.length && activeTab === 'users'" class="loading-state">加载中...</div>
    <div v-if="pendingLoading && !pendingMaterials.length && activeTab === 'materials'" class="loading-state">加载中...</div>

    <template v-else>
      <!-- 统计卡片 -->
      <div class="card-row">
        <div class="stat-card">
          <div class="stat-label">用户总数</div>
          <div class="stat-value">{{ status?.totalUsers ?? 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">今日上传数</div>
          <div class="stat-value">{{ status?.todayUploads ?? 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">活跃计划数</div>
          <div class="stat-value">{{ status?.activePlans ?? 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">服务状态</div>
          <div class="service-status">
            <el-tag :type="status?.javaStatus === 'UP' ? 'success' : 'danger'" size="small">Java {{ status?.javaStatus }}</el-tag>
            <el-tag :type="status?.pythonStatus === 'UP' ? 'success' : 'danger'" size="small">Python {{ status?.pythonStatus }}</el-tag>
            <el-tag :type="status?.dbStatus === 'UP' ? 'success' : 'danger'" size="small">DB {{ status?.dbStatus }}</el-tag>
          </div>
        </div>
      </div>

      <!-- Tabs -->
      <el-tabs v-model="activeTab" @tab-change="onTabChange" class="admin-tabs">
        <el-tab-pane label="用户管理" name="users">
          <div class="table-section">
            <h2 class="section-title">用户列表</h2>
            <el-table :data="users" v-loading="loading" border stripe>
              <el-table-column prop="studentNo" label="学号" min-width="120" />
              <el-table-column prop="username" label="用户名" min-width="120" />
              <el-table-column prop="email" label="邮箱" min-width="180" />
              <el-table-column prop="major" label="专业" min-width="120" />
              <el-table-column prop="grade" label="年级" min-width="100" />
              <el-table-column prop="role" label="角色" min-width="100">
                <template #default="{ row }">
                  <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">{{ row.role }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" min-width="100">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">{{ row.status === 1 ? '正常' : '已冻结' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" min-width="120" fixed="right">
                <template #default="{ row }">
                  <el-button
                    :type="row.status === 1 ? 'warning' : 'success'"
                    size="small"
                    @click="handleFreeze(row)"
                    :disabled="row.id === currentUserId"
                  >
                    {{ row.status === 1 ? '冻结' : '解冻' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="page"
                v-model:page-size="pageSize"
                :total="total"
                layout="total, prev, pager, next"
                @current-change="fetchUsers"
              />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="内容审核" name="materials">
          <div class="table-section">
            <h2 class="section-title">待审核资料</h2>
            <el-table :data="pendingMaterials" v-loading="pendingLoading" border stripe>
              <el-table-column prop="id" label="ID" min-width="80" />
              <el-table-column prop="title" label="资料标题" min-width="200" />
              <el-table-column prop="userId" label="上传者ID" min-width="120" />
              <el-table-column prop="fileType" label="类型" min-width="100" />
              <el-table-column prop="createdAt" label="上传时间" min-width="180" />
              <el-table-column label="操作" min-width="200" fixed="right">
                <template #default="{ row }">
                  <el-button type="success" size="small" @click="handleAudit(row, 'approve')">通过</el-button>
                  <el-button type="danger" size="small" @click="handleAudit(row, 'reject')">拒绝</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="pendingPage"
                v-model:page-size="pendingPageSize"
                :total="pendingTotal"
                layout="total, prev, pager, next"
                @current-change="fetchPendingMaterials"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, freezeUser, getSystemStatus, getPendingMaterials, auditMaterial } from '@/api/admin'
import type { User, SystemStatusVO, Material } from '@/api/admin'

const users = ref<User[]>([])
const status = ref<SystemStatusVO | null>(null)
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const currentUserId = ref<number | null>(null)

const activeTab = ref('users')
const pendingMaterials = ref<Material[]>([])
const pendingLoading = ref(false)
const pendingPage = ref(1)
const pendingPageSize = ref(10)
const pendingTotal = ref(0)

function parseCurrentUser() {
  const token = localStorage.getItem('token')
  if (!token) return
  const parts = token.split('.')
  if (parts.length < 2) return
  try {
    const base64 = parts[1]
    const json = atob(base64.replace(/-/g, '+').replace(/_/g, '/'))
    const payload = JSON.parse(json)
    currentUserId.value = payload.userId ? Number(payload.userId) : null
  } catch {
    // ignore
  }
}

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getUsers(page.value, pageSize.value)
    if (res.code === 200) {
      users.value = res.data?.records || []
      total.value = res.data?.total || 0
    } else {
      ElMessage.error(res.msg || '获取用户列表失败')
    }
  } catch {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchPendingMaterials() {
  pendingLoading.value = true
  try {
    const res = await getPendingMaterials(pendingPage.value, pendingPageSize.value)
    if (res.code === 200) {
      pendingMaterials.value = res.data?.records || []
      pendingTotal.value = res.data?.total || 0
    } else {
      ElMessage.error(res.msg || '获取待审核资料失败')
    }
  } catch {
    ElMessage.error('获取待审核资料失败')
  } finally {
    pendingLoading.value = false
  }
}

async function fetchStatus() {
  try {
    const res = await getSystemStatus()
    if (res.code === 200) {
      status.value = res.data
    }
  } catch {
    // ignore
  }
}

async function handleFreeze(row: User) {
  const freeze = row.status === 1
  const actionText = freeze ? '冻结' : '解冻'

  try {
    await ElMessageBox.confirm(`确定要${actionText}用户 "${row.username}" 吗？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    const res = await freezeUser(row.id, freeze)
    if (res.code === 200) {
      ElMessage.success(`${actionText}成功`)
      await fetchUsers()
      await fetchStatus()
    } else {
      ElMessage.error(res.msg || `${actionText}失败`)
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || `${actionText}失败`)
  }
}

async function handleAudit(row: Material, action: 'approve' | 'reject') {
  const actionText = action === 'approve' ? '通过' : '拒绝'
  try {
    await ElMessageBox.confirm(`确定要${actionText}资料 "${row.title}" 吗？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: action === 'approve' ? 'success' : 'danger',
    })
  } catch {
    return
  }

  try {
    const res = await auditMaterial(row.id, action)
    if (res.code === 200) {
      ElMessage.success(`${actionText}成功`)
      await fetchPendingMaterials()
      await fetchStatus()
    } else {
      ElMessage.error(res.msg || `${actionText}失败`)
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.msg || `${actionText}失败`)
  }
}

function onTabChange(tabName: string | number) {
  if (tabName === 'materials') {
    fetchPendingMaterials()
  }
}

onMounted(() => {
  parseCurrentUser()
  fetchUsers()
  fetchStatus()
})
</script>

<style scoped>
.card-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.service-status {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-2);
}

.table-section {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.pagination-wrapper {
  margin-top: var(--space-4);
  display: flex;
  justify-content: flex-end;
}

/* Tabs 样式微调 */
.admin-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--space-4);
}

.admin-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--border-color);
}
</style>
