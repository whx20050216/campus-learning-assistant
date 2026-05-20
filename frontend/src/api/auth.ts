import request from './request'

export interface RegisterDTO {
  studentNo: string
  username: string
  email: string
  password: string
  major?: string
  grade?: string
}

export interface LoginDTO {
  account: string
  password: string
}

export interface UserVO {
  id: number
  studentNo: string
  username: string
  email: string
  major?: string
  grade?: string
  role: string
  status: number
  createdAt: string
}

export function register(data: RegisterDTO) {
  return request.post<any, { code: number; msg: string; data: number }>('/api/auth/register', data)
}

export function login(data: LoginDTO) {
  return request.post<any, { code: number; msg: string; data: string }>('/api/auth/login', data)
}

export function getCurrentUser() {
  return request.get<any, { code: number; msg: string; data: UserVO }>('/api/auth/me')
}
