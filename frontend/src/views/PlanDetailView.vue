<template>
  <div class="plan-detail" v-if="plan">
    <div class="header-actions">
      <el-button @click="goBack">← 返回列表</el-button>
    </div>

    <div class="info-card">
      <h1 class="plan-title">{{ plan.name }}</h1>
      <p class="plan-desc">{{ plan.description || '暂无描述' }}</p>
      <div class="info-row">
        <span>📅 {{ plan.startDate }} ~ {{ plan.endDate }}</span>
        <span>⏱️ 每日 {{ plan.dailyHours }} 小时</span>
        <span>📖 共 {{ plan.totalPages }} 页</span>
        <el-tag :type="statusType(plan.status)">{{ statusLabel(plan.status) }}</el-tag>
      </div>
      <div class="progress-row">
        <span class="progress-label">总进度</span>
        <el-progress :percentage="Math.round(plan.progress || 0)" :status="progressStatus(plan.status)" style="flex: 1" />
        <span class="progress-value">{{ (plan.progress || 0).toFixed(1) }}%</span>
      </div>
    </div>

    <div class="chart-card">
      <h3 class="section-title">📊 每日任务分布</h3>
      <v-chart class="chart" :option="chartOption" autoresize />
    </div>

    <div class="task-card">
      <h3 class="section-title">📝 任务列表</h3>
      <div class="task-list">
        <div
          v-for="task in tasks"
          :key="task.id"
          class="task-item"
          :class="{ completed: task.status === 'completed' }"
        >
          <div class="task-info">
            <div class="task-name">{{ task.taskName }}</div>
            <div class="task-meta">
              <span>{{ task.taskDate }}</span>
              <span>计划 {{ task.plannedHours }} 小时</span>
            </div>
          </div>
          <div class="task-actions">
            <el-tag v-if="task.status === 'completed'" type="success">已完成</el-tag>
            <el-button
              v-else
              type="primary"
              size="small"
              @click="openCheckIn(task)"
            >
              打卡
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 打卡弹窗 -->
    <el-dialog v-model="checkInVisible" title="学习打卡" width="400px">
      <el-form :model="checkInForm" label-width="80px">
        <el-form-item label="学习时长">
          <el-input-number v-model="checkInForm.duration" :min="1" :max="480" placeholder="分钟" style="width: 100%" />
        </el-form-item>
        <el-form-item label="学习内容">
          <el-input v-model="checkInForm.content" type="textarea" placeholder="简要记录今日学习内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkInVisible = false">取消</el-button>
        <el-button type="primary" :loading="checkInLoading" @click="submitCheckIn">确认打卡</el-button>
      </template>
    </el-dialog>
  </div>

  <div v-else class="loading-state">加载中...</div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { usePlanStore } from '@/stores/plan'
import type { TaskVO } from '@/api/plan'

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
const checkInForm = ref({ duration: 30, content: '' })

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
        itemStyle: { color: '#67c23a' },
      },
      {
        name: '未完成',
        type: 'bar',
        stack: 'total',
        data: pendingData,
        itemStyle: { color: '#e6a23c' },
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

function openCheckIn(task: TaskVO) {
  currentTask.value = task
  checkInForm.value = { duration: 30, content: '' }
  checkInVisible.value = true
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
    })
    if (res.code === 200) {
      ElMessage.success('打卡成功')
      checkInVisible.value = false
    } else {
      ElMessage.error(res.msg || '打卡失败')
    }
  } finally {
    checkInLoading.value = false
  }
}

async function load() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  await store.fetchPlanDetail(id)
  loading.value = false
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<style scoped>
.plan-detail {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}
.header-actions {
  margin-bottom: 16px;
}
.info-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.plan-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 8px;
  color: #303133;
}
.plan-desc {
  color: #606266;
  margin: 0 0 16px;
}
.info-row {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  color: #606266;
  font-size: 14px;
}
.progress-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.progress-label {
  font-weight: 500;
  color: #303133;
}
.progress-value {
  font-weight: 600;
  color: #409eff;
  min-width: 60px;
  text-align: right;
}
.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.section-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 16px;
  color: #303133;
}
.chart {
  width: 100%;
  height: 320px;
}
.task-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  background: #f5f7fa;
  transition: background 0.3s;
}
.task-item.completed {
  background: #f0f9eb;
}
.task-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}
.task-meta {
  font-size: 13px;
  color: #909399;
  display: flex;
  gap: 12px;
}
.loading-state {
  text-align: center;
  padding: 60px;
  color: #909399;
}
</style>
