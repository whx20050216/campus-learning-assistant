import request from './request'

export interface User {
  id: number
  studentNo: string
  username: string
  email: string
  major: string
  grade: string
  role: string
  status: number
  createdAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface SystemStatusVO {
  totalUsers: number
  todayUploads: number
  activePlans: number
  javaStatus: string
  pythonStatus: string
  dbStatus: string
  redisStatus: string
  diskStatus: string
  diskUsage: string
}

export function getUsers(page: number = 1, size: number = 10) {
  return request.get<any, { code: number; msg: string; data: PageResult<User> }>(`/api/admin/users?page=${page}&size=${size}`)
}

export function freezeUser(userId: number, freeze: boolean) {
  return request.post<any, { code: number; msg: string; data: null }>(`/api/admin/users/${userId}/freeze?freeze=${freeze}`)
}

export interface Material {
  id: number
  userId: number
  title: string
  fileType: string
  status: string
  createdAt: string
}

export function getSystemStatus() {
  return request.get<any, { code: number; msg: string; data: SystemStatusVO }>('/api/admin/status')
}

export function getPendingMaterials(page: number = 1, size: number = 10) {
  return request.get<any, { code: number; msg: string; data: PageResult<Material> }>(`/api/admin/materials/pending?page=${page}&size=${size}`)
}

export function auditMaterial(materialId: number, action: 'approve' | 'reject') {
  return request.post<any, { code: number; msg: string; data: null }>(`/api/admin/materials/${materialId}/audit?action=${action}`)
}

export function getAdminMaterialDetail(materialId: number) {
  return request.get<any, { code: number; msg: string; data: any }>(`/api/admin/materials/${materialId}/detail`)
}

export function getCurrentUser() {
  return request.get<any, { code: number; msg: string; data: User }>('/api/auth/me')
}
