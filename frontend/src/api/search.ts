import request from './request'

export interface SearchParams {
  query: string
  mode: string
  page?: number
  size?: number
}

export interface SearchResult {
  success: boolean
  items: any[]
  total: number
  totalPages: number
}

/**
 * 智能搜索接口
 * 适配后端 Result<Page<T>> 或 Result<List<T>> 两种返回格式
 */
export const search = async (params: SearchParams): Promise<SearchResult> => {
  const res: any = await request.get('/api/search', { params })

  // 兼容三种可能的返回结构：
  // 1. keyword/all 模式：{ code, msg, data: { records, total, pages } }
  // 2. course/knowledge 模式：{ code, msg, data: [...] }（直接是数组）
  // 3. 直接返回：{ success, items, total, totalPages }
  const pageData = res.data || res

  // course / knowledge 模式后端直接返回数组
  if (Array.isArray(pageData)) {
    return {
      success: res.code === 200 || res.success === true,
      items: pageData,
      total: pageData.length,
      totalPages: 1
    }
  }

  return {
    success: res.code === 200 || res.success === true || !!pageData.records || !!pageData.items,
    items: pageData.records || pageData.items || [],
    total: pageData.total || 0,
    totalPages: pageData.pages || pageData.totalPages || 0
  }
}
