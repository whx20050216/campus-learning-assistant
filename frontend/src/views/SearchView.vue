<template>
  <div class="page-container-narrow search-page">
    <!-- 未登录提示 -->
    <div v-if="!isLoggedIn" class="empty-state" style="margin-top: var(--space-8)">
      <div class="empty-icon"><el-icon :size="28"><Lock /></el-icon></div>
      <p class="empty-text">请先登录</p>
      <p class="empty-tip">登录后即可搜索您的学习资料</p>
      <el-button type="primary" @click="goLogin">去登录</el-button>
    </div>

    <template v-else>
      <!-- 头部 -->
      <header class="page-header-center">
        <h1 class="page-title-large">智能资料搜索</h1>
        <p class="page-subtitle">基于 OCR 和 NLP 的知识库检索</p>
      </header>

      <!-- 搜索区域 -->
      <section class="search-section">
        <div class="search-box">
          <div class="search-input-wrap">
            <el-icon class="search-input-icon"><Search /></el-icon>
            <input
              v-model="keyword"
              class="search-input"
              placeholder="输入关键词搜索知识点，留空查看全部资料"
              @keyup.enter="search"
            />
          </div>
          <el-radio-group v-model="mode" class="search-mode-group" size="large">
            <el-radio-button label="all">综合</el-radio-button>
            <el-radio-button label="keyword">关键词</el-radio-button>
            <el-radio-button label="knowledge">知识点</el-radio-button>
            <el-radio-button label="course">课程</el-radio-button>
          </el-radio-group>
          <el-button
            type="primary"
            size="large"
            class="search-btn"
            :loading="loading"
            @click="search"
          >
            <el-icon><Search /></el-icon>
            <span>{{ loading ? '搜索中...' : '搜索' }}</span>
          </el-button>
        </div>
      </section>

      <!-- 结果区域 -->
      <main v-if="searched" v-loading="loading" class="results-section">
        <!-- 结果头部 -->
        <div class="results-meta">
          <span class="results-count">
            找到 <strong>{{ total }}</strong> 条相关结果
          </span>
          <span class="mode-tag">{{ modeText }}</span>
        </div>

        <!-- 空状态 -->
        <div v-if="results.length === 0" class="empty-results">
          <el-empty description="暂无搜索结果，试试其他关键词">
            <el-button type="primary" @click="goUpload">去上传资料</el-button>
          </el-empty>
        </div>

        <!-- 结果列表 -->
        <div v-else class="results-list">
          <article
            v-for="item in results"
            :key="item.id"
            class="result-card"
          >
            <div class="result-card-body">
              <!-- 左侧文件图标 -->
              <div class="result-file-icon">
                <el-icon :size="28"><component :is="getFileIcon(item.fileType)" /></el-icon>
              </div>

              <!-- 右侧内容 -->
              <div class="result-content">
                <!-- 标题行 -->
                <div class="result-title-row">
                  <h3 class="result-filename" @click="viewDetail(item.id)">
                    <span v-html="item.title || item.filename"></span>
                  </h3>
                  <span
                    v-if="item.ocrConfidence !== undefined && item.ocrConfidence !== null"
                    class="confidence-badge"
                    :class="getConfidenceClass(item.ocrConfidence)"
                  >
                    {{ (item.ocrConfidence * 100).toFixed(0) }}%
                  </span>
                </div>

                <!-- 摘要 -->
                <p class="result-summary" v-html="item.ocrTextSnippet || item.summary || '暂无摘要'"></p>

                <!-- 标签行 -->
                <div class="result-tags-row">
                  <el-tag v-if="item.courseTag" size="small" type="info" effect="plain">
                    {{ item.courseTag }}
                  </el-tag>
                  <span
                    v-for="kw in (item.keywords || '').split(',')"
                    :key="kw"
                    class="result-keyword-tag"
                  >
                    {{ kw.trim() }}
                  </span>
                  <span v-if="!item.keywords" class="no-keywords">暂无关键词</span>
                </div>

                <!-- 底部元信息 -->
                <div class="result-meta-row">
                  <span class="result-meta-item">
                    <el-icon><Timer /></el-icon>
                    {{ formatTime(item.createdAt) }}
                  </span>
                  <span class="result-meta-item">
                    <el-icon><Document /></el-icon>
                    {{ item.fileType || '-' }}
                  </span>
                  <span class="status-badge" :class="item.status">
                    {{ getStatusText(item.status) }}
                  </span>
                  <div class="result-actions">
                    <el-button type="primary" size="small" @click="viewDetail(item.id)">
                      查看详情
                    </el-button>
                    <el-button type="danger" size="small" plain @click="handleDelete(item.id)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </article>
        </div>

        <!-- 分页 -->
        <nav v-if="totalPages > 1" class="pagination">
          <button
            v-for="page in totalPages"
            :key="page"
            @click="goToPage(page - 1)"
            :class="['page-btn', { active: currentPage === page - 1 }]"
          >
            {{ page }}
          </button>
        </nav>
      </main>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { search as searchApi } from '@/api/search'
import { deleteMaterial, getMaterialList } from '@/api/material'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Timer, Document, Delete, Link, Picture, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const isLoggedIn = computed(() => !!localStorage.getItem('token'))

function goLogin() {
  router.push('/login')
}

function goUpload() {
  router.push('/upload')
}

// 状态
const keyword = ref('')
const mode = ref('all')
const results = ref<any[]>([])
const total = ref(0)
const totalPages = ref(0)
const currentPage = ref(0)
const loading = ref(false)
const searched = ref(false)

// 计算属性
const modeText = computed(() => {
  const map: Record<string, string> = {
    'all': '综合搜索',
    'keyword': '关键词搜索',
    'course': '课程搜索',
    'knowledge': '知识点搜索'
  }
  return map[mode.value] || '综合搜索'
})

// 搜索
const search = async () => {
  const kw = keyword.value.trim()
  loading.value = true
  searched.value = true
  currentPage.value = 0

  try {
    if (!kw) {
      // 空关键词：展示全部资料
      const res = await getMaterialList()
      if (res.code === 200) {
        const records = res.data?.records || []
        results.value = records
        total.value = records.length
        totalPages.value = 1
      }
    } else {
      const data = await searchApi({
        query: kw,
        mode: mode.value,
        page: 0,
        size: 10
      })

      if (data.success) {
        results.value = data.items
        total.value = data.total
        totalPages.value = data.totalPages
      }
    }
  } catch (error: any) {
    console.error('搜索失败:', error)
    ElMessage.error(error.response?.data?.msg || error.message || '搜索失败，请检查后端服务')
    results.value = []
    total.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

// 翻页
const goToPage = async (page: number) => {
  const kw = keyword.value.trim()
  if (!kw) {
    // 空关键词时一次性加载全部，无需翻页请求
    currentPage.value = page
    return
  }

  currentPage.value = page
  loading.value = true

  try {
    const data = await searchApi({
      query: kw,
      mode: mode.value,
      page,
      size: 10
    })

    if (data.success) {
      results.value = data.items
    }
  } catch (error: any) {
    console.error('翻页失败:', error)
    ElMessage.error(error.response?.data?.msg || error.message || '翻页失败')
  } finally {
    loading.value = false
  }
}

// 工具函数
const getFileIcon = (type: string) => {
  const icons: Record<string, any> = {
    'IMAGE': Picture,
    'PDF': Document,
    'PPT': Document
  }
  return icons[type] || Link
}

const getConfidenceClass = (confidence: number) => {
  if (confidence >= 0.9) return 'high'
  if (confidence >= 0.7) return 'medium'
  return 'low'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'completed': '已完成',
    'processing': '处理中',
    'uploaded': '已上传'
  }
  return map[status] || status
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const viewDetail = (id: number) => {
  router.push(`/materials/${id}`)
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这份资料吗？', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await deleteMaterial(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      // 刷新当前列表
      await search()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.msg || err.message || '删除失败')
    }
  }
}
</script>

<style scoped>
/* 搜索区域 */
.search-section {
  margin-bottom: var(--space-6);
}

.search-box {
  background: var(--bg-card);
  padding: var(--space-5);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  max-width: 700px;
  margin: 0 auto;
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-input-icon {
  position: absolute;
  left: var(--space-3);
  color: var(--text-tertiary);
  font-size: 18px;
}

.search-input {
  flex: 1;
  width: 100%;
  padding: 12px var(--space-4) 12px 40px;
  border: 1.5px solid var(--border-color);
  border-radius: var(--radius-md);
  font-size: 16px;
  transition: all 0.2s ease;
  min-width: 0;
  background: var(--bg-hover);
  height: 48px;
}

.search-input:focus {
  outline: none;
  border-color: var(--primary-500);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.2);
  background: var(--bg-card);
}

.search-input::placeholder {
  color: var(--text-tertiary);
}

/* 模式切换胶囊按钮 */
.search-mode-group {
  display: flex;
  justify-content: center;
}

.search-mode-group :deep(.el-radio-button__inner) {
  font-size: var(--text-sm);
}

.search-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
}

/* 结果区域 */
.results-section {
  background: transparent;
  min-height: 200px;
}

.results-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-5);
  padding: 0 var(--space-1);
}

.results-count {
  color: var(--text-secondary);
  font-size: var(--text-sm);
}

.results-count strong {
  color: var(--primary-500);
  font-size: var(--text-lg);
  margin: 0 var(--space-1);
}

.mode-tag {
  background: var(--primary-50);
  color: var(--primary-500);
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: 500;
  border: 1px solid var(--primary-100);
}

/* 空状态 */
.empty-results {
  padding: var(--space-12) 0;
}

/* 结果列表 */
.results-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.result-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  transition: all 0.2s ease;
}

.result-card:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
  border-color: var(--primary-200);
}

.result-card-body {
  display: flex;
  gap: var(--space-4);
}

/* 左侧文件图标 */
.result-file-icon {
  font-size: 40px;
  line-height: 1;
  flex-shrink: 0;
  width: 48px;
  text-align: center;
}

/* 右侧内容 */
.result-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

/* 标题行 */
.result-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.result-filename {
  font-size: var(--text-base);
  color: var(--primary-500);
  margin: 0;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.result-filename:hover {
  text-decoration: underline;
}

/* 摘要 */
.result-summary {
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 标签行 */
.result-tags-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
}

.result-keyword-tag {
  background: var(--primary-50);
  color: var(--primary-500);
  padding: 2px var(--space-3);
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: 500;
  border: 1px solid var(--primary-100);
}

.no-keywords {
  color: var(--text-tertiary);
  font-size: var(--text-xs);
  font-style: italic;
}

/* 底部元信息 */
.result-meta-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding-top: var(--space-2);
  margin-top: var(--space-1);
  border-top: 1px solid var(--border-color);
  flex-wrap: wrap;
}

.result-meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.status-badge {
  font-size: var(--text-xs);
  padding: 2px var(--space-2);
  border-radius: var(--radius-sm);
  font-weight: 500;
}

.status-badge.completed {
  background: var(--success-50);
  color: var(--success-500);
}

.status-badge.processing {
  background: var(--warning-50);
  color: var(--warning-500);
}

.status-badge.uploaded {
  background: var(--gray-100);
  color: var(--text-tertiary);
}

.result-actions {
  display: flex;
  gap: var(--space-2);
  margin-left: auto;
}

/* 置信度徽章 */
.confidence-badge {
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: 600;
  white-space: nowrap;
  flex-shrink: 0;
}

.confidence-badge.high {
  background: var(--success-50);
  color: var(--success-500);
}

.confidence-badge.medium {
  background: var(--warning-50);
  color: var(--warning-500);
}

.confidence-badge.low {
  background: var(--danger-50);
  color: var(--danger-500);
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  gap: var(--space-2);
  margin-top: var(--space-6);
}

.page-btn {
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: var(--text-sm);
  color: var(--text-secondary);
  transition: all 0.2s ease;
}

.page-btn:hover {
  border-color: var(--primary-500);
  color: var(--primary-500);
}

.page-btn.active {
  background: var(--primary-500);
  color: #fff;
  border-color: var(--primary-500);
}

/* 响应式适配 */
@media (max-width: 768px) {
  .search-box {
    padding: var(--space-4);
  }

  .search-btn {
    width: 100%;
  }

  .result-card-body {
    flex-direction: column;
    gap: var(--space-3);
  }

  .result-file-icon {
    align-self: flex-start;
  }

  .result-meta-row {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
  }

  .result-actions {
    margin-left: 0;
    width: 100%;
  }

  .results-meta {
    flex-direction: column;
    gap: var(--space-2);
    align-items: flex-start;
  }
}
</style>
