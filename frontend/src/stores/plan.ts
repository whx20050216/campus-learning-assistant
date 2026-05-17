import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import {
  createPlan as apiCreatePlan,
  getPlanList as apiGetPlanList,
  getPlanDetail as apiGetPlanDetail,
  checkIn as apiCheckIn,
  getProgress as apiGetProgress,
} from '@/api/plan'
import { getDashboard as apiGetDashboard } from '@/api/analysis'
import type { PlanVO, TaskVO, PlanCreateDTO, CheckInDTO } from '@/api/plan'
import type { DashboardVO } from '@/api/analysis'

export const usePlanStore = defineStore('plan', () => {
  const planList = ref<PlanVO[]>([])
  const currentPlan = ref<PlanVO | null>(null)
  const taskList = ref<TaskVO[]>([])
  const dashboardData = ref<DashboardVO | null>(null)
  const loading = ref(false)

  const activePlans = computed(() => planList.value.filter((p) => p.status === 'active'))

  async function fetchPlanList() {
    loading.value = true
    try {
      const res = await apiGetPlanList()
      if (res.code === 200) {
        planList.value = res.data || []
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchPlanDetail(id: number) {
    loading.value = true
    try {
      const res = await apiGetPlanDetail(id)
      if (res.code === 200) {
        currentPlan.value = res.data
        taskList.value = res.data?.tasks || []
      }
    } finally {
      loading.value = false
    }
  }

  async function createPlan(dto: PlanCreateDTO) {
    const res = await apiCreatePlan(dto)
    if (res.code === 200) {
      planList.value.unshift(res.data)
    }
    return res
  }

  async function checkInTask(taskId: number, dto: CheckInDTO) {
    const res = await apiCheckIn(taskId, dto)
    if (res.code === 200 && currentPlan.value) {
      await fetchPlanDetail(currentPlan.value.id)
      await fetchPlanList()
    }
    return res
  }

  async function refreshProgress(planId: number) {
    const res = await apiGetProgress(planId)
    if (res.code === 200 && currentPlan.value && currentPlan.value.id === planId) {
      currentPlan.value.progress = res.data
    }
    return res
  }

  async function fetchDashboard() {
    loading.value = true
    try {
      const res = await apiGetDashboard()
      if (res.code === 200) {
        dashboardData.value = res.data
      }
    } finally {
      loading.value = false
    }
  }

  return {
    planList,
    currentPlan,
    taskList,
    dashboardData,
    loading,
    activePlans,
    fetchPlanList,
    fetchPlanDetail,
    createPlan,
    checkInTask,
    refreshProgress,
    fetchDashboard,
  }
})
