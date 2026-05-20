import request from './request'

export const getMaterialDetail = (id: number | string) => {
  return request.get<any, { code: number; msg: string; data: any }>(`/api/materials/${id}`)
}

export const deleteMaterial = (id: number | string) => {
  return request.delete<any, { code: number; msg: string; data: any }>(`/api/materials/${id}`)
}
