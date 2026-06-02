import request from './request'

export const getMaterialDetail = (id: number | string) => {
  return request.get<any, { code: number; msg: string; data: any }>(`/api/materials/${id}`)
}

export const deleteMaterial = (id: number | string, permanent = false) => {
  return request.delete<any, { code: number; msg: string; data: any }>(`/api/materials/${id}?permanent=${permanent}`)
}

export const restoreMaterial = (id: number | string) => {
  return request.post<any, { code: number; msg: string; data: any }>(`/api/materials/${id}/restore`)
}

export const getTrashList = () => {
  return request.get<any, { code: number; msg: string; data: any }>('/api/materials/trash')
}

export const updateOcrText = (id: number | string, ocrText: string) => {
  return request.put<any, { code: number; msg: string; data: any }>(`/api/materials/${id}/ocr`, { ocrText })
}

export const getMaterialList = () => {
  return request.get<any, { code: number; msg: string; data: { records: any[] } }>('/api/materials?page=0&size=1000')
}

export const updateKeywords = (id: number | string, keywords: string[]) => {
  return request.put<any, { code: number; msg: string; data: any }>(`/api/materials/${id}/keywords`, keywords)
}

export function downloadMaterial(id: number | string, preview = false) {
  return request({
    url: `/api/materials/${id}/download`,
    method: 'get',
    params: preview ? { preview: 1 } : undefined,
    responseType: 'blob'
  })
}

export const updateMaterialInfo = (
  id: number | string,
  data: { title?: string; courseTag?: string; pages?: number }
) => {
  const params = new URLSearchParams()
  if (data.title != null) params.append('title', data.title)
  if (data.courseTag != null) params.append('courseTag', data.courseTag)
  if (data.pages != null) params.append('pages', data.pages.toString())
  return request.put<any, { code: number; msg: string; data: any }>(`/api/materials/${id}?${params.toString()}`)
}

export const aiEnhanceSummary = (id: number | string) => {
  return request.post<any, { code: number; msg: string; data: string }>(`/api/materials/${id}/ai-summary`)
}
