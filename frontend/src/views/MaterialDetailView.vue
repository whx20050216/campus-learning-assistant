<template>
  <div class="page-container material-detail-page">
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

      <!-- 错误 -->
      <div v-else-if="error" class="empty-state">
        <div class="empty-icon"><el-icon :size="28"><Warning /></el-icon></div>
        <p class="empty-text">{{ error }}</p>
        <el-button type="primary" @click="goBack">返回</el-button>
      </div>

      <!-- 内容 -->
      <template v-else-if="material">
        <!-- 顶部操作栏 -->
        <div class="detail-header">
          <div class="detail-breadcrumb">
            <el-button link @click="router.push('/')">
              <el-icon><House /></el-icon> 首页
            </el-button>
            <span class="breadcrumb-sep">/</span>
            <el-button link @click="router.push('/search')">
              搜索
            </el-button>
            <span class="breadcrumb-sep">/</span>
            <span class="breadcrumb-current" :title="material.title">{{ material.title }}</span>
          </div>
          <div class="detail-actions">
            <el-button @click="goBack">
              <el-icon><ArrowLeft /></el-icon> 返回
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </div>
        </div>

        <div class="detail-grid">
          <!-- 左侧：文件预览/信息 -->
          <aside class="detail-sidebar">
            <div class="preview-card">
              <div class="preview-title">文件信息</div>
              <div class="preview-content">
                <!-- 图片预览 -->
                <img
                  v-if="isImage"
                  :src="previewBlobUrl"
                  alt="资料预览"
                  class="preview-image"
                />
                <!-- PDF 或其他 -->
                <div v-else class="preview-placeholder">
                  <el-icon class="preview-icon" :size="48"><component :is="getFileIcon(material.fileType)" /></el-icon>
                  <span class="preview-type">{{ material.fileType || '未知格式' }}</span>
                  <span v-if="material.pages" class="preview-pages">{{ material.pages }} 页</span>
                </div>
              </div>
              <div class="file-meta-actions" style="padding: var(--space-3) var(--space-4) 0; display: flex; justify-content: flex-end; gap: 8px;">
                <template v-if="!infoEditing">
                  <el-button size="small" @click="startInfoEdit">
                    <el-icon><Edit /></el-icon> 编辑资料信息
                  </el-button>
                </template>
                <template v-else>
                  <el-button size="small" type="primary" :loading="savingInfo" @click="saveInfoEdit">保存</el-button>
                  <el-button size="small" @click="cancelInfoEdit">取消</el-button>
                </template>
              </div>
              <div class="file-meta-list">
                <div class="file-meta-item">
                  <span class="meta-label">文件名</span>
                  <template v-if="infoEditing">
                    <el-input v-model="editTitle" size="small" style="flex: 1; max-width: 160px;" />
                  </template>
                  <span v-else class="meta-value" :title="material.title">{{ material.title }}</span>
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
                  <template v-if="infoEditing">
                    <el-input-number v-model="editPages" :min="1" size="small" style="flex: 1; max-width: 120px;" />
                  </template>
                  <span v-else class="meta-value">{{ material.pages ?? '-' }}</span>
                </div>
                <div class="file-meta-item">
                  <span class="meta-label">课程标签</span>
                  <template v-if="infoEditing">
                    <el-input v-model="editCourseTag" size="small" maxlength="20" style="flex: 1; max-width: 160px;" />
                  </template>
                  <span v-else class="meta-value">{{ material.courseTag || '-' }}</span>
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
              <div class="file-actions" style="margin-top: 12px;">
                <el-button
                  v-if="isPdf"
                  type="primary"
                  size="small"
                  @click="handleDownload"
                >
                  ⬇️ 下载原始 PDF
                </el-button>
                <el-button
                  v-else-if="isImage"
                  type="primary"
                  size="small"
                  @click="handleDownload"
                >
                  ⬇️ 下载原图
                </el-button>
              </div>
              <!-- 文件预览区域 -->
              <div class="preview-section" v-if="material">
                <div class="preview-header">
                  <el-icon><View /></el-icon>
                  <span>文件预览</span>
                </div>
                <div class="preview-content">
                  <iframe
                    v-if="isPdf"
                    :src="previewBlobUrl"
                    width="100%"
                    height="400px"
                    frameborder="0"
                  ></iframe>
                  <img
                    v-else-if="isImage"
                    :src="previewBlobUrl"
                    style="max-width: 100%; border-radius: 8px;"
                  />
                  <div v-else class="unsupported-tip">
                    <el-icon><Document /></el-icon>
                    <span>该格式暂不支持预览，请下载查看</span>
                  </div>
                </div>
              </div>
            </div>
          </aside>

          <!-- 右侧：OCR + 关键词 -->
          <main class="detail-main">
            <!-- 处理中状态 -->
            <div v-if="material.status === 'processing'" class="detail-section processing-section">
              <div class="processing-state">
                <el-icon class="is-loading" size="40"><Loading /></el-icon>
                <p class="processing-text">识别中，请稍候...</p>
                <p class="processing-tip">OCR 引擎正在处理您的文件，完成后将自动展示识别结果</p>
              </div>
            </div>

            <!-- OCR 处理失败状态 -->
            <div v-else-if="material.status === 'failed'" class="detail-section processing-section">
              <div class="processing-state">
                <div class="empty-icon">⚠️</div>
                <p class="processing-text">OCR 处理失败</p>
                <p class="processing-tip">文件已保存，但识别过程出现异常。可尝试重新上传或压缩后重试。</p>
              </div>
            </div>

            <!-- OCR 文本 -->
            <section v-else class="detail-section ocr-section-card">
              <div class="section-title-row">
                <div class="section-title-left">
                  <h2 class="section-title">识别结果</h2>
                  <el-tag
                    v-if="material.ocrResult?.source"
                    size="small"
                    :type="getSourceType(material.ocrResult.source)"
                    class="source-badge"
                    :style="getSourceStyle(material.ocrResult.source)"
                  >
                    {{ getSourceLabel(material.ocrResult.source) }}
                  </el-tag>
                </div>
                <div class="ocr-actions">
                  <template v-if="editing">
                    <el-button size="small" type="primary" :loading="loading" @click="saveOcrText">
                      <el-icon><Check /></el-icon> 保存
                    </el-button>
                    <el-button size="small" @click="cancelEdit">
                      <el-icon><Close /></el-icon> 取消
                    </el-button>
                  </template>
                  <template v-else>
                    <el-button
                      v-if="material.ocrResult?.ocrText"
                      size="small"
                      @click="startEdit"
                    >
                      <el-icon><Edit /></el-icon> 编辑
                    </el-button>
                    <el-button
                      v-if="material.ocrResult?.ocrText"
                      size="small"
                      @click="copyOcrText"
                    >
                      <el-icon><DocumentCopy /></el-icon> 复制
                    </el-button>
                  </template>
                </div>
              </div>
              <div v-if="material.ocrResult?.ocrText || editing" class="ocr-text-box">
                <el-input
                  v-if="editing"
                  v-model="editText"
                  type="textarea"
                  :rows="10"
                  resize="vertical"
                  placeholder="请输入校正后的 OCR 文本"
                  maxlength="5000"
                  show-word-limit
                />
                <pre v-else>{{ material.ocrResult.ocrText }}</pre>
              </div>
              <div v-else class="empty-state empty-state-flat">
                <p class="empty-text">暂无 OCR 结果</p>
              </div>
            </section>

            <!-- 摘要 -->
            <section v-if="material.ocrResult?.summary" class="detail-section">
              <div class="section-title-row">
                <h2 class="section-title">文本摘要</h2>
                <el-button
                  size="small"
                  type="primary"
                  :loading="enhancingSummary.value"
                  :disabled="material.ocrResult?.source === 'ai_enhanced_summary'"
                  @click="handleAiEnhanceSummary"
                >
                  {{ material.ocrResult?.source === 'ai_enhanced_summary' ? '已 AI 增强' : 'AI 增强摘要' }}
                </el-button>
              </div>
              <blockquote class="summary-block">
                {{ material.ocrResult.summary }}
              </blockquote>
            </section>

            <!-- 关键词 -->
            <section class="detail-section">
              <div class="section-title-row">
                <h2 class="section-title">关键词</h2>
                <el-button size="small" type="primary" :loading="savingKeywords" @click="saveKeywords">
                  保存关键词
                </el-button>
              </div>
              <div v-if="material.keywords?.length" class="keywords-cloud">
                <el-tag
                  v-for="(kw, idx) in material.keywords"
                  :key="kw.keyword + idx"
                  :type="['primary', 'success', 'warning', 'danger', 'info'][idx % 5]"
                  effect="light"
                  class="keyword-tag"
                  size="large"
                  closable
                  @close="removeKeyword(idx)"
                >
                  {{ kw.keyword }}
                </el-tag>
              </div>
              <div v-else class="empty-state empty-state-flat">
                <p class="empty-text">暂无关键词，可手动添加</p>
              </div>
              <div class="keyword-input-row" style="margin-top: var(--space-3); display: flex; gap: var(--space-2);">
                <el-input
                  v-model="newKeyword"
                  placeholder="输入新关键词"
                  size="small"
                  maxlength="100"
                  @keyup.enter="addKeyword"
                />
                <el-button size="small" @click="addKeyword">添加</el-button>
              </div>
            </section>

            <!-- 知识点 -->
            <section class="detail-section">
              <div class="section-header">
                <el-icon><Collection /></el-icon>
                <span>知识点</span>
              </div>
              <div v-if="knowledgePoints && knowledgePoints.length > 0" class="knowledge-list">
                <el-tag
                  v-for="(kp, index) in knowledgePoints"
                  :key="index"
                  :type="getKnowledgeTypeColor(kp.type)"
                  class="knowledge-tag"
                  effect="light"
                >
                  {{ kp.content }}
                </el-tag>
              </div>
              <div v-else class="empty-knowledge">
                <el-icon><InfoFilled /></el-icon>
                <span>暂无知识点</span>
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
                  <span class="engine-value">
                    <el-tag
                      size="small"
                      :type="getSourceType(material.ocrResult.source)"
                      :style="getSourceStyle(material.ocrResult.source)"
                    >
                      {{ getSourceLabel(material.ocrResult.source) }}
                    </el-tag>
                  </span>
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, House, ArrowLeft, Delete, Edit, DocumentCopy, Check, Close, View, Document, Collection, InfoFilled } from '@element-plus/icons-vue'
import { getMaterialDetail, deleteMaterial, updateOcrText, updateKeywords, downloadMaterial, aiEnhanceSummary, updateMaterialInfo } from '@/api/material'

const route = useRoute()
const router = useRouter()
const isLoggedIn = computed(() => !!localStorage.getItem('token'))

const material = ref<any>(null)
const loading = ref(false)
const error = ref('')
const editing = ref(false)
const editText = ref('')
const newKeyword = ref('')
const savingKeywords = ref(false)
const enhancingSummary = ref(false)
const previewBlobUrl = ref('')
const infoEditing = ref(false)
const editTitle = ref('')
const editPages = ref<number | undefined>(undefined)
const editCourseTag = ref('')
const savingInfo = ref(false)

const isImage = computed(() => {
  const type = material.value?.fileType?.toUpperCase?.()
  return type === 'IMAGE' || type === 'JPG' || type === 'JPEG' || type === 'PNG'
})

const isPdf = computed(() => {
  const type = material.value?.fileType?.toUpperCase?.()
  return type === 'PDF' || material.value?.fileType?.includes('pdf')
})

const knowledgePoints = computed(() => {
  return material.value?.knowledgePoints || []
})

function getKnowledgeTypeColor(type?: string): any {
  const map: Record<string, any> = {
    'definition': 'primary',
    'formula': 'success',
    'theorem': 'warning',
    'example': 'info'
  }
  return map[type || ''] || 'default'
}

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
  const icons: Record<string, any> = {
    'PDF': Document,
    'PPT': Document,
    'IMAGE': Picture
  }
  return icons[type?.toUpperCase() || ''] || Link
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

function getSourceType(source?: string): any {
  const map: Record<string, any> = {
    'local': 'primary',
    'ai_enhanced': 'success',
    'ai_enhanced_summary': 'success',
    'fallback': 'warning',
    'manual_corrected': 'success'
  }
  return map[source || ''] || 'info'
}

function getSourceLabel(source?: string): string {
  const map: Record<string, string> = {
    'local': '本地识别',
    'ai_enhanced': 'AI 增强',
    'ai_enhanced_summary': 'AI 增强摘要',
    'fallback': '降级处理',
    'manual_corrected': '人工修正'
  }
  return map[source || ''] || source || '-'
}

function getSourceStyle(source?: string) {
  if (source === 'ai_enhanced' || source === 'ai_enhanced_summary') {
    return { background: 'var(--primary-50)', borderColor: 'var(--primary-500)', color: 'var(--primary-500)' }
  }
  return {}
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
      await loadPreview()
    } else {
      error.value = res.msg || '获取资料详情失败'
      ElMessage.error(error.value)
    }
  } catch (err: any) {
    error.value = err.response?.data?.msg || err.message || '获取资料详情失败'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除这份资料吗？删除后可从回收站恢复。', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const id = route.params.id
    const res = await deleteMaterial(id as string, false)
    if (res.code === 200) {
      ElMessage.success('删除成功，已移至回收站')
      router.push('/search')
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.msg || err.message || '删除失败')
    }
  }
}

function startEdit() {
  editText.value = material.value?.ocrResult?.ocrText || ''
  editing.value = true
}

function cancelEdit() {
  editing.value = false
  editText.value = ''
}

async function saveOcrText() {
  const id = route.params.id
  if (!id) return
  try {
    const res = await updateOcrText(id as string, editText.value)
    if (res.code === 200) {
      ElMessage.success('修改已保存')
      editing.value = false
      // 更新本地显示
      if (material.value?.ocrResult) {
        material.value.ocrResult.ocrText = editText.value
        material.value.ocrResult.source = 'manual_corrected'
      }
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.msg || err.message || '保存失败')
  }
}

function copyOcrText() {
  const text = material.value?.ocrResult?.ocrText || ''
  if (!text) return
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

async function loadPreview() {
  if (!material.value) return
  const type = material.value.fileType?.toUpperCase?.() || 'PDF'
  const mimeType = type === 'PDF' ? 'application/pdf'
                 : type === 'PNG' ? 'image/png'
                 : 'image/jpeg'
  try {
    const res = await downloadMaterial(material.value.id, true)
    const blob = new Blob([res.data], { type: mimeType })
    previewBlobUrl.value = URL.createObjectURL(blob)
  } catch (err) {
    console.error('预览加载失败', err)
  }
}

async function handleDownload() {
  if (!material.value) {
    ElMessage.error('资料信息未加载')
    return
  }
  try {
    const res = await downloadMaterial(material.value.id)
    const type = material.value.fileType?.toUpperCase?.() || 'PDF'
    const mimeType = type === 'PDF' ? 'application/pdf'
                   : type === 'PNG' ? 'image/png'
                   : 'image/jpeg'
    const blob = new Blob([res.data], { type: mimeType })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = material.value.title || 'download'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (err) {
    ElMessage.error('下载失败，请稍后重试')
    console.error(err)
  }
}

function removeKeyword(index: number) {
  if (material.value?.keywords) {
    material.value.keywords.splice(index, 1)
  }
}

function addKeyword() {
  const kw = newKeyword.value.trim()
  if (!kw) return
  if (!material.value) return
  if (!material.value.keywords) {
    material.value.keywords = []
  }
  if (material.value.keywords.length >= 20) {
    ElMessage.warning('最多添加 20 个关键词')
    return
  }
  if (material.value.keywords.some((k: any) => k.keyword === kw)) {
    ElMessage.warning('关键词已存在')
    return
  }
  material.value.keywords.push({ keyword: kw, weight: 1.0 })
  newKeyword.value = ''
}

async function saveKeywords() {
  const id = route.params.id
  if (!id || !material.value) return
  const keywords = (material.value.keywords || []).map((k: any) => k.keyword)
  savingKeywords.value = true
  try {
    const res = await updateKeywords(id as string, keywords)
    if (res.code === 200) {
      ElMessage.success('关键词已保存')
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.msg || err.message || '保存失败')
  } finally {
    savingKeywords.value = false
  }
}

async function handleAiEnhanceSummary() {
  const id = route.params.id
  if (!id || !material.value) return
  enhancingSummary.value = true
  try {
    const res = await aiEnhanceSummary(id as string)
    if (res.code === 200) {
      ElMessage.success('AI 增强完成')
      if (material.value.ocrResult) {
        material.value.ocrResult.summary = res.data
        material.value.ocrResult.source = 'ai_enhanced_summary'
      }
    } else {
      ElMessage.error(res.msg || '增强失败，请稍后重试')
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.msg || err.message || '增强失败，请稍后重试')
  } finally {
    enhancingSummary.value = false
  }
}

function startInfoEdit() {
  editTitle.value = material.value?.title || ''
  editPages.value = material.value?.pages ?? undefined
  editCourseTag.value = material.value?.courseTag || ''
  infoEditing.value = true
}

function cancelInfoEdit() {
  infoEditing.value = false
}

async function saveInfoEdit() {
  const id = route.params.id
  if (!id) return
  savingInfo.value = true
  try {
    const data: { title?: string; courseTag?: string; pages?: number } = {}
    if (editTitle.value !== material.value?.title) data.title = editTitle.value
    if (editCourseTag.value !== material.value?.courseTag) data.courseTag = editCourseTag.value
    if (editPages.value !== material.value?.pages) data.pages = editPages.value
    if (Object.keys(data).length === 0) {
      infoEditing.value = false
      return
    }
    const res = await updateMaterialInfo(id as string, data)
    if (res.code === 200) {
      ElMessage.success('保存成功')
      infoEditing.value = false
      await loadDetail()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.msg || err.message || '保存失败')
  } finally {
    savingInfo.value = false
  }
}

onMounted(() => {
  if (isLoggedIn.value) {
    loadDetail()
  }
})

onBeforeUnmount(() => {
  if (previewBlobUrl.value) {
    URL.revokeObjectURL(previewBlobUrl.value)
  }
})
</script>

<style scoped>
.material-detail-page {
  padding: var(--space-6);
}

/* 顶部操作栏 */
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
  max-width: var(--page-max-width);
  margin-left: auto;
  margin-right: auto;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.detail-breadcrumb {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.breadcrumb-sep {
  color: var(--text-tertiary);
  padding: 0 4px;
}

.breadcrumb-current {
  color: var(--text-primary);
  font-weight: 500;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-actions {
  display: flex;
  gap: var(--space-2);
}

/* 左右分栏 35% / 65% */
.detail-grid {
  display: grid;
  grid-template-columns: 35% 1fr;
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
  box-shadow: var(--shadow-sm);
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
  max-height: 300px;
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
  font-size: 64px;
}

.preview-type {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
}

.preview-pages {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  background: var(--bg-hover);
  padding: 2px var(--space-2);
  border-radius: var(--radius-sm);
}

.file-meta-list {
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
}

.file-meta-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--text-sm);
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--border-color);
}

.file-meta-item:last-child {
  border-bottom: none;
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
  box-shadow: var(--shadow-sm);
}

.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
  flex-wrap: wrap;
  gap: var(--space-2);
}

.section-title-left {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.section-title {
  font-size: var(--text-lg);
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

.source-badge {
  font-weight: 500;
}

.ocr-text-box {
  background: #f1f5f9;
  border-radius: var(--radius-md);
  padding: var(--space-4);
  max-height: 400px;
  overflow-y: auto;
}

.ocr-text-box :deep(.el-textarea__inner) {
  background: #fff;
  font-family: inherit;
  font-size: var(--text-sm);
  line-height: 1.7;
  border-radius: var(--radius-md);
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

/* 关键词云 */
.keywords-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.keyword-tag {
  font-size: var(--text-sm);
  margin: 4px;
}

.keyword-weight {
  opacity: 0.7;
  font-size: var(--text-xs);
  margin-left: 2px;
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

/* 处理中状态 */
.processing-section {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 280px;
}

.processing-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
}

.processing-text {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.processing-tip {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  margin: 0;
}

.preview-section {
  margin-top: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}
.preview-header {
  padding: 8px 12px;
  background: #f5f7fa;
  font-size: 14px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 6px;
}
.preview-content {
  padding: 0;
  background: #fff;
}
.preview-content iframe,
.preview-content img {
  display: block;
  border: none;
}
.unsupported-tip {
  padding: 40px 20px;
  text-align: center;
  color: #909399;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

/* 知识点区域 */
.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-4);
}
.knowledge-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}
.knowledge-tag {
  font-size: var(--text-sm);
  padding: 6px 12px;
}
.empty-knowledge {
  text-align: center;
  padding: var(--space-6);
  color: var(--text-tertiary);
  font-size: var(--text-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
}

/* 响应式 */
@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-sidebar {
    position: static;
  }

  .detail-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
