<template>
  <div class="page-container admin-view">
    <div class="page-header-row">
      <h1 class="page-title-large">系统管理</h1>
    </div>

    <div v-if="loading && !users.length && activeTab === 'users'" class="loading-state">加载中...</div>
    <div v-if="pendingLoading && !pendingMaterials.length && activeTab === 'materials'" class="loading-state">加载中...</div>

    <template v-else>
      <!-- 统计卡片 -->
      <div class="stat-cards-row">
        <div class="stat-card" style="--stat-accent: var(--primary-500);">
          <div class="stat-card-topbar"></div>
          <div class="stat-card-body">
            <div class="stat-card-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-card-content">
              <div class="stat-card-value">{{ status?.totalUsers ?? 0 }}</div>
              <div class="stat-card-label">用户总数</div>
            </div>
          </div>
        </div>
        <div class="stat-card" style="--stat-accent: var(--success-500);">
          <div class="stat-card-topbar"></div>
          <div class="stat-card-body">
            <div class="stat-card-icon" style="color: var(--success-500);">
              <el-icon><Upload /></el-icon>
            </div>
            <div class="stat-card-content">
              <div class="stat-card-value">{{ status?.todayUploads ?? 0 }}</div>
              <div class="stat-card-label">今日上传</div>
            </div>
          </div>
        </div>
        <div class="stat-card" style="--stat-accent: var(--warning-500);">
          <div class="stat-card-topbar"></div>
          <div class="stat-card-body">
            <div class="stat-card-icon" style="color: var(--warning-500);">
              <el-icon><Calendar /></el-icon>
            </div>
            <div class="stat-card-content">
              <div class="stat-card-value">{{ status?.activePlans ?? 0 }}</div>
              <div class="stat-card-label">活跃计划</div>
            </div>
          </div>
        </div>
        <div class="stat-card" style="--stat-accent: var(--primary-400);">
          <div class="stat-card-topbar"></div>
          <div class="stat-card-body">
            <div class="stat-card-icon" style="color: var(--primary-400);">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="stat-card-content">
              <div class="stat-card-services">
                <el-tag :type="status?.javaStatus === 'UP' ? 'success' : (status?.javaStatus === 'WARNING' ? 'warning' : 'danger')" size="small" effect="light">Java</el-tag>
                <el-tag :type="status?.pythonStatus === 'UP' ? 'success' : 'danger'" size="small" effect="light">Python</el-tag>
                <el-tag :type="status?.dbStatus === 'UP' ? 'success' : 'danger'" size="small" effect="light">DB</el-tag>
                <el-tag :type="status?.redisStatus === 'UP' ? 'success' : 'danger'" size="small" effect="light">Redis</el-tag>
                <el-tag :type="status?.diskStatus === 'UP' ? 'success' : (status?.diskStatus === 'WARNING' || status?.diskStatus === 'CRITICAL' ? 'warning' : 'danger')" size="small" effect="light">磁盘 {{ status?.diskUsage || '' }}</el-tag>
              </div>
              <div class="stat-card-label">系统状态</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Tabs -->
      <el-tabs v-model="activeTab" @tab-change="onTabChange" class="admin-tabs">
        <el-tab-pane label="用户管理" name="users">
          <div class="table-section">
            <div class="section-header">
              <h2 class="section-title">
                <el-icon><UserFilled /></el-icon>
                用户列表
              </h2>
            </div>
            <el-table :data="users" v-loading="loading" border stripe class="admin-table">
              <el-table-column prop="studentNo" label="学号" min-width="120" />
              <el-table-column prop="username" label="用户名" min-width="120" />
              <el-table-column prop="email" label="邮箱" min-width="180" />
              <el-table-column prop="major" label="专业" min-width="120" />
              <el-table-column prop="grade" label="年级" min-width="100" />
              <el-table-column prop="role" label="角色" min-width="100">
                <template #default="{ row }">
                  <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small" effect="light">{{ row.role }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" min-width="100">
                <template #default="{ row }">
                  <div class="status-dot-row">
                    <span class="status-dot" :class="row.status === 1 ? 'active' : 'frozen'"></span>
                    <span>{{ row.status === 1 ? '正常' : '已冻结' }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="操作" min-width="120" fixed="right">
                <template #default="{ row }">
                  <el-button
                    :type="row.status === 1 ? 'warning' : 'success'"
                    size="small"
                    link
                    :icon="row.status === 1 ? Lock : Unlock"
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
            <div class="section-header">
              <h2 class="section-title">
                <el-icon><DocumentChecked /></el-icon>
                待审核资料
              </h2>
            </div>
            <div v-if="pendingMaterials.length" class="audit-card-grid">
              <div v-for="row in pendingMaterials" :key="row.id" class="audit-card">
                <div class="audit-card-header">
                  <el-icon class="audit-card-icon" :size="28"><component :is="getFileIcon(row.fileType)" /></el-icon>
                  <div class="audit-card-info">
                    <div class="audit-card-title">{{ row.title }}</div>
                    <div class="audit-card-meta">
                      <span>上传者 ID: {{ row.userId }}</span>
                      <span class="meta-sep">·</span>
                      <span>{{ row.fileType }}</span>
                    </div>
                  </div>
                </div>
                <div class="audit-card-time">
                  <el-icon><Clock /></el-icon>
                  <span>{{ formatTime(row.createdAt) }}</span>
                </div>
                <div class="audit-card-actions">
                  <el-button type="primary" size="small" @click="openAuditDialog(row)">
                    查看详情
                  </el-button>
                  <el-button type="success" size="small" :icon="CircleCheck" @click="handleAudit(row, 'approve')">
                    通过
                  </el-button>
                  <el-button type="danger" size="small" :icon="CircleClose" @click="handleAudit(row, 'reject')">
                    拒绝
                  </el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无待审核资料" />

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

  <!-- 审核详情弹窗 -->
  <el-dialog
    v-model="auditDialogVisible"
    :title="`《${auditDetail?.title || ''}》审核详情`"
    width="800px"
    destroy-on-close
    @closed="closeAuditDialog"
  >
    <el-skeleton v-if="auditDialogLoading" :rows="6" animated />
    <div v-else-if="auditDetail" class="audit-dialog-body">
      <!-- 文件预览 -->
      <div class="preview-box">
        <iframe
          v-if="auditDetail.fileType?.toUpperCase?.() === 'PDF' && previewBlobUrl"
          :src="previewBlobUrl"
          class="preview-frame"
        />
        <img
          v-else-if="previewBlobUrl"
          :src="previewBlobUrl"
          class="preview-image"
          alt="资料预览"
        />
        <div v-else class="preview-placeholder">
          <el-icon :size="48"><component :is="getFileIcon(auditDetail.fileType)" /></el-icon>
          <span>预览加载失败</span>
        </div>
      </div>

      <!-- 文件信息 -->
      <div class="detail-meta">
        <span>类型：{{ auditDetail.fileType || '未知' }}</span>
        <span v-if="auditDetail.pages">· {{ auditDetail.pages }} 页</span>
        <span>· 上传者 ID：{{ auditDetail.userId }}</span>
      </div>

      <!-- OCR 文本（折叠） -->
      <el-collapse v-model="ocrExpanded" class="ocr-collapse">
        <el-collapse-item title="OCR 识别文本（辅助参考）" name="1">
          <div v-if="auditDetail.ocrResult?.ocrText" class="ocr-text">
            {{ auditDetail.ocrResult.ocrText }}
          </div>
          <div v-else class="ocr-empty">暂无 OCR 文本</div>
        </el-collapse-item>
      </el-collapse>

      <!-- 关键词 -->
      <div v-if="auditDetail.keywords?.length" class="detail-section">
        <div class="section-label">关键词</div>
        <div class="tag-list">
          <el-tag v-for="(kw, idx) in auditDetail.keywords" :key="idx" size="small" effect="light">
            {{ kw.keyword }}
          </el-tag>
        </div>
      </div>

      <!-- 知识点 -->
      <div v-if="auditDetail.knowledgePoints?.length" class="detail-section">
        <div class="section-label">知识点</div>
        <div class="tag-list">
          <el-tag
            v-for="(kp, idx) in auditDetail.knowledgePoints"
            :key="idx"
            size="small"
            type="success"
            effect="light"
          >
            {{ kp.content }}
          </el-tag>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="closeAuditDialog">取消</el-button>
      <el-button type="success" @click="handleAuditFromDialog('approve')">通过</el-button>
      <el-button type="danger" @click="handleAuditFromDialog('reject')">拒绝</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User, Upload, Calendar, Monitor,
  UserFilled, DocumentChecked, Clock,
  Lock, Unlock, CircleCheck, CircleClose,
  Document, Picture, Link
} from '@element-plus/icons-vue'
import { getUsers, freezeUser, getSystemStatus, getPendingMaterials, auditMaterial, getAdminMaterialDetail } from '@/api/admin'
import { downloadMaterial } from '@/api/material'
import type { User as UserType, SystemStatusVO, Material } from '@/api/admin'

const users = ref<UserType[]>([])
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

// 审核详情弹窗
const auditDialogVisible = ref(false)
const auditDetail = ref<any>(null)
const previewBlobUrl = ref('')
const ocrExpanded = ref(false)
const auditDialogLoading = ref(false)

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

function getFileIcon(type?: string) {
  const icons: Record<string, any> = {
    'PDF': Document,
    'IMAGE': Picture,
    'PPT': Document,
  }
  return icons[type?.toUpperCase() || ''] || Link
}

function formatTime(time?: string) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
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

async function handleFreeze(row: UserType) {
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

async function openAuditDialog(row: Material) {
  auditDialogVisible.value = true
  auditDialogLoading.value = true
  ocrExpanded.value = false
  previewBlobUrl.value = ''
  try {
    const res = await getAdminMaterialDetail(row.id)
    if (res.code === 200 && res.data) {
      auditDetail.value = res.data
      await loadPreview(row.id, row.fileType)
    } else {
      ElMessage.error(res.msg || '获取详情失败')
      auditDialogVisible.value = false
    }
  } catch {
    ElMessage.error('获取详情失败')
    auditDialogVisible.value = false
  } finally {
    auditDialogLoading.value = false
  }
}

async function loadPreview(id: number, fileType?: string) {
  const type = fileType?.toUpperCase?.() || 'PDF'
  const mimeType = type === 'PDF' ? 'application/pdf'
                 : type === 'PNG' ? 'image/png'
                 : 'image/jpeg'
  try {
    const res = await downloadMaterial(id, true)
    const blob = new Blob([res.data], { type: mimeType })
    previewBlobUrl.value = URL.createObjectURL(blob)
  } catch (err) {
    console.error('预览加载失败', err)
  }
}

function closeAuditDialog() {
  if (previewBlobUrl.value) {
    URL.revokeObjectURL(previewBlobUrl.value)
    previewBlobUrl.value = ''
  }
  auditDetail.value = null
  auditDialogVisible.value = false
}

async function handleAuditFromDialog(action: 'approve' | 'reject') {
  if (!auditDetail.value) return
  const row = { id: auditDetail.value.id, title: auditDetail.value.title } as Material
  await handleAudit(row, action)
  closeAuditDialog()
}

let statusTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  parseCurrentUser()
  fetchUsers()
  fetchStatus()
  statusTimer = setInterval(() => {
    fetchStatus()
  }, 30000)
})

onUnmounted(() => {
  if (statusTimer) {
    clearInterval(statusTimer)
    statusTimer = null
  }
})
</script>

<style scoped>
.page-header-row {
  margin-bottom: var(--space-6);
}

.page-title-large {
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

/* 统计卡片 */
.stat-cards-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

@media (max-width: 1200px) {
  .stat-cards-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stat-cards-row {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  transition: all 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-card-topbar {
  height: 4px;
  background: var(--stat-accent);
}

.stat-card-body {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-5);
}

.stat-card-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  background: var(--bg-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: var(--primary-500);
  flex-shrink: 0;
}

.stat-card-icon .el-icon {
  font-size: 24px;
}

.stat-card-content {
  flex: 1;
  min-width: 0;
}

.stat-card-value {
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
  margin-bottom: var(--space-1);
}

.stat-card-label {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  font-weight: 500;
}

.stat-card-services {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  margin-bottom: var(--space-1);
}

/* 表格区域 */
.table-section {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.section-header {
  margin-bottom: var(--space-4);
}

.section-title {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

/* 状态 dot */
.status-dot-row {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.active {
  background: var(--success-500);
}

.status-dot.frozen {
  background: var(--danger-500);
}

/* 审核卡片网格 */
.audit-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.audit-card {
  background: var(--bg-hover);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  transition: all 0.2s ease;
}

.audit-card:hover {
  border-color: var(--primary-300);
  box-shadow: var(--shadow-sm);
}

.audit-card-header {
  display: flex;
  gap: var(--space-3);
  align-items: flex-start;
}

.audit-card-icon {
  font-size: 36px;
  line-height: 1;
  flex-shrink: 0;
}

.audit-card-info {
  flex: 1;
  min-width: 0;
}

.audit-card-title {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-card-meta {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  display: flex;
  gap: var(--space-1);
  align-items: center;
}

.meta-sep {
  opacity: 0.5;
}

.audit-card-time {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.audit-card-actions {
  display: flex;
  gap: var(--space-2);
  justify-content: flex-end;
  margin-top: auto;
}

.pagination-wrapper {
  margin-top: var(--space-4);
  display: flex;
  justify-content: flex-end;
}

/* Tabs 样式 */
.admin-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--space-4);
}

.admin-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 2px;
  background: var(--border-color);
}

.admin-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-12);
  color: var(--text-secondary);
}

/* 审核详情弹窗 */
.audit-dialog-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.preview-box {
  width: 100%;
  height: 360px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-frame {
  width: 100%;
  height: 100%;
  border: none;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  color: var(--text-tertiary);
}

.detail-meta {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.ocr-collapse {
  margin-top: var(--space-2);
}

.ocr-text {
  max-height: 200px;
  overflow-y: auto;
  white-space: pre-wrap;
  font-size: var(--text-sm);
  line-height: 1.6;
  color: var(--text-primary);
  background: var(--bg-secondary);
  padding: var(--space-3);
  border-radius: var(--radius-sm);
}

.ocr-empty {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  padding: var(--space-3);
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.section-label {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-secondary);
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}
</style>
