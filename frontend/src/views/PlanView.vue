<template>
  <div class="page-container plan-view">
    <div class="page-header">
      <h1 class="page-title">📋 学习计划</h1>
      <el-button v-if="isLoggedIn" type="primary" @click="openCreateDialog">+ 新建计划</el-button>
    </div>

    <!-- 未登录提示 -->
    <div v-if="!isLoggedIn" class="empty-state">
      <div class="empty-icon">🔒</div>
      <p class="empty-text">请先登录</p>
      <p class="empty-tip">登录后即可查看和管理您的学习计划</p>
      <el-button type="primary" @click="goLogin">去登录</el-button>
    </div>

    <div v-else-if="loading" class="loading-state">加载中...</div>

    <div v-else-if="planList.length === 0" class="empty-state empty-state-flat">
      <div class="empty-icon">📚</div>
      <p class="empty-text">暂无学习计划</p>
      <p class="empty-tip">创建您的第一个学习计划，开始高效学习之旅</p>
      <el-button type="primary" @click="openCreateDialog">创建第一个计划</el-button>
    </div>

    <div v-else class="plan-grid">
      <div
        v-for="plan in planList"
        :key="plan.id"
        class="plan-card card-hover"
        @click="goDetail(plan.id)"
      >
        <div class="plan-header">
          <h3 class="plan-name">{{ plan.name }}</h3>
          <el-tag :type="statusType(plan.status)" size="small">{{ statusLabel(plan.status) }}</el-tag>
        </div>
        <div class="plan-meta">
          <span class="meta-item">
            <span class="meta-icon">📅</span>
            <span>截止 {{ plan.endDate }}</span>
          </span>
          <span class="meta-item">
            <span class="meta-icon">📖</span>
            <span>{{ plan.totalPages }} 页</span>
          </span>
        </div>
        <div class="plan-progress">
          <el-progress :percentage="Math.round(plan.progress || 0)" :status="progressStatus(plan.status)" :stroke-width="8" />
        </div>
      </div>
    </div>

    <!-- 新建计划弹窗 -->
    <el-dialog v-model="dialogVisible" title="新建学习计划" width="560px" class="plan-dialog">
      <el-form :model="form" label-width="100px" class="plan-form">
        <el-form-item label="计划名称">
          <el-input v-model="form.name" placeholder="例如：期末复习计划" />
        </el-form-item>
        <el-form-item label="计划描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="可选，描述计划的目标和内容" />
        </el-form-item>
        <el-form-item label="学习资料">
          <el-select
            v-model="form.materialIds"
            multiple
            placeholder="请选择学习资料"
            style="width: 100%"
          >
            <el-option
              v-for="m in materials"
              :key="m.id"
              :label="m.title"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" placeholder="选择开始日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" placeholder="选择结束日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="每日时长">
          <el-input-number v-model="form.dailyHours" :min="0.5" :max="12" :step="0.5" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { usePlanStore } from '@/stores/plan'
import { getMaterials } from '@/api/plan'
import type { MaterialVO } from '@/api/plan'

const router = useRouter()
const store = usePlanStore()

const planList = ref(store.planList)
const loading = ref(store.loading)
const dialogVisible = ref(false)
const submitting = ref(false)
const materials = ref<MaterialVO[]>([])

const isLoggedIn = computed(() => !!localStorage.getItem('token'))

const form = ref({
  name: '',
  description: '',
  startDate: '',
  endDate: '',
  dailyHours: 2,
  materialIds: [] as number[],
})

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
  } catch {
    materials.value = []
  }
}

async function submitCreate() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入计划名称')
    return
  }
  if (form.value.materialIds.length === 0) {
    ElMessage.warning('请至少选择一项学习资料')
    return
  }
  if (!form.value.startDate || !form.value.endDate) {
    ElMessage.warning('请选择起止日期')
    return
  }
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
  } finally {
    submitting.value = false
  }
}

function goDetail(id: number) {
  router.push(`/plans/${id}`)
}

function goLogin() {
  router.push('/login')
}

onMounted(() => {
  if (isLoggedIn.value) {
    store.fetchPlanList()
  }
})
</script>

<style scoped>
.plan-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--space-4);
}

.plan-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.plan-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  border-color: transparent;
}

.plan-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-3);
  gap: var(--space-2);
}

.plan-name {
  font-size: var(--text-lg);
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

.meta-icon {
  opacity: 0.7;
}

.plan-progress {
  margin-top: var(--space-2);
}

/* 弹窗表单 */
.plan-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text-secondary);
}
</style>
