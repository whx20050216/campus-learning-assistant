<template>
  <div class="page-container upload-view">
    <!-- 全局上传遮罩 -->
    <div v-if="isUploading" class="global-upload-mask">
      <div class="mask-content">
        <el-icon class="mask-loading-icon"><Loading /></el-icon>
        <p class="mask-title">资料上传与识别中</p>
        <p class="mask-sub">请稍候，约需 30 秒...</p>
      </div>
    </div>

    <!-- 未登录提示 -->
    <div v-if="!isLoggedIn" class="empty-state">
      <div class="empty-icon"><el-icon :size="28"><Lock /></el-icon></div>
      <p class="empty-text">请先登录</p>
      <p class="empty-tip">登录后即可上传和管理您的学习资料</p>
      <el-button type="primary" @click="goLogin">去登录</el-button>
    </div>

    <!-- 上传表单 -->
    <div v-else class="upload-card">
      <div class="upload-header">
        <h2 class="page-title">文件上传</h2>
        <p class="page-subtitle">支持 PDF、JPG、PNG 格式，单个文件不超过 50MB</p>
      </div>

      <div class="upload-body">
        <!-- 拖拽区 -->
        <div
          class="drop-zone"
          :class="{ 'has-file': file, 'dragover': isDragOver }"
          @dragenter.prevent="isDragOver = true"
          @dragleave.prevent="isDragOver = false"
          @dragover.prevent
          @drop.prevent="handleDrop"
        >
          <input
            ref="fileInput"
            type="file"
            class="file-input-hidden"
            @change="handleFileChange"
            accept=".pdf,.jpg,.jpeg,.png"
          />
          <template v-if="!file">
            <div class="drop-zone-icon">
              <el-icon :size="48"><Upload /></el-icon>
            </div>
            <div class="drop-zone-title">点击或拖拽文件至此</div>
            <div class="drop-zone-desc">支持格式：PDF、JPG、PNG（最大 50MB）</div>
          </template>
          <template v-else>
            <div class="file-selected-card">
              <el-icon class="file-selected-icon" :size="36"><component :is="getFileIcon(file.name)" /></el-icon>
              <div class="file-selected-info">
                <div class="file-selected-name">{{ file.name }}</div>
                <div class="file-selected-size">{{ formatSize(file.size) }}</div>
              </div>
              <el-button
                type="danger"
                :icon="Delete"
                circle
                size="small"
                class="file-remove-btn"
                @click.stop="clearFile"
              />
            </div>
          </template>
        </div>

        <!-- 资料信息表单 -->
        <div v-if="file" class="upload-meta-form">
          <div class="meta-form-item">
            <label class="meta-form-label">课程标签</label>
            <el-input
              v-model="courseTag"
              placeholder="如：高等数学、期末考试"
              maxlength="20"
              show-word-limit
              clearable
            />
          </div>
          <div class="meta-form-item">
            <label class="meta-form-label">资料页数</label>
            <el-input-number
              v-model="pages"
              placeholder="可选，用于学习计划精确分配"
              :min="1"
              :controls="true"
              style="width: 100%"
            />
          </div>
        </div>

        <!-- 上传进度 -->
        <div v-if="uploading && uploadProgress > 0" class="upload-progress">
          <el-progress :percentage="uploadProgress" :status="uploadProgress === 100 ? 'success' : ''" />
        </div>

        <el-button
          type="primary"
          size="large"
          class="upload-btn"
          :loading="uploading"
          :disabled="!file || uploading"
          @click="uploadFile"
        >
          {{ uploading ? '上传中...' : '上传文件' }}
        </el-button>

        <!-- 上传成功后的 OCR 结果面板 -->
        <div v-if="uploadResult && !uploading" class="ocr-result-panel">
          <div class="ocr-result-header">
            <div class="ocr-result-title">
              <el-icon class="ocr-result-badge" :size="20"><CircleCheck /></el-icon>
              <span>上传成功</span>
            </div>
            <el-button type="primary" size="small" @click="viewDetail">
              查看详情 →
            </el-button>
          </div>

          <div v-if="materialId" class="ocr-result-body">
            <!-- 识别文本 -->
            <div class="ocr-section">
              <div class="ocr-section-title">识别文本</div>
              <div class="ocr-textarea-wrapper">
                <el-input
                  v-model="ocrText"
                  type="textarea"
                  :rows="6"
                  readonly
                  resize="none"
                  class="ocr-textarea"
                />
                <el-button
                  v-if="ocrText"
                  class="ocr-copy-btn"
                  size="small"
                  @click="copyText"
                >
                  <el-icon><CopyDocument /></el-icon> 复制
                </el-button>
              </div>
            </div>

            <!-- 关键词标签 -->
            <div v-if="keywords.length" class="ocr-section">
              <div class="ocr-section-title">关键词</div>
              <div class="keywords-cloud">
                <el-tag
                  v-for="(kw, idx) in keywords"
                  :key="kw"
                  :type="['primary', 'success', 'warning', 'danger', 'info'][idx % 5]"
                  effect="light"
                  class="keyword-tag"
                  size="large"
                >
                  {{ kw }}
                </el-tag>
              </div>
            </div>

            <!-- 摘要 -->
            <div v-if="summary" class="ocr-section">
              <div class="ocr-section-title">摘要</div>
              <blockquote class="summary-block">
                {{ summary }}
              </blockquote>
            </div>

            <!-- 引擎/置信度信息 -->
            <div v-if="engineInfo.engine" class="ocr-section">
              <div class="ocr-section-title">识别信息</div>
              <div class="engine-info">
                <div class="engine-item">
                  <span class="engine-label">引擎</span>
                  <span class="engine-value">{{ engineInfo.engine }}</span>
                </div>
                <div class="engine-item">
                  <span class="engine-label">置信度</span>
                  <span class="engine-value">{{ engineInfo.confidence }}</span>
                </div>
                <div class="engine-item">
                  <span class="engine-label">来源</span>
                  <span class="engine-value">
                    <el-tag
                      :type="getSourceType(engineInfo.source)"
                      size="small"
                      effect="light"
                    >
                      {{ engineInfo.source }}
                    </el-tag>
                  </span>
                </div>
                <div class="engine-item">
                  <span class="engine-label">处理耗时</span>
                  <span class="engine-value">{{ engineInfo.processingTime }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="error && !uploading" class="alert alert-error">
          <strong>上传失败</strong>
          <span>{{ error }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Delete, Loading, Upload, Document, Picture, Link,
  Collection, CircleCheck, Close, CopyDocument
} from '@element-plus/icons-vue'
import { uploadMaterial } from '@/api/upload'

const router = useRouter()
const fileInput = ref<HTMLInputElement | null>(null)
const file = ref<File | null>(null)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadResult = ref(false)
const error = ref('')
const materialId = ref<number | null>(null)
const isDragOver = ref(false)
const isUploading = ref(false)
const courseTag = ref('')
const pages = ref<number | undefined>(undefined)

// OCR 结果展示
const ocrText = ref('')
const keywords = ref<string[]>([])
const summary = ref('')
const engineInfo = ref({
  engine: '',
  confidence: '',
  source: '',
  processingTime: ''
})

const isLoggedIn = computed(() => !!localStorage.getItem('token'))

function goLogin() {
  router.push('/login')
}

function handleDrop(e: DragEvent) {
  isDragOver.value = false
  const dropped = e.dataTransfer?.files[0]
  if (dropped) {
    validateAndSetFile(dropped)
  }
}

function validateAndSetFile(selected: File) {
  error.value = ''
  uploadResult.value = false
  materialId.value = null
  uploadProgress.value = 0

  const ext = '.' + selected.name.split('.').pop()?.toLowerCase()
  if (!ALLOWED_TYPES.includes(ext)) {
    ElMessage.error('仅支持 PDF、JPG、PNG 格式')
    file.value = null
    if (fileInput.value) fileInput.value.value = ''
    return
  }

  if (selected.size > MAX_SIZE_MB * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 50MB')
    file.value = null
    if (fileInput.value) fileInput.value.value = ''
    return
  }

  file.value = selected
}

function clearFile() {
  file.value = null
  courseTag.value = ''
  pages.value = undefined
  if (fileInput.value) fileInput.value.value = ''
}

function getFileIcon(name: string) {
  const ext = name.split('.').pop()?.toLowerCase()
  if (ext === 'pdf') return Document
  if (['jpg', 'jpeg', 'png'].includes(ext || '')) return Picture
  return Link
}

function copyText() {
  if (!ocrText.value) return
  navigator.clipboard.writeText(ocrText.value).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

function getSourceType(source: string) {
  const map: Record<string, string> = {
    'local': 'primary',
    'ai_enhanced': 'success',
    'fallback': 'warning',
    'manual_corrected': 'info'
  }
  return map[source] || 'info'
}

function formatSize(bytes: number) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const ALLOWED_TYPES = ['.pdf', '.jpg', '.jpeg', '.png']
const MAX_SIZE_MB = 50

const handleFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const selected = target.files ? target.files[0] : null
  // 重置 input，防止同一文件重复触发
  if (fileInput.value) {
    fileInput.value.value = ''
  }
  if (!selected) {
    file.value = null
    return
  }
  validateAndSetFile(selected)
}

const uploadFile = async () => {
  if (!file.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  uploading.value = true
  uploadProgress.value = 0
  uploadResult.value = false
  error.value = ''
  materialId.value = null

  isUploading.value = true
  localStorage.setItem('uploading', 'true')

  // 阻止浏览器刷新/关闭
  const beforeUnloadHandler = (e: BeforeUnloadEvent) => {
    e.preventDefault()
    e.returnValue = '上传尚未完成，确定要离开吗？'
  }
  window.addEventListener('beforeunload', beforeUnloadHandler)

  try {
    const res: any = await uploadMaterial(
      file.value,
      courseTag.value || undefined,
      pages.value,
      (progressEvent) => {
        if (progressEvent.total) {
          uploadProgress.value = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        }
      }
    )

    if (res.code === 200) {
      uploadResult.value = true
      materialId.value = res.data?.id ?? null
      ElMessage.success('上传成功')

      // 解析并展示 OCR 结果
      const data = res.data || {}
      ocrText.value = data.ocrResult?.ocrText || data.ocrText || ''
      summary.value = data.ocrResult?.summary || data.summary || ''

      // 关键词
      const kwRaw = data.keywords || data.ocrResult?.keywords
      if (Array.isArray(kwRaw)) {
        keywords.value = kwRaw.map((k: any) => (typeof k === 'string' ? k : k.keyword))
      } else if (typeof kwRaw === 'string') {
        keywords.value = kwRaw.split(',').map((k: string) => k.trim()).filter(Boolean)
      } else {
        keywords.value = []
      }

      // 引擎信息
      const ocr = data.ocrResult || {}
      engineInfo.value = {
        engine: ocr.engine || data.engine || '-',
        confidence: ocr.confidence ? (ocr.confidence * 100).toFixed(1) + '%' : (data.confidence ? (data.confidence * 100).toFixed(1) + '%' : '-'),
        source: ocr.source || data.source || '-',
        processingTime: ocr.processingTimeMs ? ocr.processingTimeMs + 'ms' : (data.processingTimeMs ? data.processingTimeMs + 'ms' : '-')
      }
    } else if (res.code === 408 && res.data?.id) {
      // OCR 超时但文件已上传成功，返回 materialId 引导用户去资料库查看
      uploadResult.value = true
      materialId.value = res.data.id
      ElMessage.warning(res.msg || '识别处理超时，资料已保存，请稍后查看')
    } else {
      error.value = res.msg || '上传失败'
      ElMessage.error(error.value)
    }
  } catch (err: any) {
    let msg = '上传失败'
    if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
      msg = '上传超时，请检查网络或尝试上传更小文件'
    } else if (err.response?.status === 403) {
      msg = '权限不足，请重新登录'
    } else if (err.response?.status === 413) {
      msg = '文件过大，服务端拒绝接收'
    } else if (err.response?.status >= 500) {
      msg = '服务器内部错误，请稍后重试'
    } else if (err.response?.data?.msg) {
      msg = err.response.data.msg
    } else if (err.message) {
      msg = err.message
    }
    error.value = msg
    ElMessage.error(msg)
  } finally {
    uploading.value = false
    uploadProgress.value = 0
    isUploading.value = false
    localStorage.removeItem('uploading')
    window.removeEventListener('beforeunload', beforeUnloadHandler)
  }
}

function viewDetail() {
  if (materialId.value) {
    router.push(`/materials/${materialId.value}`)
  } else {
    ElMessage.success('上传成功，请前往我的资料库查看')
    router.push('/materials')
  }
}
</script>

<style scoped>
.upload-view {
  min-height: calc(100vh - var(--navbar-height));
}

.upload-card {
  max-width: 720px;
  margin: 0 auto;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-8);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.upload-header {
  text-align: center;
  margin-bottom: var(--space-6);
}

.upload-header .page-title {
  margin-bottom: var(--space-2);
}

.upload-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

/* 拖拽区 */
.drop-zone {
  border: 2px dashed var(--border-color);
  border-radius: var(--radius-lg);
  padding: var(--space-10) var(--space-6);
  text-align: center;
  cursor: pointer;
  transition: all 0.25s ease;
  background: var(--bg-hover);
  position: relative;
}

.drop-zone:hover,
.drop-zone.dragover {
  border-color: var(--primary-500);
  background: var(--primary-50);
}

.file-input-hidden {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.drop-zone-icon {
  margin-bottom: var(--space-3);
  color: var(--primary-500);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.drop-zone-title {
  font-size: var(--text-lg);
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: var(--space-1);
}

.drop-zone-desc {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
}

/* 文件选中卡片 */
.file-selected-card {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4);
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
}

.file-selected-icon {
  color: var(--primary-500);
}

.file-selected-info {
  flex: 1;
  text-align: left;
  min-width: 0;
}

.file-selected-name {
  font-size: var(--text-base);
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-selected-size {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-top: 2px;
}

.file-remove-btn {
  flex-shrink: 0;
}

.upload-progress {
  padding: var(--space-2) 0;
}

.upload-btn {
  width: 100%;
  margin-top: var(--space-2);
  font-size: var(--text-lg);
  font-weight: 500;
  border-radius: var(--radius-md);
}

.upload-meta-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-top: var(--space-2);
}

.meta-form-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.meta-form-label {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  font-weight: 500;
}

.alert {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-4);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  line-height: 1.5;
}

.alert-success {
  background: var(--success-50);
  border: 1px solid var(--success-500);
  color: var(--success-500);
}

.alert-error {
  background: var(--danger-50);
  border: 1px solid var(--danger-500);
  color: var(--danger-500);
}

.alert strong {
  font-weight: 600;
}

.alert span {
  opacity: 0.9;
}

/* OCR 结果面板 */
.ocr-result-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  margin-top: var(--space-2);
}

.ocr-result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ocr-result-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-weight: 600;
  color: var(--text-primary);
  font-size: var(--text-base);
}

.ocr-result-badge {
  color: var(--success-500);
  margin-right: var(--space-2);
  display: inline-flex;
  align-items: center;
}

.ocr-result-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.ocr-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.ocr-section-title {
  font-weight: 600;
  font-size: var(--text-sm);
  color: var(--text-primary);
}

/* 识别文本 */
.ocr-textarea-wrapper {
  position: relative;
}

.ocr-textarea :deep(.el-textarea__inner) {
  background: var(--bg-hover);
  font-family: inherit;
  font-size: var(--text-sm);
  line-height: 1.7;
  border-radius: var(--radius-md);
  padding: var(--space-3);
}

.ocr-copy-btn {
  position: absolute;
  top: var(--space-2);
  right: var(--space-2);
}

/* 关键词云 */
.keywords-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.keyword-tag {
  font-size: var(--text-sm);
}

/* 摘要引用块 */
.summary-block {
  margin: 0;
  padding: var(--space-3) var(--space-4);
  background: var(--bg-hover);
  border-left: 3px solid var(--primary-500);
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: 1.7;
}

/* 引擎信息 */
.engine-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: var(--space-4);
  background: var(--bg-hover);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
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

/* 全局上传遮罩 */
.global-upload-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: all;
}

.mask-content {
  text-align: center;
  color: var(--primary-500);
}

.mask-loading-icon {
  font-size: 48px;
  animation: rotate 1s linear infinite;
}

.mask-title {
  font-size: 18px;
  font-weight: 600;
  margin-top: 16px;
  color: var(--text-primary);
}

.mask-sub {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 8px;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 480px) {
  .upload-card {
    padding: var(--space-5) var(--space-4);
  }
}
</style>
