<template>
  <div class="page-container material-detail-page">
    <!-- 未登录 -->
    <div v-if="!isLoggedIn" class="empty-state" style="margin-top: var(--space-8)">
      <div class="empty-icon">🔒</div>
      <p class="empty-text">请先登录</p>
      <el-button type="primary" @click="goLogin">去登录</el-button>
    </div>

    <template v-else>
      <!-- 加载中 -->
      <div v-if="loading" class="empty-state">
        <el-icon class="is-loading" size="32"><Loading /></el-icon>
        <p class="empty-text">加载中...</p>
      </div>

      <!-- 错误 -->
      <div v-else-if="error" class="empty-state">
        <div class="empty-icon">⚠️</div>
        <p class="empty-text">{{ error }}</p>
        <el-button type="primary" @click="goBack">返回</el-button>
      </div>

      <!-- 内容 -->
      <template v-else-if="material">
        <!-- 顶部操作栏 -->
        <div class="detail-header">
          <el-button link @click="goBack">
            ← 返回
          </el-button>
          <div class="detail-actions">
            <el-button type="danger" size="small" @click="handleDelete">
              🗑️ 删除
            </el-button>
          </div>
        </div>

        <div class="detail-grid">
          <!-- 左侧：文件预览/信息 -->
          <aside class="detail-sidebar">
            <div class="preview-card">
              <div class="preview-title">📄 文件信息</div>
              <div class="preview-content">
                <!-- 图片预览 -->
                <img
                  v-if="isImage"
                  :src="material.fileUrl"
                  alt="资料预览"
                  class="preview-image"
                />
                <!-- PDF 或其他 -->
                <div v-else class="preview-placeholder">
                  <span class="preview-icon">{{ getFileIcon(material.fileType) }}</span>
                  <span class="preview-type">{{ material.fileType || '未知格式' }}</span>
                </div>
              </div>
              <div class="file-meta-list">
                <div class="file-meta-item">
                  <span class="meta-label">文件名</span>
                  <span class="meta-value" :title="material.title">{{ material.title }}</span>
                </div>
                <div class="file-meta-item">
                  <span class="meta-label">类型</span>
                  <span class="meta-value">{{ material.fileType || '-' }}</span>
                </div>
                <div class="file-meta-item">
                  <span class="meta-label">大小</span>
                  <span class="meta-value">{{ formatSize(material.fileSize) }}</span>
                </div>
                <div class="file-meta-item">
                  <span class="meta-label">页数</span>
                  <span class="meta-value">{{ material.pages ?? '-' }}</span>
                </div>
                <div class="file-meta-item">
                  <span class="meta-label">课程标签</span>
                  <span class="meta-value">{{ material.courseTag || '-' }}</span>
                </div>
                <div class="file-meta-item">
                  <span class="meta-label">状态</span>
                  <span class="meta-value">
                    <el-tag :type="getStatusType(material.status)" size="small">
                      {{ getStatusText(material.status) }}
                    </el-tag>
                  </span>
                </div>
                <div class="file-meta-item">
                  <span class="meta-label">上传时间</span>
                  <span class="meta-value">{{ formatTime(material.createdAt) }}</span>
                </div>
              </div>
            </div>
          </aside>

          <!-- 右侧：OCR + 关键词 -->
          <main class="detail-main">
            <!-- OCR 文本 -->
            <section class="detail-section">
              <h2 class="section-title">📝 OCR 识别文本</h2>
              <div v-if="material.ocrResult?.ocrText" class="ocr-text-box">
                <pre>{{ material.ocrResult.ocrText }}</pre>
              </div>
              <div v-else class="empty-state empty-state-flat">
                <p class="empty-text">暂无 OCR 结果</p>
              </div>
            </section>

            <!-- 摘要 -->
            <section v-if="material.ocrResult?.summary" class="detail-section">
              <h2 class="section-title">📋 文本摘要</h2>
              <p class="summary-text">{{ material.ocrResult.summary }}</p>
            </section>

            <!-- 关键词 -->
            <section class="detail-section">
              <h2 class="section-title">🏷️ 关键词</h2>
              <div v-if="material.keywords?.length" class="keywords-grid">
                <el-tag
                  v-for="kw in material.keywords"
                  :key="kw.keyword"
                  type="primary"
                  effect="light"
                  class="keyword-tag"
                >
                  {{ kw.keyword }}
                  <span v-if="kw.weight" class="keyword-weight">({{ (kw.weight * 100).toFixed(1) }}%)</span>
                </el-tag>
              </div>
              <div v-else class="empty-state empty-state-flat">
                <p class="empty-text">暂无关键词</p>
              </div>
            </section>

            <!-- 识别引擎信息 -->
            <section v-if="material.ocrResult" class="detail-section">
              <h2 class="section-title">⚙️ 识别信息</h2>
              <div class="engine-info">
                <div class="engine-item">
                  <span class="engine-label">引擎</span>
                  <span class="engine-value">{{ material.ocrResult.engine || '-' }}</span>
                </div>
                <div class="engine-item">
                  <span class="engine-label">置信度</span>
                  <span class="engine-value">{{ material.ocrResult.confidence ? (material.ocrResult.confidence * 100).toFixed(1) + '%' : '-' }}</span>
                </div>
                <div class="engine-item">
                  <span class="engine-label">来源</span>
                  <span class="engine-value">{{ material.ocrResult.source || '-' }}</span>
                </div>
                <div class="engine-item">
                  <span class="engine-label">处理耗时</span>
                  <span class="engine-value">{{ material.ocrResult.processingTimeMs ? material.ocrResult.processingTimeMs + 'ms' : '-' }}</span>
                </div>
              </div>
            </section>
          </main>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getMaterialDetail, deleteMaterial } from '@/api/material'

const route = useRoute()
const router = useRouter()
const isLoggedIn = computed(() => !!localStorage.getItem('token'))

const material = ref<any>(null)
const loading = ref(false)
const error = ref('')

const isImage = computed(() => {
  const type = material.value?.fileType?.toUpperCase?.()
  return type === 'IMAGE' || type === 'JPG' || type === 'JPEG' || type === 'PNG'
})

function goLogin() {
  router.push('/login')
}

function goBack() {
  router.back()
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
  return new Date(time).toLocaleString('zh-CN')
}

function getFileIcon(type?: string) {
  const icons: Record<string, string> = {
    'PDF': '📄',
    'PPT': '📊',
    'IMAGE': '🖼️'
  }
  return icons[type?.toUpperCase() || ''] || '📎'
}

function getStatusText(status?: string) {
  const map: Record<string, string> = {
    'completed': '已完成',
    'processing': '处理中',
    'uploaded': '已上传',
    'pending': '待审核'
  }
  return map[status || ''] || status || '-'
}

function getStatusType(status?: string): any {
  const map: Record<string, any> = {
    'completed': 'success',
    'processing': 'warning',
    'uploaded': 'info',
    'pending': 'danger'
  }
  return map[status || ''] || 'info'
}

async function loadDetail() {
  const id = route.params.id
  if (!id) {
    error.value = '资料 ID 无效'
    return
  }

  loading.value = true
  error.value = ''
  try {
    const res = await getMaterialDetail(id as string)
    if (res.code === 200) {
      material.value = res.data
    } else {
      error.value = res.msg || '获取资料详情失败'
    }
  } catch (err: any) {
    error.value = err.response?.data?.msg || '获取资料详情失败'
  } finally {
    loading.value = false
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除这份资料吗？删除后不可恢复。', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const id = route.params.id
    const res = await deleteMaterial(id as string)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      router.push('/search')
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.msg || '删除失败')
    }
  }
}

onMounted(() => {
  if (isLoggedIn.value) {
    loadDetail()
  }
})
</script>

<style scoped>
.material-detail-page {
  padding: var(--space-6);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
  max-width: var(--page-max-width);
  margin-left: auto;
  margin-right: auto;
}

.detail-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: var(--space-6);
  max-width: var(--page-max-width);
  margin: 0 auto;
  align-items: start;
}

/* 侧边栏 */
.detail-sidebar {
  position: sticky;
  top: calc(var(--navbar-height) + var(--space-6));
}

.preview-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.preview-title {
  padding: var(--space-4);
  font-weight: 600;
  font-size: var(--text-base);
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-hover);
}

.preview-content {
  padding: var(--space-4);
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 160px;
}

.preview-image {
  max-width: 100%;
  max-height: 280px;
  border-radius: var(--radius-md);
  object-fit: contain;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
}

.preview-icon {
  font-size: 48px;
}

.preview-type {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
}

.file-meta-list {
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.file-meta-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--text-sm);
}

.meta-label {
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.meta-value {
  color: var(--text-primary);
  font-weight: 500;
  text-align: right;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 主内容 */
.detail-main {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.detail-section {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.section-title {
  font-size: var(--text-lg);
  font-weight: 600;
  margin-bottom: var(--space-4);
  color: var(--text-primary);
}

.ocr-text-box {
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  max-height: 400px;
  overflow-y: auto;
}

.ocr-text-box pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: var(--text-sm);
  line-height: 1.7;
  color: var(--text-secondary);
}

.summary-text {
  color: var(--text-secondary);
  line-height: 1.7;
  font-size: var(--text-sm);
}

.keywords-grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.keyword-tag {
  font-size: var(--text-sm);
}

.keyword-weight {
  opacity: 0.7;
  font-size: var(--text-xs);
  margin-left: 2px;
}

.engine-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: var(--space-4);
}

.engine-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.engine-label {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.engine-value {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--text-primary);
}

/* 响应式 */
@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-sidebar {
    position: static;
  }
}
</style>
