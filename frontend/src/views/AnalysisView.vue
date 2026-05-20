<template>
  <div class="page-container analysis-view">
    <h1 class="page-title">📊 数据分析仪表盘</h1>

    <!-- 未登录提示 -->
    <div v-if="!isLoggedIn" class="empty-state">
      <div class="empty-icon">🔒</div>
      <p class="empty-text">请先登录</p>
      <p class="empty-tip">登录后即可查看您的学习数据分析</p>
      <el-button type="primary" @click="goLogin">去登录</el-button>
    </div>

    <div v-else-if="loading" class="loading-state">加载中...</div>

    <div v-else-if="dashboard" class="dashboard">
      <!-- 时长对比卡片 -->
      <div class="card-row">
        <div class="stat-card">
          <div class="stat-label">本周学习时长</div>
          <div class="stat-value">{{ formatDuration(dashboard.weeklyDuration) }}</div>
          <div class="stat-compare">
            <span v-if="dashboard.lastWeekDuration > 0" :class="weekCompareClass">
              {{ weekCompareIcon }} {{ weekCompareText }}
            </span>
            <span v-else class="compare-neutral">上周无记录</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-label">上周学习时长</div>
          <div class="stat-value">{{ formatDuration(dashboard.lastWeekDuration) }}</div>
          <div class="stat-compare compare-neutral">基准对比</div>
        </div>
      </div>

      <!-- 饼图 + 进度条 -->
      <div class="chart-row">
        <div class="chart-card">
          <h3 class="card-title">📚 课程分布</h3>
          <v-chart class="pie-chart" :option="pieOption" autoresize />
          <div v-if="!dashboard.courseDistribution?.length" class="empty-chart">暂无资料数据</div>
        </div>
        <div class="chart-card">
          <h3 class="card-title">🎯 进行中计划进度</h3>
          <div v-if="dashboard.currentProgress?.length" class="progress-list">
            <div v-for="item in dashboard.currentProgress" :key="item.name" class="progress-item">
              <div class="progress-header">
                <span class="progress-name">{{ item.name }}</span>
                <span class="progress-percent">{{ (item.progress || 0).toFixed(1) }}%</span>
              </div>
              <el-progress :percentage="Math.round(item.progress || 0)" :stroke-width="8" />
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
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { usePlanStore } from '@/stores/plan'

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent])

const router = useRouter()
const store = usePlanStore()
const dashboard = computed(() => store.dashboardData)
const loading = ref(store.loading)
const isLoggedIn = computed(() => !!localStorage.getItem('token'))

function goLogin() {
  router.push('/login')
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

const weekCompareClass = computed(() => {
  const cur = dashboard.value?.weeklyDuration || 0
  const last = dashboard.value?.lastWeekDuration || 0
  if (cur > last) return 'compare-up'
  if (cur < last) return 'compare-down'
  return 'compare-neutral'
})

const weekCompareIcon = computed(() => {
  const cur = dashboard.value?.weeklyDuration || 0
  const last = dashboard.value?.lastWeekDuration || 0
  if (cur > last) return '↑'
  if (cur < last) return '↓'
  return '→'
})

const weekCompareText = computed(() => {
  const cur = dashboard.value?.weeklyDuration || 0
  const last = dashboard.value?.lastWeekDuration || 0
  if (last === 0) return '上周无记录'
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

onMounted(() => {
  if (isLoggedIn.value) {
    store.fetchDashboard()
  }
})
</script>

<style scoped>
.card-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.chart-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: var(--space-4);
}

.chart-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.card-title {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 var(--space-4);
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
