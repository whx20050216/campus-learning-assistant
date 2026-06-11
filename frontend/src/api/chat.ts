import request from './request'

export interface ChatSessionVO {
  id: number
  userId: number
  title: string
  materialId?: number
  createdAt: string
  updatedAt: string
}

export interface ChatMessageVO {
  id: number
  sessionId: number
  role: string
  content: string
  createdAt: string
}

export function createSession(title?: string, materialId?: number) {
  return request.post<any, { code: number; msg: string; data: ChatSessionVO }>('/api/chat/sessions', { title, materialId })
}

export function getSessions() {
  return request.get<any, { code: number; msg: string; data: ChatSessionVO[] }>('/api/chat/sessions')
}

export function getMessages(sessionId: number) {
  return request.get<any, { code: number; msg: string; data: ChatMessageVO[] }>(`/api/chat/sessions/${sessionId}/messages`)
}

export function deleteSession(sessionId: number) {
  return request.delete<any, { code: number; msg: string; data: any }>(`/api/chat/sessions/${sessionId}`)
}

export function updateTitle(sessionId: number, title: string) {
  return request.put<any, { code: number; msg: string; data: any }>(`/api/chat/sessions/${sessionId}/title`, { title })
}

export function createStreamUrl(message: string, sessionId?: number, materialId?: number): string {
  const params = new URLSearchParams()
  params.append('message', message)
  if (sessionId) params.append('sessionId', sessionId.toString())
  if (materialId) params.append('materialId', materialId.toString())
  const token = localStorage.getItem('token')
  if (token) params.append('token', token)
  return `/api/chat/stream?${params.toString()}`
}
