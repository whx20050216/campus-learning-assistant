import request from './request'

export interface PieItem {
  name: string
  value: number
}

export interface ProgressItem {
  name: string
  progress: number
}

export interface DashboardVO {
  courseDistribution: PieItem[]
  currentProgress: ProgressItem[]
  weeklyDuration: number
  lastWeekDuration: number
}

export function getDashboard() {
  return request.get<any, { code: number; msg: string; data: DashboardVO }>('/api/analysis/dashboard')
}
