import request from './request'

export interface PlanCreateDTO {
  name: string
  description?: string
  startDate: string
  endDate: string
  dailyHours: number
  materialIds: number[]
}

export interface PlanUpdateDTO {
  name?: string
  endDate?: string
  dailyHours?: number
  materialIds?: number[]
}

export interface CheckInDTO {
  duration: number
  content?: string
  studyDate?: string
}

export interface TaskVO {
  id: number
  planId: number
  materialId?: number
  taskName: string
  taskDate: string
  plannedHours: number
  status: string
  completedAt?: string
  checkedIn: boolean
  version: number
  createdAt: string
  updatedAt: string
}

export interface PlanVO {
  id: number
  userId: number
  name: string
  description?: string
  startDate: string
  endDate: string
  dailyHours: number
  totalPages: number
  progress: number
  status: string
  remindDate?: string
  reminderSent: number
  version: number
  createdAt: string
  updatedAt: string
  tasks?: TaskVO[]
  materials?: MaterialVO[]
}

export interface PlanReminderVO {
  planId: number
  planName: string
  remindDate: string
  daysLeft: number
}

export interface MaterialVO {
  id: number
  title: string
  courseTag?: string
  pages?: number
  fileSize?: number
}

export function createPlan(data: PlanCreateDTO) {
  return request.post<any, { code: number; msg: string; data: PlanVO }>('/api/plans', data)
}

export function getPlanList() {
  return request.get<any, { code: number; msg: string; data: PlanVO[] }>('/api/plans')
}

export function getPlanDetail(id: number) {
  return request.get<any, { code: number; msg: string; data: PlanVO }>(`/api/plans/${id}`)
}

export function updatePlan(id: number, data: PlanUpdateDTO) {
  return request.put<any, { code: number; msg: string; data: PlanVO }>(`/api/plans/${id}`, data)
}

export function checkIn(taskId: number, data: CheckInDTO) {
  return request.post<any, { code: number; msg: string; data: null }>(`/api/plans/tasks/${taskId}/check-in`, data)
}

export function getProgress(id: number) {
  return request.get<any, { code: number; msg: string; data: number }>(`/api/plans/${id}/progress`)
}

export function getMaterials() {
  return request.get<any, { code: number; msg: string; data: { records: MaterialVO[] } }>('/api/materials?page=0&size=1000')
}

export function deletePlan(id: number) {
  return request.delete<any, { code: number; msg: string; data: any }>(`/api/plans/${id}`)
}

export function getReminders() {
  return request.get<any, { code: number; msg: string; data: PlanReminderVO[] }>('/api/plans/reminders')
}

export function readReminder(planId: number) {
  return request.post<any, { code: number; msg: string; data: any }>(`/api/plans/reminders/${planId}/read`)
}
