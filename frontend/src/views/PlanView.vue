<template>
  <div v-loading="loading" class="page-container plan-view">
    <!-- 页面标题区 -->
    <div class="page-header-row">
      <div class="page-header-left">
        <h1 class="page-title-large">我的学习计划</h1>
        <p class="page-subtitle">制定计划、跟踪进度、高效学习</p>
      </div>
      <el-button
        v-if="isLoggedIn"
        type="primary"
        size="large"
        :icon="Plus"
        @click="openCreateDialog"
      >
        新建计划
      </el-button>
    </div>

    <!-- 未登录提示 -->
    <div v-if="!isLoggedIn" class="empty-state">
      <div class="empty-icon"><el-icon :size="28"><Lock /></el-icon></div>
      <p class="empty-text">请先登录</p>
      <p class="empty-tip">登录后即可查看和管理您的学习计划</p>
      <el-button type="primary" @click="goLogin">去登录</el-button>
    </div>

    <!-- 无计划空状态 -->
    <div v-else-if="planList.length === 0" class="empty-state empty-state-flat">
      <el-empty description="暂无学习计划，快去制定一个吧">
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">去制定</el-button>
      </el-empty>
    </div>

    <!-- 计划列表 -->
    <div v-else class="plan-grid">
      <div
        v-for="plan in planList"
        :key="plan.id"
        class="plan-card"
        @click="goDetail(plan.id)"
      >
        <div class="plan-card-topbar" :class="plan.status"></div>
        <div class="plan-card-body">
          <div class="plan-header">
            <h3 class="plan-name">{{ plan.name }}</h3>
            <div class="plan-header-actions">
              <el-tag :type="statusType(plan.status)" size="small" effect="light">
                {{ statusLabel(plan.status) }}
              </el-tag>
              <el-tag
                v-if="plan.interruptionRisk === 1"
                type="danger"
                size="small"
                effect="light"
              >
                学习中断风险
              </el-tag>
              <el-button
                type="danger"
                size="small"
                circle
                plain
                :icon="Delete"
                @click.stop="handleDelete(plan.id)"
              />
            </div>
          </div>
          <div class="plan-meta">
            <span class="meta-item">
              <el-icon><Calendar /></el-icon>
              <span>截止 {{ plan.endDate }}</span>
            </span>
            <span class="meta-item">
              <el-icon><Clock /></el-icon>
              <span>每日 {{ plan.dailyHours || '-' }} 小时</span>
            </span>
            <span class="meta-item">
              <el-icon><Files /></el-icon>
              <span>{{ plan.taskCount || plan.totalPages || 0 }} 任务</span>
            </span>
          </div>
          <div class="plan-progress">
            <div class="progress-label-row">
              <span class="progress-label">总进度</span>
              <span class="progress-value">{{ Math.round(plan.progress || 0) }}%</span>
            </div>
            <el-progress
              :percentage="Math.round(plan.progress || 0)"
              :status="progressStatus(plan.status)"
              :stroke-width="10"
              :color="progressColor(plan.status)"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 新建计划弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="新建学习计划"
      width="600px"
      class="plan-dialog"
      align-center
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" class="plan-form">
        <el-form-item label="计划名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：期末复习计划" size="large" />
        </el-form-item>
        <el-form-item label="计划描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="可选，描述计划的目标和内容"
          />
        </el-form-item>
        <el-form-item label="学习资料" prop="materialIds">
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
          <div v-if="form.materialIds.length" class="material-selected-count">
            已选择 {{ form.materialIds.length }} 份资料
          </div>
        </el-form-item>
        <el-form-item label="日期范围" prop="dateRange">
          <div class="date-range-row">
            <el-date-picker
              v-model="form.startDate"
              type="date"
              placeholder="开始日期"
              value-format="YYYY-MM-DD"
              :disabled-date="disabledDate"
              size="large"
            />
            <span class="date-sep">~</span>
            <el-date-picker
              v-model="form.endDate"
              type="date"
              placeholder="截止日期"
              value-format="YYYY-MM-DD"
              :disabled-date="disabledDate"
              size="large"
            />
          </div>
        </el-form-item>
        <el-form-item label="每日时长" prop="dailyHours">
          <el-input-number
            v-model="form.dailyHours"
            :min="0.5"
            :max="12"
            :step="0.5"
            size="large"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" size="large" :loading="submitting" @click="submitCreate">
            确认创建
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { usePlanStore } from '@/stores/plan'
import { getMaterials, deletePlan } from '@/api/plan'
import type { MaterialVO } from '@/api/plan'
import { Plus, Calendar, Clock, Files, Check, Delete, Lock, Document, Picture, Link } from '@element-plus/icons-vue'

const router = useRouter()
const store = usePlanStore()
const formRef = ref<FormInstance>()

const planList = computed(() => store.planList)
const loading = computed(() => store.loading)
const dialogVisible = ref(false)
const submitting = ref(false)
const materials = ref<MaterialVO[]>([])
const materialSearchQuery = ref('')

const filteredMaterials = computed(() => {
  const q = materialSearchQuery.value.trim().toLowerCase()
  if (!q) return materials.value
  return materials.value.filter((m) => m.title.toLowerCase().includes(q))
})

const isLoggedIn = computed(() => !!localStorage.getItem('token'))

const form = ref({
  name: '',
  description: '',
  startDate: '',
  endDate: '',
  dailyHours: 2,
  materialIds: [] as number[],
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入计划名称', trigger: 'blur' },
  ],
  materialIds: [
    { required: true, message: '请至少选择一项学习资料', trigger: 'change', type: 'array', min: 1 },
  ],
  startDate: [
    { required: true, message: '请选择开始日期', trigger: 'change' },
  ],
  endDate: [
    { required: true, message: '请选择结束日期', trigger: 'change' },
  ],
  dailyHours: [
    { required: true, message: '每日时长不能小于 0.5 小时', trigger: 'change', type: 'number', min: 0.5 },
  ],
}

function disabledDate(time: Date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return time.getTime() < today.getTime()
}

function statusType(status: string) {
  const map: Record<string, any> = {
    active: 'primary',
    paused: 'warning',
    completed: 'success',
    overdue: 'danger',
  }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    active: '进行中',
    paused: '已暂停',
    completed: '已完成',
    overdue: '已逾期',
  }
  return map[status] || status
}

function progressStatus(status: string) {
  if (status === 'completed') return 'success'
  if (status === 'overdue') return 'exception'
  return ''
}

function progressColor(status: string) {
  if (status === 'completed') return 'var(--success-500)'
  if (status === 'overdue') return 'var(--danger-500)'
  return 'var(--primary-500)'
}

function getFileIcon(type?: string) {
  const icons: Record<string, any> = {
    'PDF': Document,
    'IMAGE': Picture,
    'PPT': Document,
  }
  return icons[type?.toUpperCase() || ''] || Link
}

function isMaterialSelected(id: number) {
  return form.value.materialIds.includes(id)
}

function toggleMaterial(id: number) {
  const idx = form.value.materialIds.indexOf(id)
  if (idx > -1) {
    form.value.materialIds.splice(idx, 1)
  } else {
    form.value.materialIds.push(id)
  }
}

function openCreateDialog() {
  form.value = {
    name: '',
    description: '',
    startDate: '',
    endDate: '',
    dailyHours: 2,
    materialIds: [],
  }
  dialogVisible.value = true
  loadMaterials()
}

async function loadMaterials() {
  try {
    const res = await getMaterials()
    if (res.code === 200) {
      materials.value = res.data?.records || []
    }
  } catch (err: any) {
    materials.value = []
    ElMessage.error(err.response?.data?.msg || err.message || '获取资料列表失败')
  }
}

async function submitCreate() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await store.createPlan({
      name: form.value.name.trim(),
      description: form.value.description || undefined,
      startDate: form.value.startDate,
      endDate: form.value.endDate,
      dailyHours: form.value.dailyHours,
      materialIds: form.value.materialIds,
    })
    if (res.code === 200) {
      ElMessage.success('计划创建成功')
      dialogVisible.value = false
    } else {
      ElMessage.error(res.msg || '创建失败')
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.msg || err.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

function goDetail(id: number) {
  router.push(`/plans/${id}`)
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该学习计划吗？删除后不可恢复。', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await deletePlan(id)
    if (res.code === 200) {
      ElMessage.success('计划已删除')
      await store.fetchPlanList()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.msg || err.message || '删除失败')
    }
  }
}

function goLogin() {
  router.push('/login')
}

onMounted(() => {
  if (isLoggedIn.value) {
    store.fetchPlanList().catch((err: any) => {
      ElMessage.error(err.response?.data?.msg || err.message || '获取计划列表失败')
    })
  }
})
</script>

<style scoped>
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

.page-title-large {
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

/* 计划卡片网格 */
.plan-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-5);
}

@media (max-width: 1200px) {
  .plan-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .plan-grid {
    grid-template-columns: 1fr;
  }
}

.plan-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.plan-card:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-4px);
  border-color: transparent;
}

.plan-card-topbar {
  height: 4px;
}

.plan-card-topbar.active {
  background: var(--primary-500);
}

.plan-card-topbar.completed {
  background: var(--success-500);
}

.plan-card-topbar.overdue {
  background: var(--danger-500);
}

.plan-card-topbar.paused {
  background: var(--warning-500);
}

.plan-card-body {
  padding: var(--space-5);
}

.plan-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-4);
  gap: var(--space-2);
}

.plan-header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.plan-name {
  font-size: var(--text-xl);
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
  line-height: 1.4;
  word-break: break-all;
}

.plan-meta {
  display: flex;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  color: var(--text-secondary);
  font-size: var(--text-sm);
}

.meta-item .el-icon {
  color: var(--text-tertiary);
}

.plan-progress {
  margin-top: var(--space-2);
}

.progress-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-1);
}

.progress-label {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.progress-value {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
}

/* 弹窗表单 */
.plan-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text-secondary);
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

/* 日期并排 */
.date-range-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.date-sep {
  color: var(--text-tertiary);
  font-weight: 500;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

@media (max-width: 600px) {
  .date-range-row {
    flex-direction: column;
    align-items: stretch;
  }

  .date-sep {
    text-align: center;
  }

  .material-selector {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
