import request from './request'

/**
 * 资料上传接口
 * @param formData 包含 file 字段的 FormData
 * @returns Promise<any>
 */
export const uploadMaterial = (formData: FormData) => {
  return request.post('/api/materials/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
