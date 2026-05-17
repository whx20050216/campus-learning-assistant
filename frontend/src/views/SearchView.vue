<template>
  <div class="search-page">
    <!-- 头部 -->
    <header class="page-header">
      <h1 class="title">🔍 智能资料搜索</h1>
      <p class="subtitle">基于 OCR 和 NLP 的知识库检索</p>
    </header>

    <!-- 搜索区域 -->
    <section class="search-section">
      <div class="search-box">
        <input 
          v-model="keyword" 
          class="search-input"
          placeholder="输入关键词搜索知识点（如：链表、排序、算法）..." 
          @keyup.enter="search"
        />
        <select v-model="mode" class="search-select">
          <option value="all">🔥 综合</option>
          <option value="keyword">🏷️ 标签</option>
          <option value="text">📝 全文</option>
        </select>
        <button 
          @click="search" 
          class="search-btn" 
          :disabled="loading"
        >
          {{ loading ? '搜索中...' : '搜索' }}
        </button>
      </div>
    </section>

    <!-- 结果区域 -->
    <main v-if="searched" class="results-section">
      <!-- 结果头部 -->
      <div class="results-meta">
        <span class="results-count">
          找到 <strong>{{ total }}</strong> 条相关结果
        </span>
        <span class="mode-tag">{{ modeText }}</span>
      </div>

      <!-- 空状态 -->
      <div v-if="results.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <p class="empty-text">未找到相关资料</p>
        <span class="empty-tip">试试其他关键词，或上传新资料</span>
      </div>

      <!-- 结果列表 -->
      <div v-else class="results-list">
        <article 
          v-for="item in results" 
          :key="item.id" 
          class="result-card"
        >
          <!-- 卡片头部 -->
          <header class="card-header">
            <span class="file-icon">{{ getFileIcon(item.fileType) }}</span>
            <div class="file-info">
              <h3 class="filename">{{ item.filename }}</h3>
              <time class="upload-time">{{ formatTime(item.createdAt) }}</time>
            </div>
            <span 
              class="confidence-badge"
              :class="getConfidenceClass(item.ocrConfidence)"
            >
              {{ (item.ocrConfidence * 100).toFixed(0) }}%
            </span>
          </header>

          <!-- 关键词 -->
          <div class="keywords-row">
            <span 
              v-for="kw in item.keywords?.split(',')" 
              :key="kw" 
              class="keyword-tag"
            >
              {{ kw.trim() }}
            </span>
            <span v-if="!item.keywords" class="no-keywords">暂无关键词</span>
          </div>

          <!-- 摘要 -->
          <p class="summary-text">{{ item.summary }}</p>

          <!-- 底部操作 -->
          <footer class="card-footer">
            <span class="status-badge" :class="item.status">
              {{ getStatusText(item.status) }}
            </span>
            <button class="view-btn" @click="viewDetail(item.id)">
              查看详情
            </button>
          </footer>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

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
    'keyword': '标签搜索',
    'text': '全文搜索'
  }
  return map[mode.value] || '综合搜索'
})

// 搜索
const search = async () => {
  if (!keyword.value.trim()) return
  
  loading.value = true
  searched.value = true
  currentPage.value = 0
  
  try {
    const response = await fetch(
      `/api/search?keyword=${encodeURIComponent(keyword.value)}&mode=${mode.value}&page=0&size=10`
    )
    const data = await response.json()
    
    if (data.success) {
      results.value = data.items
      total.value = data.total
      totalPages.value = data.totalPages
    }
  } catch (error) {
    console.error('搜索失败:', error)
    alert('搜索失败，请检查后端服务')
  } finally {
    loading.value = false
  }
}

// 翻页
const goToPage = async (page: number) => {
  currentPage.value = page
  loading.value = true
  
  try {
    const response = await fetch(
      `/api/search?keyword=${encodeURIComponent(keyword.value)}&mode=${mode.value}&page=${page}&size=10`
    )
    const data = await response.json()
    
    if (data.success) {
      results.value = data.items
    }
  } catch (error) {
    console.error('翻页失败:', error)
  } finally {
    loading.value = false
  }
}

// 工具函数
const getFileIcon = (type: string) => {
  const icons: Record<string, string> = {
    'IMAGE': '🖼️',
    'PDF': '📄',
    'PPT': '📊'
  }
  return icons[type] || '📎'
}

const getConfidenceClass = (confidence: number) => {
  if (confidence >= 0.9) return 'high'
  if (confidence >= 0.7) return 'medium'
  return 'low'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'completed': '✅ 已完成',
    'processing': '⏳ 处理中',
    'uploaded': '📤 已上传'
  }
  return map[status] || status
}

const formatTime = (time: string) => {
  return new Date(time).toLocaleString('zh-CN')
}

const viewDetail = (id: number) => {
  alert(`查看详情功能开发中... ID: ${id}`)
}
</script>

<style scoped>
/* 页面容器 */
.search-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 20px;
  min-height: 100vh;
  background: #f5f7fa;
}

/* 头部 */
.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.title {
  font-size: 32px;
  color: #1a1a1a;
  margin: 0 0 8px 0;
  font-weight: 600;
}

.subtitle {
  color: #666;
  font-size: 16px;
  margin: 0;
}

/* 搜索区域 */
.search-section {
  margin-bottom: 32px;
}

.search-box {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  gap: 12px;
  max-width: 700px;
  margin: 0 auto;
}

.search-input {
  flex: 1;
  padding: 12px 16px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.3s;
  min-width: 0;
}

.search-input:focus {
  outline: none;
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

.search-select {
  padding: 12px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
}

.search-btn {
  padding: 12px 28px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s;
  white-space: nowrap;
}

.search-btn:hover:not(:disabled) {
  background: #66b1ff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.search-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

/* 结果区域 */
.results-section {
  background: transparent;
}

.results-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 4px;
}

.results-count {
  color: #606266;
  font-size: 14px;
}

.results-count strong {
  color: #409eff;
  font-size: 18px;
  margin: 0 4px;
}

.mode-tag {
  background: #ecf5ff;
  color: #409eff;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  border: 1px solid #d9ecff;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-text {
  color: #606266;
  font-size: 16px;
  margin: 0 0 8px 0;
}

.empty-tip {
  color: #909399;
  font-size: 14px;
}

/* 结果卡片 */
.results-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-card {
  background: white;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s;
}

.result-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
  border-color: #d9ecff;
}

/* 卡片头部 */
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.file-icon {
  font-size: 36px;
  line-height: 1;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.filename {
  font-size: 16px;
  color: #303133;
  margin: 0 0 6px 0;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upload-time {
  font-size: 13px;
  color: #909399;
}

.confidence-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
}

.confidence-badge.high {
  background: #f0f9eb;
  color: #67c23a;
}

.confidence-badge.medium {
  background: #fdf6ec;
  color: #e6a23c;
}

.confidence-badge.low {
  background: #fef0f0;
  color: #f56c6c;
}

/* 关键词 */
.keywords-row {
  margin-bottom: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.keyword-tag {
  background: #ecf5ff;
  color: #409eff;
  padding: 4px 10px;
  border-radius: 16px;
  font-size: 12px;
  border: 1px solid #d9ecff;
}

.no-keywords {
  color: #c0c4cc;
  font-size: 13px;
  font-style: italic;
}

/* 摘要 */
.summary-text {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 16px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 卡片底部 */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.status-badge {
  font-size: 13px;
  padding: 4px 10px;
  border-radius: 4px;
}

.status-badge.completed {
  background: #f0f9eb;
  color: #67c23a;
}

.view-btn {
  padding: 6px 16px;
  background: white;
  border: 1px solid #dcdfe6;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s;
}

.view-btn:hover {
  color: #409eff;
  border-color: #c6e2ff;
  background: #ecf5ff;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 32px;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid #dcdfe6;
  background: white;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  min-width: 40px;
}

.page-btn:hover:not(.active) {
  border-color: #409eff;
  color: #409eff;
}

.page-btn.active {
  background: #409eff;
  color: white;
  border-color: #409eff;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .search-page {
    padding: 20px 16px;
  }

  .title {
    font-size: 24px;
  }

  .search-box {
    flex-direction: column;
    padding: 16px;
  }

  .search-select {
    width: 100%;
  }

  .search-btn {
    width: 100%;
  }

  .results-meta {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }
}
</style>