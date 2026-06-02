<template>
  <div class="page-container material-list-page">
    <!-- 未登录提示 -->
    <div v-if="!isLoggedIn" class="empty-state" style="margin-top: var(--space-8)">
      <div class="empty-icon"><el-icon :size="28"><Lock /></el-icon></div>
      <p class="empty-text">请先登录</p>
      <p class="empty-tip">登录后即可管理您的学习资料</p>
      <el-button type="primary" @click="goLogin">去登录</el-button>
    </div>

    <template v-else>
      <!-- 页面头部 -->
      <div class="page-header-row">
        <div class="page-header-left">
          <h1 class="page-title">我的资料库</h1>
          <p class="page-subtitle">管理您上传的全部学习资料</p>
        </div>
        <el-button type="primary" @click="goUpload">
          <el-icon><Upload /></el-icon>
          上传新资料
        </el-button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" size="32"><Loading /></el-icon>
        <p>加载中...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="materials.length === 0" class="empty-state" style="margin-top: var(--space-8)">
        <el-empty description="暂无资料，快去上传第一份学习资料吧">
          <el-button type="primary" @click="goUpload">去上传</el-button>
        </el-empty>
      </div>

      <!-- 资料卡片网格 -->
      <div v-else class="material-grid">
        <article
          v-for="item in materials"
          :key="item.id"
          class="material-card"
        >
          <!-- 文件类型大图标 -->
          <div class="material-icon-wrap">
            <el-icon class="material-icon" :size="36"><component :is="getFileIcon(item.fileType)" /></el-icon>
          </div>

          <!-- 资料标题 -->
          <h3 class="material-title" :title="item.title">{{ item.title }}</h3>

          <!-- 标签行 -->
          <div class="material-tags">
            <el-tag v-if="item.courseTag" size="small" type="info" effect="plain">
              {{ item.courseTag }}
            </el-tag>
            <el-tag
              size="small"
              :type="getStatusType(item.status)"
              effect="light"
            >
              {{ getStatusText(item.status) }}
            </el-tag>
          </div>

          <!-- 上传时间 -->
          <div class="material-time">
            <el-icon><Timer /></el-icon>
            <span>{{ formatTime(item.createdAt) }}</span>
          </div>

          <!-- 底部操作 -->
          <div class="material-actions">
            <el-button type="primary" size="small" @click="viewDetail(item.id)">
              查看详情
            </el-button>
            <el-button type="danger" size="small" plain @click="handleDelete(item.id)">
              删除
            </el-button>
          </div>
        </article>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, Timer, Upload, Document, Picture, Link } from '@element-plus/icons-vue'
import { getMaterialList, deleteMaterial } from '@/api/material'

const router = useRouter()
const isLoggedIn = computed(() => !!localStorage.getItem('token'))

const materials = ref<any[]>([])
const loading = ref(false)

function goLogin() {
  router.push('/login')
}

function goUpload() {
  router.push('/upload')
}

function getFileIcon(type?: string) {
  const icons: Record<string, any> = {
    'PDF': Document,
    'PPT': Document,
    'IMAGE': Picture,
    'JPG': Picture,
    'JPEG': Picture,
    'PNG': Picture
  }
  return icons[type?.toUpperCase() || ''] || Link
}

function getStatusText(status?: string) {
  const map: Record<string, string> = {
    'completed': '已完成',
    'processing': '处理中',
    'uploaded': '已上传',
    'pending': '待审核',
    'failed': '处理失败'
  }
  return map[status || ''] || status || '-'
}

function getStatusType(status?: string): any {
  const map: Record<string, any> = {
    'completed': 'success',
    'processing': 'warning',
    'uploaded': 'info',
    'pending': 'danger',
    'failed': 'danger'
  }
  return map[status || ''] || 'info'
}

function formatTime(time?: string) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

function viewDetail(id: number) {
  router.push(`/materials/${id}`)
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除这份资料吗？删除后可从回收站恢复。', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await deleteMaterial(id, false)
    if (res.code === 200) {
      ElMessage.success('删除成功，已移至回收站')
      await loadMaterials()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.msg || err.message || '删除失败')
    }
  }
}

async function loadMaterials() {
  loading.value = true
  try {
    const res = await getMaterialList()
    if (res.code === 200) {
      materials.value = res.data?.records || []
    } else {
      ElMessage.error(res.msg || '获取资料列表失败')
    }
  } catch (err: any) {
    console.error('获取资料列表失败:', err)
    ElMessage.error(err.response?.data?.msg || err.message || '获取资料列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (isLoggedIn.value) {
    loadMaterials()
  }
})
</script>

<style scoped>
.material-list-page {
  padding: var(--space-6);
}

.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
  flex-wrap: wrap;
  gap: var(--space-3);
}

.page-header-left {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.page-title {
  font-size: var(--text-2xl);
  font-weight: 700;
  margin: 0;
  color: var(--text-primary);
}

.page-subtitle {
  margin: 0;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12);
  gap: var(--space-3);
  color: var(--text-secondary);
}

/* 卡片网格 */
.material-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-5);
}

.material-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: var(--space-3);
  transition: all 0.2s ease;
}

.material-card:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
  border-color: var(--primary-200);
}

.material-icon-wrap {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-hover);
  border-radius: var(--radius-xl);
}

.material-icon {
  font-size: 36px;
  line-height: 1;
}

.material-title {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--space-2);
}

.material-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.material-actions {
  display: flex;
  gap: var(--space-2);
  margin-top: auto;
  padding-top: var(--space-2);
  width: 100%;
  justify-content: center;
}

/* 响应式 */
@media (max-width: 1199px) {
  .material-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 767px) {
  .material-grid {
    grid-template-columns: 1fr;
  }

  .page-header-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
