<template>
  <div class="page-container exam-list-page">
    <div class="page-header-row">
      <div class="page-header-left">
        <h1 class="page-title">我的试卷</h1>
        <p class="page-subtitle">查看和管理智能生成的试卷</p>
      </div>
      <el-button type="primary" @click="openGenerateDialog">
        <el-icon><Plus /></el-icon> 新建试卷
      </el-button>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" size="32"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <div v-else-if="papers.length === 0" class="empty-state" style="margin-top: var(--space-8)">
      <el-empty description="暂无试卷，快去新建一份吧">
        <el-button type="primary" @click="openGenerateDialog">新建试卷</el-button>
      </el-empty>
    </div>

    <div v-else class="exam-table-wrapper">
      <el-table :data="papers" style="width: 100%">
        <el-table-column prop="title" label="试卷标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="courseTag" label="课程" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ row.courseTag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="questionCount" label="题数" width="80" />
        <el-table-column prop="difficulty" label="难度" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="difficultyType(row.difficulty)" effect="light">
              {{ difficultyLabel(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="goAnswer(row.id)">
              开始答题
            </el-button>
            <el-button size="small" @click="viewResult(row.id)">
              查看结果
            </el-button>
            <el-button type="danger" size="small" plain @click="handleDelete(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新建试卷弹窗 -->
    <el-dialog v-model="generateVisible" title="新建智能试卷" width="560px" align-center>
      <el-form label-width="90px">
        <el-form-item label="选择资料">
          <el-input
            v-model="materialSearchQuery"
            placeholder="搜索资料名称"
            size="small"
            clearable
            style="margin-bottom: var(--space-2)"
          />
          <div class="material-selector">
            <div
              v-for="m in filteredMaterials"
              :key="m.id"
              class="material-option"
              :class="{ selected: isMaterialSelected(m.id) }"
              @click="toggleMaterial(m.id)"
            >
              <el-icon class="material-option-icon" :size="28"><component :is="getFileIcon(m.fileType)" /></el-icon>
              <div class="material-option-name">{{ m.title }}</div>
              <div v-if="isMaterialSelected(m.id)" class="material-option-check">
                <el-icon><Check /></el-icon>
              </div>
            </div>
          </div>
          <div v-if="generateForm.materialIds.length" class="material-selected-count">
            已选择 {{ generateForm.materialIds.length }} 份资料
          </div>
        </el-form-item>

        <el-form-item label="课程标签">
          <el-input v-model="generateForm.courseTag" placeholder="请输入课程标签，用于分类统计" />
        </el-form-item>

        <el-form-item label="题目数量">
          <el-slider v-model="generateForm.questionCount" :min="5" :max="30" show-stops />
        </el-form-item>

        <el-form-item label="难度">
          <el-radio-group v-model="generateForm.difficulty">
            <el-radio-button label="easy">简单</el-radio-button>
            <el-radio-button label="medium">中等</el-radio-button>
            <el-radio-button label="hard">困难</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="题型">
          <el-checkbox-group v-model="generateForm.types">
            <el-checkbox label="single">单选题</el-checkbox>
            <el-checkbox label="multiple">多选题</el-checkbox>
            <el-checkbox label="judge">判断题</el-checkbox>
            <el-checkbox label="essay">简答题</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="generateVisible = false">取消</el-button>
          <el-button type="primary" size="large" :loading="generateLoading" @click="submitGenerate">
            开始组卷
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 结果弹窗 -->
    <el-dialog v-model="resultVisible" title="答题结果" width="500px" align-center>
      <div v-if="record" class="result-body">
        <div class="result-score">{{ record.score }}<span class="result-unit">分</span></div>
        <div class="result-detail">
          <span>正确 {{ record.correctCount }} / {{ record.totalCount }} 题</span>
          <span>用时 {{ formatDuration(record.spentTimeSeconds) }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="resultVisible = false">关闭</el-button>
        <el-button type="primary" @click="resultVisible = false; router.push(`/exams/${currentPaperId}/answer`)">
          重新答题
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, Plus, Check, Document, Picture, Link } from '@element-plus/icons-vue'
import { getExamList, deleteExam, getExamRecord, generateExam, type ExamPaperVO, type ExamRecordVO, type GenerateExamDTO } from '@/api/exam'
import { getMaterials, type MaterialVO } from '@/api/plan'

const router = useRouter()
const loading = ref(false)
const papers = ref<ExamPaperVO[]>([])
const resultVisible = ref(false)
const record = ref<ExamRecordVO | null>(null)
const currentPaperId = ref<number>(0)

// 组卷弹窗
const generateVisible = ref(false)
const generateLoading = ref(false)
const generateForm = ref<GenerateExamDTO>({
  materialIds: [],
  courseTag: '',
  questionCount: 10,
  difficulty: 'medium',
  types: ['single', 'multiple', 'judge', 'essay'],
})

// 资料选择
const materialList = ref<MaterialVO[]>([])
const materialSearchQuery = ref('')

const filteredMaterials = computed(() => {
  if (!materialSearchQuery.value) return materialList.value
  return materialList.value.filter((m) => m.title.includes(materialSearchQuery.value))
})

function isMaterialSelected(id: number) {
  return generateForm.value.materialIds.includes(id)
}

function toggleMaterial(id: number) {
  const idx = generateForm.value.materialIds.indexOf(id)
  if (idx > -1) {
    generateForm.value.materialIds.splice(idx, 1)
  } else {
    generateForm.value.materialIds.push(id)
  }
}

function getFileIcon(type?: string) {
  const icons: Record<string, any> = {
    PDF: Document,
    IMAGE: Picture,
    PPT: Document,
  }
  return icons[type?.toUpperCase() || ''] || Link
}

async function openGenerateDialog() {
  generateForm.value = {
    materialIds: [],
    courseTag: '',
    questionCount: 10,
    difficulty: 'medium',
    types: ['single', 'multiple', 'judge', 'essay'],
  }
  materialSearchQuery.value = ''
  generateVisible.value = true
  try {
    const res = await getMaterials()
    materialList.value = res.data?.records || []
  } catch {
    ElMessage.error('加载资料列表失败')
  }
}

async function submitGenerate() {
  if (generateForm.value.materialIds.length === 0) {
    ElMessage.warning('请至少选择一份资料')
    return
  }
  if (generateForm.value.materialIds.length > 5) {
    ElMessage.warning('最多选择5份资料')
    return
  }
  if (!generateForm.value.courseTag.trim()) {
    ElMessage.warning('请输入课程标签')
    return
  }
  if (generateForm.value.types.length === 0) {
    ElMessage.warning('请至少选择一种题型')
    return
  }
  generateLoading.value = true
  try {
    const res = await generateExam({
      ...generateForm.value,
      courseTag: generateForm.value.courseTag.trim(),
    })
    if (res.code === 200 && res.data) {
      ElMessage.success('组卷成功')
      generateVisible.value = false
      router.push(`/exams/${res.data.id}/answer`)
    } else {
      ElMessage.error(res.msg || '组卷失败')
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.msg || err.message || '组卷失败')
  } finally {
    generateLoading.value = false
  }
}

onMounted(() => loadPapers())

async function loadPapers() {
  loading.value = true
  try {
    const res = await getExamList()
    papers.value = res.data?.records || []
  } catch {
    ElMessage.error('加载试卷列表失败')
  } finally {
    loading.value = false
  }
}

function goAnswer(id: number) {
  router.push(`/exams/${id}/answer`)
}

async function viewResult(id: number) {
  currentPaperId.value = id
  try {
    const res = await getExamRecord(id)
    record.value = res.data
    resultVisible.value = true
  } catch {
    ElMessage.info('暂无答题记录')
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('删除后不可恢复，确认删除？', '提示', { type: 'warning' })
    await deleteExam(id)
    ElMessage.success('删除成功')
    loadPapers()
  } catch {
    // 取消
  }
}

function difficultyType(d: string) {
  const map: Record<string, string> = { easy: 'success', medium: 'warning', hard: 'danger' }
  return map[d] || 'info'
}
function difficultyLabel(d: string) {
  const map: Record<string, string> = { easy: '简单', medium: '中等', hard: '困难' }
  return map[d] || d
}
function statusType(s: string) {
  const map: Record<string, string> = { draft: 'info', published: 'success', completed: 'success' }
  return map[s] || 'info'
}
function statusLabel(s: string) {
  const map: Record<string, string> = { draft: '草稿', published: '已发布', completed: '已完成' }
  return map[s] || s
}
function formatTime(t?: string) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN', { hour12: false })
}
function formatDuration(sec: number) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}
</script>

<style scoped>
.exam-table-wrapper {
  margin-top: var(--space-4);
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  border: 1px solid var(--border-color);
}
.result-body {
  text-align: center;
  padding: var(--space-6);
}
.result-score {
  font-size: 48px;
  font-weight: 700;
  color: var(--primary-500);
  line-height: 1;
}
.result-unit {
  font-size: 20px;
  font-weight: 400;
  color: var(--text-secondary);
  margin-left: 4px;
}
.result-detail {
  margin-top: var(--space-4);
  display: flex;
  justify-content: center;
  gap: var(--space-6);
  color: var(--text-secondary);
  font-size: 14px;
}

/* 资料选择器卡片 */
.material-selector {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: var(--space-2);
  max-height: 200px;
  overflow-y: auto;
  padding: var(--space-2);
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
}
.material-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-3) var(--space-2);
  background: var(--bg-card);
  border: 1.5px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  text-align: center;
}
.material-option:hover {
  border-color: var(--primary-300);
}
.material-option.selected {
  border-color: var(--primary-500);
  background: var(--primary-50);
}
.material-option-icon {
  color: var(--primary-500);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.material-option-name {
  font-size: var(--text-xs);
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}
.material-option-check {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 18px;
  height: 18px;
  background: var(--primary-500);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
}
.material-selected-count {
  margin-top: var(--space-2);
  font-size: var(--text-xs);
  color: var(--text-secondary);
}
</style>
