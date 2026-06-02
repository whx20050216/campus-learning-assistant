<template>
  <div class="page-container trash-page">
    <!-- 未登录 -->
    <div v-if="!isLoggedIn" class="empty-state" style="margin-top: var(--space-8)">
      <div class="empty-icon"><el-icon :size="28"><Lock /></el-icon></div>
      <p class="empty-text">请先登录</p>
      <el-button type="primary" @click="goLogin">去登录</el-button>
    </div>

    <template v-else>
      <!-- 页面标题 -->
      <div class="page-header-center">
        <h1 class="page-title-large">回收站</h1>
        <p class="page-subtitle">30 天内可恢复，超期自动清理</p>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="empty-state">
        <el-icon class="is-loading" size="32"><Loading /></el-icon>
        <p class="empty-text">加载中...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="items.length === 0" class="empty-state">
        <el-empty description="回收站为空">
          <el-button type="primary" @click="goHome">返回首页</el-button>
        </el-empty>
      </div>

      <!-- 资料卡片网格 -->
      <div v-else class="trash-grid">
        <div
          v-for="item in items"
          :key="item.id"
          class="trash-card"
        >
          <div class="trash-card-body">
            <!-- 顶部：文件图标 + 名称 -->
            <div class="trash-card-header">
              <el-icon class="trash-file-icon" :size="28"><component :is="getFileIcon(item.fileType)" /></el-icon>
              <div class="trash-file-info">
                <h3 class="trash-filename">{{ item.title || '未命名资料' }}</h3>
                <div class="trash-file-meta">
                  <el-tag size="small" :type="getStatusType(item.status)" effect="light">
                    {{ getStatusText(item.status) }}
                  </el-tag>
                  <span class="meta-sep">·</span>
                  <span class="meta-size">{{ formatSize(item.fileSize) }}</span>
                </div>
              </div>
            </div>

            <!-- 中部：删除时间 + 剩余天数 -->
            <div class="trash-card-middle">
              <div class="trash-delete-time">
                <el-icon><Delete /></el-icon>
                <span>删除于 {{ formatTime(item.deletedAt) }}</span>
              </div>
              <div class="trash-remaining">
                <el-icon><Timer /></el-icon>
                <span>剩余 {{ getRemainingDays(item.deletedAt) }} 天</span>
              </div>
            </div>

            <!-- 底部操作 -->
            <div class="trash-card-footer">
              <el-button type="primary" size="small" @click="handleRestore(item.id)">
                <el-icon><RefreshLeft /></el-icon> 恢复
              </el-button>
              <el-button type="danger" size="small" plain @click="handlePermanentDelete(item.id)">
                <el-icon><DeleteFilled /></el-icon> 永久删除
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, Delete, DeleteFilled, Timer, RefreshLeft, Lock, Document, Picture, Link } from '@element-plus/icons-vue'
import { getTrashList, restoreMaterial, deleteMaterial } from '@/api/material'

const router = useRouter()
const isLoggedIn = computed(() => !!localStorage.getItem('token'))

const items = ref<any[]>([])
const loading = ref(false)

function goLogin() {
  router.push('/login')
}

function goHome() {
  router.push('/')
}

function getFileIcon(type?: string) {
  const icons: Record<string, any> = {
    'PDF': Document,
    'IMAGE': Picture,
    'PPT': Document,
  }
  return icons[type?.toUpperCase() || ''] || Link
}

function formatSize(bytes?: number) {
  if (!bytes) return '-'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

function formatTime(time?: string) {
  if (!time) return '-'
  const date = new Date(time)
  return `${date.getMonth() + 1}月${date.getDate()}日 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function getRemainingDays(deletedAt?: string) {
  if (!deletedAt) return 30
  const deleted = new Date(deletedAt).getTime()
  const expiry = deleted + 30 * 24 * 60 * 60 * 1000
  const remaining = Math.ceil((expiry - Date.now()) / (24 * 60 * 60 * 1000))
  return Math.max(0, remaining)
}

function getStatusText(status?: string) {
  const map: Record<string, string> = {
    'deleted': '已删除',
    'completed': '已完成',
    'processing': '处理中'
  }
  return map[status || ''] || status || '-'
}

function getStatusType(status?: string): any {
  const map: Record<string, any> = {
    'deleted': 'danger',
    'completed': 'success',
    'processing': 'warning'
  }
  return map[status || ''] || 'info'
}

async function loadTrash() {
  loading.value = true
  try {
    const res = await getTrashList()
    if (res.code === 200) {
      items.value = res.data?.records || res.data || []
    } else {
      ElMessage.error(res.msg || '获取回收站失败')
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.msg || err.message || '获取回收站失败')
  } finally {
    loading.value = false
  }
}

async function handleRestore(id: number | string) {
  try {
    const res = await restoreMaterial(id)
    if (res.code === 200) {
      ElMessage.success('恢复成功')
      items.value = items.value.filter(item => item.id !== id)
    } else {
      ElMessage.error(res.msg || '恢复失败')
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.msg || err.message || '恢复失败')
  }
}

async function handlePermanentDelete(id: number | string) {
  try {
    await ElMessageBox.confirm('永久删除后不可恢复，确定继续？', '确认永久删除', {
      confirmButtonText: '永久删除',
      cancelButtonText: '取消',
      type: 'danger'
    })
    const res = await deleteMaterial(id, true)
    if (res.code === 200) {
      ElMessage.success('已永久删除')
      items.value = items.value.filter(item => item.id !== id)
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.msg || err.message || '删除失败')
    }
  }
}

onMounted(() => {
  if (isLoggedIn.value) {
    loadTrash()
  }
})
</script>

<style scoped>
.trash-page {
  padding: var(--space-6);
}

.page-header-center {
  text-align: center;
  margin-bottom: var(--space-8);
}

.page-title-large {
  font-size: var(--text-2xl);
  font-weight: 700;
  margin: 0 0 var(--space-2);
  color: var(--text-primary);
}

.page-subtitle {
  color: var(--text-tertiary);
  font-size: var(--text-base);
  margin: 0;
}

/* 卡片网格 */
.trash-grid {
  max-width: var(--page-max-width);
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--space-4);
}

.trash-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  transition: all 0.2s ease;
  overflow: hidden;
}

.trash-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.trash-card-body {
  padding: var(--space-5);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

/* 顶部 */
.trash-card-header {
  display: flex;
  gap: var(--space-3);
  align-items: flex-start;
}

.trash-file-icon {
  font-size: 40px;
  line-height: 1;
  flex-shrink: 0;
}

.trash-file-info {
  flex: 1;
  min-width: 0;
}

.trash-filename {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 var(--space-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trash-file-meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.meta-sep {
  opacity: 0.5;
}

/* 中部 */
.trash-card-middle {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-3);
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
}

.trash-delete-time {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  color: var(--text-secondary);
}

.trash-delete-time .el-icon {
  color: var(--text-tertiary);
}

.trash-remaining {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  color: var(--danger-500);
  font-weight: 500;
}

.trash-remaining .el-icon {
  color: var(--danger-500);
}

/* 底部操作 */
.trash-card-footer {
  display: flex;
  gap: var(--space-2);
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .trash-grid {
    grid-template-columns: 1fr;
  }
}
</style>
