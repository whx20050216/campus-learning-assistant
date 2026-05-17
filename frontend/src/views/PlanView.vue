<template>
  <div class="plan-view">
    <div class="page-header">
      <h1 class="title">📋 学习计划</h1>
      <el-button type="primary" @click="openCreateDialog">+ 新建计划</el-button>
    </div>

    <div v-if="loading" class="loading-state">加载中...</div>

    <div v-else-if="planList.length === 0" class="empty-state">
      <div class="empty-icon">📚</div>
      <p class="empty-text">暂无学习计划</p>
      <el-button type="primary" @click="openCreateDialog">创建第一个计划</el-button>
    </div>

    <div v-else class="plan-list">
      <div
        v-for="plan in planList"
        :key="plan.id"
        class="plan-card"
        @click="goDetail(plan.id)"
      >
        <div class="plan-header">
          <h3 class="plan-name">{{ plan.name }}</h3>
          <el-tag :type="statusType(plan.status)">{{ statusLabel(plan.status) }}</el-tag>
        </div>
        <div class="plan-meta">
          <span>📅 截止日期：{{ plan.endDate }}</span>
          <span>📖 总页数：{{ plan.totalPages }} 页</span>
        </div>
        <div class="plan-progress">
          <el-progress :percentage="Math.round(plan.progress || 0)" :status="progressStatus(plan.status)" />
        </div>
      </div>
    </div>

    <!-- 新建计划弹窗 -->
    <el-dialog v-model="dialogVisible" title="新建学习计划" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="计划名称">
          <el-input v-model="form.name" placeholder="例如：期末复习计划" />
        </el-form-item>
        <el-form-item label="计划描述">
          <el-input v-model="form.description" type="textarea" placeholder="可选" />
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
import { ref, onMounted } from 'vue'
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

onMounted(() => {
  store.fetchPlanList()
})
</script>

<style scoped>
.plan-view {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.loading-state {
  text-align: center;
  padding: 40px;
  color: #909399;
}
.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: #fff;
  border-radius: 12px;
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
.empty-text {
  color: #606266;
  margin-bottom: 16px;
}
.plan-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
.plan-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: box-shadow 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.plan-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}
.plan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.plan-name {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: #303133;
}
.plan-meta {
  display: flex;
  gap: 16px;
  color: #606266;
  font-size: 14px;
  margin-bottom: 12px;
}
.plan-progress {
  margin-top: 8px;
}
</style>
