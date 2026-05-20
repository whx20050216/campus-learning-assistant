<template>
  <div class="page-container upload-view">
    <!-- 未登录提示 -->
    <div v-if="!isLoggedIn" class="empty-state">
      <div class="empty-icon">🔒</div>
      <p class="empty-text">请先登录</p>
      <p class="empty-tip">登录后即可上传和管理您的学习资料</p>
      <el-button type="primary" @click="goLogin">去登录</el-button>
    </div>

    <!-- 上传表单 -->
    <div v-else class="upload-card">
      <div class="upload-header">
        <h2 class="page-title">📁 文件上传</h2>
        <p class="page-subtitle">支持 PDF、PPT、图片等多种格式</p>
      </div>

      <div class="upload-body">
        <div class="form-group">
          <label>选择文件</label>
          <div class="file-input-wrapper">
            <input
              type="file"
              class="file-input"
              @change="handleFileChange"
              accept=".pdf,.ppt,.pptx,.jpg,.jpeg,.png"
            />
            <div class="file-input-hint">
              支持格式：PDF、PPT、PPTX、JPG、PNG
            </div>
          </div>
        </div>

        <div v-if="file" class="file-preview">
          <span class="file-preview-name">📄 {{ file.name }}</span>
          <span class="file-preview-size">{{ formatSize(file.size) }}</span>
        </div>

        <button
          class="btn-primary upload-btn"
          :disabled="!file || uploading"
          @click="uploadFile"
        >
          {{ uploading ? '⏳ 上传中...' : '📤 上传文件' }}
        </button>

        <div v-if="result" class="alert alert-success">
          <strong>✅ 上传成功</strong>
          <span>{{ result }}</span>
        </div>

        <div v-if="error" class="alert alert-error">
          <strong>❌ 上传失败</strong>
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
import { uploadMaterial } from '@/api/upload'

const router = useRouter()
const file = ref<File | null>(null)
const uploading = ref(false)
const result = ref('')
const error = ref('')

const isLoggedIn = computed(() => !!localStorage.getItem('token'))

function goLogin() {
  router.push('/login')
}

function formatSize(bytes: number) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const handleFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  file.value = target.files ? target.files[0] : null
  result.value = ''
  error.value = ''
}

const uploadFile = async () => {
  if (!file.value) {
    error.value = '请先选择文件'
    return
  }

  uploading.value = true
  result.value = ''
  error.value = ''

  const formData = new FormData()
  formData.append('file', file.value)

  try {
    const res: any = await uploadMaterial(formData)
    result.value = res.msg || res.data || '上传成功'
    ElMessage.success('上传成功')
  } catch (err: any) {
    if (err.response?.status === 401) {
      error.value = '登录已过期，请重新登录'
      ElMessage.warning('登录已过期，请重新登录')
    } else {
      error.value = err.response?.data?.msg || err.message || '上传失败'
    }
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.upload-view {
  min-height: calc(100vh - var(--navbar-height));
}

.upload-card {
  max-width: 560px;
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

.file-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.file-input {
  width: 100%;
  padding: var(--space-3);
  border: 2px dashed var(--border-color);
  border-radius: var(--radius-md);
  background: var(--bg-hover);
  cursor: pointer;
  transition: border-color 0.2s ease;
  font-size: var(--text-sm);
}

.file-input:hover {
  border-color: var(--primary-500);
}

.file-input-hint {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.file-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
}

.file-preview-name {
  color: var(--text-primary);
  font-weight: 500;
}

.file-preview-size {
  color: var(--text-tertiary);
  font-size: var(--text-xs);
}

.upload-btn {
  width: 100%;
  margin-top: var(--space-2);
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
</style>
