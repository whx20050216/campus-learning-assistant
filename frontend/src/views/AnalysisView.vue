<template>
  <div class="analysis-view">
    <h1 class="page-title">📊 数据分析仪表盘</h1>

    <div v-if="loading" class="loading-state">加载中...</div>

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
              <el-progress :percentage="Math.round(item.progress || 0)" />
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
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { usePlanStore } from '@/stores/plan'

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent])

const store = usePlanStore()
const dashboard = computed(() => store.dashboardData)
const loading = ref(store.loading)

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
  store.fetchDashboard()
})
</script>

<style scoped>
.analysis-view {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}
.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 24px;
}
.loading-state {
  text-align: center;
  padding: 60px;
  color: #909399;
}
.card-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.stat-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}
.stat-compare {
  font-size: 14px;
}
.compare-up {
  color: #67c23a;
}
.compare-down {
  color: #f56c6c;
}
.compare-neutral {
  color: #909399;
}
.chart-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 16px;
}
.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px;
}
.pie-chart {
  width: 100%;
  height: 320px;
}
.empty-chart {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}
.progress-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.progress-item {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px 16px;
}
.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}
.progress-name {
  font-weight: 500;
  color: #303133;
}
.progress-percent {
  color: #409eff;
  font-weight: 600;
}
</style>
