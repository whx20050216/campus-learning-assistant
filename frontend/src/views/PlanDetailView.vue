<template>
  <div class="page-container plan-detail">
    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" size="32"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <template v-else-if="plan">
      <!-- 返回按钮 -->
      <div class="header-actions">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon> 返回列表
        </el-button>
      </div>

      <!-- 顶部信息卡 -->
      <div class="info-card">
        <div class="info-card-header">
          <div class="info-card-title">
            <h1 class="plan-title">{{ plan.name }}</h1>
            <el-tag
              v-if="plan.interruptionRisk === 1"
              type="danger"
              size="small"
              effect="light"
            >
              ⚠️ 学习中断风险
            </el-tag>
            <el-tag :type="statusType(plan.status)" size="small" effect="light">
              {{ statusLabel(plan.status) }}
            </el-tag>
            <el-tag
              v-if="showReminder(plan)"
              type="warning"
              size="small"
              effect="dark"
              class="reminder-tag"
            >
              ⏰ 即将到期
            </el-tag>
            <el-button
              v-if="showReminder(plan)"
              type="warning"
              size="small"
              plain
              @click="handleReadReminder(plan.id)"
            >
              知道了
            </el-button>
            <el-button size="small" @click="openEditPlan">
              <el-icon><Edit /></el-icon> 编辑计划
            </el-button>
          </div>
          <p class="plan-desc">{{ plan.description || '暂无描述' }}</p>
        </div>

        <!-- 三列统计 -->
        <div class="stats-row">
          <div class="stat-item">
            <div class="stat-number">{{ tasks.length }}</div>
            <div class="stat-label">总任务数</div>
          </div>
          <div class="stat-item">
            <div class="stat-number" style="color: var(--success-500)">
              {{ tasks.filter(t => t.status === 'completed').length }}
            </div>
            <div class="stat-label">已完成</div>
          </div>
          <div class="stat-item">
            <div class="stat-number" style="color: var(--primary-500)">
              {{ Math.max(0, Math.ceil((new Date(plan.endDate).getTime() - Date.now()) / (1000 * 60 * 60 * 24))) }}
            </div>
            <div class="stat-label">剩余天数</div>
          </div>
        </div>

        <div class="info-row">
          <span class="info-item">
            <el-icon><Calendar /></el-icon>
            {{ plan.startDate }} ~ {{ plan.endDate }}
          </span>
          <span class="info-item">
            <el-icon><Clock /></el-icon>
            每日 {{ plan.dailyHours }} 小时
          </span>
          <span class="info-item">
            <el-icon><Document /></el-icon>
            共 {{ plan.totalPages }} 页
          </span>
        </div>

        <div class="progress-row">
          <span class="progress-label">总进度</span>
          <el-progress
            :percentage="Math.round(plan.progress || 0)"
            :status="progressStatus(plan.status)"
            :stroke-width="12"
            style="flex: 1"
          />
          <span class="progress-value">{{ (plan.progress || 0).toFixed(1) }}%</span>
        </div>
      </div>

      <!-- 包含资料 -->
      <div v-if="plan.materials?.length" class="materials-card">
        <h3 class="section-title">
          计划包含资料
          <el-button type="primary" size="small" @click.stop="openGenerateExam">
            <el-icon><MagicStick /></el-icon> 一键智能组卷
          </el-button>
        </h3>
        <div class="materials-list">
          <div
            v-for="m in plan.materials"
            :key="m.id"
            class="material-item"
            @click="router.push(`/materials/${m.id}`)"
          >
            <el-icon class="material-icon" :size="20"><component :is="m.fileType?.toUpperCase?.() === 'PDF' ? Document : Picture" /></el-icon>
            <div class="material-info">
              <div class="material-title">{{ m.title }}</div>
              <div class="material-meta">
                <span v-if="m.pages">{{ m.pages }} 页</span>
                <el-tag v-if="m.courseTag" size="small" type="info" effect="plain">{{ m.courseTag }}</el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ECharts 区域 -->
      <div class="chart-card">
        <h3 class="section-title">
          <el-icon><Histogram /></el-icon>
          学习进度分布
        </h3>
        <v-chart class="chart" :option="chartOption" autoresize />
      </div>

      <!-- 任务列表 -->
      <div class="task-card">
        <h3 class="section-title">
          <el-icon><List /></el-icon>
          任务列表
        </h3>
        <div class="task-list">
          <div
            v-for="task in tasks"
            :key="task.id"
            class="task-item"
            :class="{ completed: task.status === 'completed' }"
          >
            <div class="task-timeline">
              <div class="timeline-dot" :class="task.status"></div>
              <div class="timeline-line"></div>
            </div>
            <div class="task-content">
              <div class="task-date-badge">{{ task.taskDate }}</div>
              <div class="task-name">{{ task.taskName }}</div>
              <div v-if="task.materialId" class="task-material-link">
                学习资料：
                <el-link type="primary" @click="router.push(`/materials/${task.materialId}`)">
                  查看资料
                </el-link>
              </div>
              <div v-else class="task-material-link" style="opacity: 0.6;">
                学习资料：已删除或不可用
              </div>
              <div class="task-meta">
                <span>计划 {{ task.plannedHours }} 小时</span>
              </div>
            </div>
            <div class="task-actions">
              <template v-if="task.status === 'completed'">
                <el-tag type="success" size="small" effect="light">
                  <el-icon><CircleCheck /></el-icon> 已完成
                </el-tag>
              </template>
              <template v-else>
                <el-button type="primary" size="small" @click="openCheckIn(task)">
                  <el-icon><Check /></el-icon> 打卡
                </el-button>
              </template>
            </div>
          </div>
        </div>
      </div>

      <!-- 打卡弹窗 -->
      <el-dialog v-model="checkInVisible" title="学习打卡" width="480px" align-center class="checkin-dialog">
        <div class="checkin-header">
          <el-icon class="checkin-icon" :size="48"><Collection /></el-icon>
          <div class="checkin-task-name">{{ currentTask?.taskName }}</div>
        </div>
        <el-form :model="checkInForm" label-width="90px">
          <el-form-item label="打卡日期">
            <el-date-picker
              v-model="checkInForm.studyDate"
              type="date"
              placeholder="选择打卡日期"
              :disabled-date="disabledCheckInDate"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="学习时长">
            <el-input-number
              v-model="checkInForm.duration"
              :min="1"
              :max="480"
              placeholder="分钟"
              size="large"
              style="width: 100%"
            />
            <span class="input-suffix">分钟</span>
          </el-form-item>
          <el-form-item label="学习内容">
            <el-input
              v-model="checkInForm.content"
              type="textarea"
              :rows="4"
              placeholder="简要记录今日学习内容"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button size="large" @click="checkInVisible = false">取消</el-button>
            <el-button type="primary" size="large" :loading="checkInLoading" @click="submitCheckIn">
              确认打卡
            </el-button>
          </div>
        </template>
      </el-dialog>

      <!-- 编辑计划弹窗 -->
      <el-dialog v-model="editPlanVisible" title="编辑计划" width="480px" align-center>
        <el-form :model="editPlanForm" label-width="90px">
          <el-form-item label="计划名称">
            <el-input v-model="editPlanForm.name" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="截止日期">
            <el-date-picker
              v-model="editPlanForm.endDate"
              type="date"
              placeholder="选择截止日期"
              :disabled-date="disabledEndDate"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="每日时长">
            <el-input-number
              v-model="editPlanForm.dailyHours"
              :min="0.5"
              :step="0.5"
              style="width: 100%"
            />
            <span class="input-suffix">小时</span>
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button size="large" @click="editPlanVisible = false">取消</el-button>
            <el-button type="primary" size="large" :loading="editPlanLoading" @click="submitEditPlan">
              保存
            </el-button>
          </div>
        </template>
      </el-dialog>

      <!-- 一键组卷弹窗 -->
      <el-dialog v-model="generateExamVisible" title="一键智能组卷" width="500px">
        <el-form label-width="100px">
          <el-form-item label="课程标签">
            <el-input v-model="generateExamTag" placeholder="请输入课程标签，用于分类统计" />
            <div class="input-suffix" style="margin-top: 4px;">
              基于 {{ plan.materials?.length || 0 }} 份资料生成 {{ generateExamCount }} 道题
            </div>
          </el-form-item>
          <el-form-item label="题目数量">
            <el-slider v-model="generateExamCount" :min="5" :max="30" show-stops />
          </el-form-item>
          <el-form-item label="难度">
            <el-radio-group v-model="generateExamDifficulty">
              <el-radio-button label="easy">简单</el-radio-button>
              <el-radio-button label="medium">中等</el-radio-button>
              <el-radio-button label="hard">困难</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button size="large" @click="generateExamVisible = false">取消</el-button>
            <el-button type="primary" size="large" :loading="generateExamLoading" @click="submitGenerateExam">
              开始组卷
            </el-button>
          </div>
        </template>
      </el-dialog>
    </template>

    <div v-else class="loading-state">
      <el-empty description="计划不存在或已删除" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, ArrowLeft, Calendar, Clock, Document, Histogram, List, Check, CircleCheck, Edit, Collection, Picture, Link, MagicStick } from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { usePlanStore } from '@/stores/plan'
import type { PlanVO, TaskVO } from '@/api/plan'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent, LegendComponent])

const route = useRoute()
const router = useRouter()
const store = usePlanStore()

const plan = computed(() => store.currentPlan)
const tasks = computed(() => store.taskList)
const loading = ref(false)

const checkInVisible = ref(false)
const checkInLoading = ref(false)
const currentTask = ref<TaskVO | null>(null)
const checkInForm = ref({ duration: 30, content: '', studyDate: '' })

const editPlanVisible = ref(false)
const editPlanLoading = ref(false)
const editPlanForm = ref({ name: '', endDate: '', dailyHours: 1 })

const generateExamVisible = ref(false)
const generateExamLoading = ref(false)
const generateExamTag = ref('')
const generateExamCount = ref(10)
const generateExamDifficulty = ref('medium')

function openGenerateExam() {
  const mats = plan.value?.materials || []
  const tags = [...new Set(mats.map((m) => m.courseTag).filter(Boolean))]
  generateExamTag.value = tags.length === 1 ? tags[0] : ''
  generateExamCount.value = 10
  generateExamDifficulty.value = 'medium'
  generateExamVisible.value = true
}

async function submitGenerateExam() {
  if (!generateExamTag.value.trim()) {
    ElMessage.warning('请输入课程标签')
    return
  }
  const mats = plan.value?.materials || []
  if (mats.length === 0) {
    ElMessage.warning('计划中没有资料，无法组卷')
    return
  }
  generateExamLoading.value = true
  try {
    const { generateExamFromPlan } = await import('@/api/exam')
    const res = await generateExamFromPlan({
      studyPlanId: plan.value!.id,
      materialIds: mats.map((m) => m.id),
      courseTag: generateExamTag.value.trim(),
      questionCount: generateExamCount.value,
      difficulty: generateExamDifficulty.value,
    })
    if (res.code === 200 && res.data) {
      ElMessage.success('组卷成功')
      generateExamVisible.value = false
      router.push(`/exams/${res.data.id}/answer`)
    } else {
      ElMessage.error(res.msg || '组卷失败')
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.msg || err.message || '组卷失败')
  } finally {
    generateExamLoading.value = false
  }
}

const chartOption = computed(() => {
  const list = tasks.value
  const dates = list.map((t) => t.taskDate)
  const completedData = list.map((t) => (t.status === 'completed' ? 1 : 0))
  const pendingData = list.map((t) => (t.status !== 'completed' ? 1 : 0))

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    legend: {
      data: ['已完成', '未完成'],
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { rotate: 45, fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      name: '任务量',
      minInterval: 1,
    },
    series: [
      {
        name: '已完成',
        type: 'bar',
        stack: 'total',
        data: completedData,
        itemStyle: { color: '#22c55e', borderRadius: [6, 6, 0, 0] },
      },
      {
        name: '未完成',
        type: 'bar',
        stack: 'total',
        data: pendingData,
        itemStyle: { color: '#f59e0b', borderRadius: [6, 6, 0, 0] },
      },
    ],
  }
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

function goBack() {
  router.push('/plans')
}

function showReminder(p: PlanVO) {
  if (!p.remindDate || p.reminderSent !== 0) return false
  const remind = new Date(p.remindDate)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return remind <= today
}

async function handleReadReminder(planId: number) {
  const res = await store.readReminder(planId)
  if (res.code === 200) {
    ElMessage.success('已标记为已读')
    await store.fetchPlanDetail(planId)
  } else {
    ElMessage.error(res.msg || '操作失败')
  }
}

function openCheckIn(task: TaskVO) {
  currentTask.value = task
  const today = new Date().toISOString().split('T')[0]
  checkInForm.value = { duration: 30, content: '', studyDate: today }
  checkInVisible.value = true
}

function disabledCheckInDate(date: Date) {
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  return date.getTime() > today.getTime()
}

function disabledEndDate(date: Date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

function openEditPlan() {
  if (!plan.value) return
  editPlanForm.value = {
    name: plan.value.name,
    endDate: plan.value.endDate,
    dailyHours: plan.value.dailyHours,
  }
  editPlanVisible.value = true
}

async function submitEditPlan() {
  if (!plan.value) return
  if (!editPlanForm.value.name || !editPlanForm.value.name.trim()) {
    ElMessage.warning('计划名称不能为空')
    return
  }
  editPlanLoading.value = true
  try {
    const res = await store.updatePlan(plan.value.id, {
      name: editPlanForm.value.name.trim(),
      endDate: editPlanForm.value.endDate,
      dailyHours: editPlanForm.value.dailyHours,
    })
    if (res.code === 200) {
      ElMessage.success('计划已更新，任务已重新生成')
      editPlanVisible.value = false
    } else {
      ElMessage.error(res.msg || '更新失败')
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.msg || err.message || '更新失败')
  } finally {
    editPlanLoading.value = false
  }
}

async function submitCheckIn() {
  if (!currentTask.value) return
  if (!checkInForm.value.duration || checkInForm.value.duration <= 0) {
    ElMessage.warning('请输入有效的学习时长')
    return
  }
  checkInLoading.value = true
  try {
    const res = await store.checkInTask(currentTask.value.id, {
      duration: checkInForm.value.duration,
      content: checkInForm.value.content,
      studyDate: checkInForm.value.studyDate || undefined,
    })
    if (res.code === 200) {
      ElMessage.success('打卡成功')
      checkInVisible.value = false
    } else {
      ElMessage.error(res.msg || '打卡失败')
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.msg || err.message || '打卡失败')
  } finally {
    checkInLoading.value = false
  }
}

async function load() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    await store.fetchPlanDetail(id)
  } catch (err: any) {
    ElMessage.error(err.response?.data?.msg || err.message || '获取计划详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<style scoped>
.header-actions {
  margin-bottom: var(--space-4);
}

/* 顶部信息卡 */
.info-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  margin-bottom: var(--space-5);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.info-card-header {
  margin-bottom: var(--space-5);
}

.info-card-title {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-2);
  flex-wrap: wrap;
}

.plan-title {
  font-size: var(--text-2xl);
  font-weight: 700;
  margin: 0;
  color: var(--text-primary);
  line-height: 1.3;
}

.plan-desc {
  color: var(--text-secondary);
  margin: 0;
  font-size: var(--text-sm);
  line-height: 1.6;
}

.reminder-tag {
  margin-left: var(--space-1);
}

/* 三列统计 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-5);
  padding: var(--space-4);
  background: var(--bg-hover);
  border-radius: var(--radius-md);
}

.stat-item {
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.stat-number {
  font-size: var(--text-3xl);
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  font-weight: 500;
}

.info-row {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  flex-wrap: wrap;
  margin-bottom: var(--space-4);
  color: var(--text-secondary);
  font-size: var(--text-sm);
}

.info-item {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.info-item .el-icon {
  color: var(--text-tertiary);
}

.progress-row {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--border-color);
}

.progress-label {
  font-weight: 600;
  color: var(--text-primary);
  font-size: var(--text-sm);
  white-space: nowrap;
}

.progress-value {
  font-weight: 700;
  color: var(--primary-500);
  min-width: 60px;
  text-align: right;
  font-size: var(--text-lg);
}

/* 图表卡片 */
.chart-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  margin-bottom: var(--space-5);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.section-title {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 var(--space-5);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.chart {
  width: 100%;
  height: 300px;
}

/* 任务卡片 */
.task-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.task-list {
  display: flex;
  flex-direction: column;
}

.task-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-4) 0;
  border-bottom: 1px solid var(--border-color);
  transition: background 0.2s ease;
}

.task-item:last-child {
  border-bottom: none;
}

.task-item:hover {
  background: var(--bg-hover);
  margin: 0 calc(-1 * var(--space-6));
  padding-left: var(--space-6);
  padding-right: var(--space-6);
}

/* 时间线 */
.task-timeline {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 20px;
  padding-top: var(--space-1);
}

.timeline-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid var(--border-color);
  background: var(--bg-card);
  flex-shrink: 0;
}

.timeline-dot.completed {
  background: var(--success-500);
  border-color: var(--success-500);
}

.timeline-line {
  width: 2px;
  flex: 1;
  background: var(--border-color);
  min-height: 20px;
}

.task-item:last-child .timeline-line {
  display: none;
}

.task-content {
  flex: 1;
  min-width: 0;
}

.task-date-badge {
  display: inline-block;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  background: var(--bg-hover);
  padding: 2px var(--space-2);
  border-radius: var(--radius-sm);
  margin-bottom: var(--space-1);
  font-weight: 500;
}

.task-name {
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-1);
  font-size: var(--text-base);
}

.task-material-link {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-bottom: 4px;
}

.task-meta {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.task-actions {
  flex-shrink: 0;
  padding-top: var(--space-1);
}

/* 打卡弹窗 */
.checkin-dialog :deep(.el-dialog__body) {
  padding-top: var(--space-4);
}

.checkin-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--border-color);
}

.checkin-icon {
  color: var(--primary-500);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.checkin-task-name {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--text-primary);
}

.input-suffix {
  margin-left: var(--space-2);
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

.materials-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  margin-bottom: var(--space-5);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.materials-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.material-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.2s ease;
}

.material-item:hover {
  background: var(--primary-50);
}

.material-icon {
  font-size: 28px;
  flex-shrink: 0;
}

.material-info {
  flex: 1;
  min-width: 0;
}

.material-title {
  font-weight: 500;
  color: var(--text-primary);
  font-size: var(--text-sm);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: 2px;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12);
  gap: var(--space-3);
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
    gap: var(--space-3);
  }

  .info-row {
    flex-direction: column;
    gap: var(--space-2);
    align-items: flex-start;
  }

  .task-item {
    flex-direction: column;
    gap: var(--space-2);
  }

  .task-actions {
    width: 100%;
  }
}
</style>
