import request from './request'

export interface RegisterDTO {
  studentNo: string
  username: string
  email: string
  password: string
  major?: string
  grade?: string
}

export interface TokenVO {
  accessToken: string
  refreshToken: string
}

export interface LoginDTO {
  account: string
  password: string
  rememberMe?: boolean
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
  return request.post<any, { code: number; msg: string; data: TokenVO }>('/api/auth/login', data)
}

export function getCurrentUser() {
  return request.get<any, { code: number; msg: string; data: UserVO }>('/api/auth/me')
}

// TODO: 批次 F 需后端提供 PUT /api/auth/profile 或 PUT /api/users/me 接口
export function updateProfile(data: Partial<Pick<UserVO, 'email' | 'major' | 'grade'>>) {
  return request.put<any, { code: number; msg: string; data: UserVO }>('/api/auth/profile', data)
}
