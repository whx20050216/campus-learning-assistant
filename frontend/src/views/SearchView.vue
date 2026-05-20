<template>
  <div class="page-container-narrow search-page">
    <!-- 未登录提示 -->
    <div v-if="!isLoggedIn" class="empty-state" style="margin-top: var(--space-8)">
      <div class="empty-icon">🔒</div>
      <p class="empty-text">请先登录</p>
      <p class="empty-tip">登录后即可搜索您的学习资料</p>
      <el-button type="primary" @click="goLogin">去登录</el-button>
    </div>

    <template v-else>
      <!-- 头部 -->
      <header class="page-header-center">
        <h1 class="page-title-large">🔍 智能资料搜索</h1>
        <p class="page-subtitle">基于 OCR 和 NLP 的知识库检索</p>
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
            class="btn-primary search-btn"
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
        <div v-if="results.length === 0" class="empty-state empty-state-flat">
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
            <header class="result-header">
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
            <footer class="result-footer">
              <span class="status-badge" :class="item.status">
                {{ getStatusText(item.status) }}
              </span>
              <div class="result-actions">
                <button class="btn-ghost view-btn" @click="viewDetail(item.id)">
                  查看详情 →
                </button>
                <button class="btn-ghost delete-btn" @click="handleDelete(item.id)">
                  🗑️ 删除
                </button>
              </div>
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
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { search as searchApi } from '@/api/search'
import { deleteMaterial } from '@/api/material'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const isLoggedIn = computed(() => !!localStorage.getItem('token'))

function goLogin() {
  router.push('/login')
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
    const data = await searchApi({
      keyword: keyword.value,
      mode: mode.value,
      page: 0,
      size: 10
    })

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
    const data = await searchApi({
      keyword: keyword.value,
      mode: mode.value,
      page,
      size: 10
    })

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
      ElMessage.error(err.response?.data?.msg || '删除失败')
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
  gap: var(--space-3);
  max-width: 700px;
  margin: 0 auto;
}

.search-input {
  flex: 1;
  padding: var(--space-3) var(--space-4);
  border: 1.5px solid var(--border-color);
  border-radius: var(--radius-md);
  font-size: var(--text-md);
  transition: all 0.2s ease;
  min-width: 0;
  background: var(--bg-hover);
}

.search-input:focus {
  outline: none;
  border-color: var(--primary-500);
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
  background: var(--bg-card);
}

.search-input::placeholder {
  color: var(--text-tertiary);
}

.search-select {
  padding: var(--space-3);
  border: 1.5px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  cursor: pointer;
  font-size: var(--text-sm);
  color: var(--text-secondary);
  transition: border-color 0.2s ease;
}

.search-select:focus {
  outline: none;
  border-color: var(--primary-500);
}

.search-btn {
  white-space: nowrap;
}

/* 结果区域 */
.results-section {
  background: transparent;
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
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  border-color: var(--primary-200);
}

/* 卡片头部 */
.result-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
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
  font-size: var(--text-base);
  color: var(--text-primary);
  margin: 0 0 var(--space-1);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upload-time {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.confidence-badge {
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: 600;
  white-space: nowrap;
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

/* 关键词 */
.keywords-row {
  margin-bottom: var(--space-3);
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.keyword-tag {
  background: var(--primary-50);
  color: var(--primary-500);
  padding: var(--space-1) var(--space-3);
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

/* 摘要 */
.summary-text {
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.6;
  margin: 0 0 var(--space-4);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 卡片底部 */
.result-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: var(--space-3);
  border-top: 1px solid var(--border-color);
}

.status-badge {
  font-size: var(--text-xs);
  padding: var(--space-1) var(--space-3);
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
}

.view-btn {
  font-size: var(--text-sm);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
}

.delete-btn {
  font-size: var(--text-sm);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  color: var(--danger-500);
}

.delete-btn:hover {
  background: var(--danger-50);
}

/* 响应式适配 */
@media (max-width: 768px) {
  .search-box {
    flex-direction: column;
    padding: var(--space-4);
  }

  .search-select {
    width: 100%;
  }

  .search-btn {
    width: 100%;
  }

  .results-meta {
    flex-direction: column;
    gap: var(--space-2);
    align-items: flex-start;
  }
}
</style>
