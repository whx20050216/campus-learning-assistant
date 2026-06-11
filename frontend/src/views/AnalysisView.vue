<template>
  <div v-loading="loading" class="page-container analysis-view">
    <!-- 页面标题 -->
    <div class="page-header-center">
      <h1 class="page-title-large">学习数据分析</h1>
      <p class="page-subtitle">可视化展示您的学习数据与进度</p>
    </div>

    <!-- 未登录提示 -->
    <div v-if="!isLoggedIn" class="empty-state">
      <div class="empty-icon"><el-icon :size="28"><Lock /></el-icon></div>
      <p class="empty-text">请先登录</p>
      <p class="empty-tip">登录后即可查看您的学习数据分析</p>
      <el-button type="primary" @click="goLogin">去登录</el-button>
    </div>

    <!-- 无数据空状态 -->
    <div v-else-if="!hasData" class="empty-state empty-state-flat">
      <el-empty description="暂无学习数据，快去上传资料并制定计划吧">
        <el-button type="primary" @click="goUpload">去上传资料</el-button>
        <el-button @click="goPlan">制定计划</el-button>
      </el-empty>
    </div>

    <!-- 有数据仪表盘 -->
    <div v-else class="dashboard">
      <!-- 时间维度切换 -->
      <div class="time-range-switch">
        <el-radio-group v-model="timeRange" size="large" @change="onTimeRangeChange">
          <el-radio-button label="week">本周</el-radio-button>
          <el-radio-button label="month">本月</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 顶部数据卡行 -->
      <div class="data-cards-row">
        <div class="data-card" style="--card-accent: var(--primary-500);">
          <div class="data-card-topbar"></div>
          <div class="data-card-body">
            <div class="data-card-label">{{ currentPeriodLabel }}学习时长</div>
            <div class="data-card-value">{{ formatDuration(dashboard.weeklyDuration) }}</div>
            <div class="data-card-compare">
              <span v-if="dashboard.lastWeekDuration > 0" :class="weekCompareClass">
                <el-icon v-if="weekCompareClass === 'compare-up'"><ArrowUp /></el-icon>
                <el-icon v-else-if="weekCompareClass === 'compare-down'"><ArrowDown /></el-icon>
                <el-icon v-else><Right /></el-icon>
                {{ weekCompareText }}
              </span>
              <span v-else class="compare-neutral">{{ previousPeriodLabel }}无记录</span>
            </div>
          </div>
        </div>
        <div class="data-card" style="--card-accent: var(--success-500);">
          <div class="data-card-topbar"></div>
          <div class="data-card-body">
            <div class="data-card-label">{{ previousPeriodLabel }}学习时长</div>
            <div class="data-card-value">{{ formatDuration(dashboard.lastWeekDuration) }}</div>
            <div class="data-card-compare compare-neutral">基准对比</div>
          </div>
        </div>
        <div class="data-card" style="--card-accent: var(--warning-500);">
          <div class="data-card-topbar"></div>
          <div class="data-card-body">
            <div class="data-card-label">进行中计划</div>
            <div class="data-card-value">{{ dashboard.currentProgress?.length || 0 }}</div>
            <div class="data-card-compare compare-neutral">个活跃计划</div>
          </div>
        </div>
        <div class="data-card" style="--card-accent: var(--primary-400);">
          <div class="data-card-topbar"></div>
          <div class="data-card-body">
            <div class="data-card-label">总资料数</div>
            <div class="data-card-value">{{ dashboard.materialCount || 0 }}</div>
            <div class="data-card-compare compare-neutral">份学习资料</div>
          </div>
        </div>
        <div class="data-card" style="--card-accent: var(--danger-500);">
          <div class="data-card-topbar"></div>
          <div class="data-card-body">
            <div class="data-card-label">总试卷数</div>
            <div class="data-card-value">{{ dashboard.examCount || 0 }}</div>
            <div class="data-card-compare compare-neutral">份智能试卷</div>
          </div>
        </div>
      </div>

      <!-- 中部三栏 -->
      <div class="chart-row">
        <div class="chart-card">
          <h3 class="card-title">
            <el-icon><PieChart /></el-icon>
            课程分布（资料）
          </h3>
          <v-chart class="pie-chart" :option="pieOption" autoresize />
          <div v-if="!dashboard.courseDistribution?.length" class="empty-chart">暂无资料数据</div>
        </div>
        <div class="chart-card">
          <h3 class="card-title">
            <el-icon><PieChart /></el-icon>
            课程分布（试卷）
          </h3>
          <v-chart class="pie-chart" :option="examPieOption" autoresize />
          <div v-if="!dashboard.examDistribution?.length" class="empty-chart">暂无试卷数据</div>
        </div>
        <div class="chart-card">
          <h3 class="card-title">
            <el-icon><TrendCharts /></el-icon>
            计划进度
          </h3>
          <div v-if="dashboard.currentProgress?.length" class="progress-list">
            <div v-for="item in dashboard.currentProgress" :key="item.name" class="progress-item">
              <div class="progress-header">
                <span class="progress-name">{{ item.name }}</span>
                <span class="progress-percent">{{ (item.progress || 0).toFixed(1) }}%</span>
              </div>
              <el-progress :percentage="Math.round(item.progress || 0)" :stroke-width="6" :show-text="false" />
            </div>
          </div>
          <div v-else class="empty-chart">暂无进行中的计划</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowUp, ArrowDown, Right, PieChart, TrendCharts, Lock } from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart as EchartsPieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { usePlanStore } from '@/stores/plan'

use([CanvasRenderer, EchartsPieChart, TooltipComponent, LegendComponent])

const router = useRouter()
const store = usePlanStore()
const dashboard = computed(() => store.dashboardData)
const loading = ref(false)
const isLoggedIn = computed(() => !!localStorage.getItem('token'))
const timeRange = ref<'week' | 'month'>('week')

const currentPeriodLabel = computed(() => (timeRange.value === 'month' ? '本月' : '本周'))
const previousPeriodLabel = computed(() => (timeRange.value === 'month' ? '上月' : '上周'))

const hasData = computed(() => {
  const d = dashboard.value
  if (!d) return false
  return !!(
    d.weeklyDuration ||
    d.lastWeekDuration ||
    (d.courseDistribution && d.courseDistribution.length) ||
    (d.examDistribution && d.examDistribution.length) ||
    (d.currentProgress && d.currentProgress.length)
  )
})

function goLogin() {
  router.push('/login')
}

function goUpload() {
  router.push('/upload')
}

function goPlan() {
  router.push('/plans')
}

const pieOption = computed(() => {
  const data = dashboard.value?.courseDistribution || []
  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      bottom: '0',
      left: 'center',
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: true,
          formatter: '{b}\n{c}',
        },
        data: data.map((item) => ({
          name: item.name,
          value: item.value,
        })),
      },
    ],
  }
})

const examPieOption = computed(() => {
  const data = dashboard.value?.examDistribution || []
  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      bottom: '0',
      left: 'center',
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: true,
          formatter: '{b}\n{c}',
        },
        data: data.map((item) => ({
          name: item.name,
          value: item.value,
        })),
      },
    ],
  }
})

const weekCompareClass = computed(() => {
  const cur = dashboard.value?.weeklyDuration || 0
  const last = dashboard.value?.lastWeekDuration || 0
  if (cur > last) return 'compare-up'
  if (cur < last) return 'compare-down'
  return 'compare-neutral'
})

const weekCompareText = computed(() => {
  const cur = dashboard.value?.weeklyDuration || 0
  const last = dashboard.value?.lastWeekDuration || 0
  if (last === 0) return `${previousPeriodLabel.value}无记录`
  const diff = cur - last
  const percent = Math.round((Math.abs(diff) / last) * 100)
  return `${diff >= 0 ? '增加' : '减少'} ${percent}%`
})

function formatDuration(minutes: number) {
  if (!minutes || minutes <= 0) return '0 分钟'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  if (h > 0 && m > 0) return `${h} 小时 ${m} 分钟`
  if (h > 0) return `${h} 小时`
  return `${m} 分钟`
}

function onTimeRangeChange(val: 'week' | 'month') {
  loading.value = true
  store.fetchDashboard(val)
    .catch((err: any) => {
      ElMessage.error(err.response?.data?.msg || err.message || '获取仪表盘数据失败')
    })
    .finally(() => {
      loading.value = false
    })
}

onMounted(() => {
  if (isLoggedIn.value) {
    loading.value = true
    store.fetchDashboard(timeRange.value)
      .catch((err: any) => {
        ElMessage.error(err.response?.data?.msg || err.message || '获取仪表盘数据失败')
      })
      .finally(() => {
        loading.value = false
      })
  }
})
</script>

<style scoped>
.page-header-center {
  text-align: center;
  margin-bottom: var(--space-8);
}

.page-title-large {
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 var(--space-2);
}

.page-subtitle {
  color: var(--text-tertiary);
  font-size: var(--text-base);
  margin: 0;
}

/* 时间维度切换 */
.time-range-switch {
  display: flex;
  justify-content: center;
  margin-bottom: var(--space-6);
}

/* 顶部数据卡行 */
.data-cards-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

@media (max-width: 1200px) {
  .data-cards-row {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .data-cards-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .data-cards-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .data-cards-row {
    grid-template-columns: 1fr;
  }
}

.data-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  transition: all 0.2s ease;
}

.data-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.data-card-topbar {
  height: 4px;
  background: var(--card-accent);
}

.data-card-body {
  padding: var(--space-5);
}

.data-card-label {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-2);
}

.data-card-value {
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: var(--space-2);
  line-height: 1.2;
}

.data-card-compare {
  font-size: var(--text-xs);
  display: flex;
  align-items: center;
  gap: 4px;
}

.compare-up {
  color: var(--success-500);
  font-weight: 500;
}

.compare-down {
  color: var(--danger-500);
  font-weight: 500;
}

.compare-neutral {
  color: var(--text-tertiary);
}

/* 中部三栏 */
.chart-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
}

@media (max-width: 1200px) {
  .chart-row {
    grid-template-columns: 1fr;
  }
}

.chart-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.card-title {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 var(--space-5);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.pie-chart {
  width: 100%;
  height: 320px;
}

.empty-chart {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-12) var(--space-4);
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

.progress-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.progress-item {
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-2);
}

.progress-name {
  font-weight: 600;
  color: var(--text-primary);
  font-size: var(--text-sm);
}

.progress-percent {
  color: var(--primary-500);
  font-weight: 700;
  font-size: var(--text-sm);
}

@media (max-width: 768px) {
  .chart-row {
    grid-template-columns: 1fr;
  }
}
</style>
