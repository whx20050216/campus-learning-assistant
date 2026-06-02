import request from './request'

/**
 * 资料上传接口
 * @param file 文件对象
 * @param courseTag 课程标签（可选）
 * @param pages 资料页数（可选）
 * @param onUploadProgress 上传进度回调（可选）
 * @returns Promise<any>
 */
export const uploadMaterial = (
  file: File,
  courseTag?: string,
  pages?: number,
  onUploadProgress?: (progressEvent: any) => void
) => {
  const formData = new FormData()
  formData.append('file', file)
  if (courseTag) formData.append('courseTag', courseTag)
  if (pages != null) formData.append('pages', pages.toString())
  return request.post('/api/materials/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress,
    timeout: 120000,
  })
}
