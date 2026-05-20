import request from './request'

export interface SearchParams {
  keyword: string
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
 * 适配后端 Result<Page<T>> 或直接返回的格式
 */
export const search = async (params: SearchParams): Promise<SearchResult> => {
  const res: any = await request.get('/api/search', { params })

  // 兼容两种可能的返回结构：
  // 1. 标准 Result 包装：{ code, msg, data: { records, total, pages } }
  // 2. 直接返回：{ success, items, total, totalPages }
  const pageData = res.data || res

  return {
    success: res.code === 200 || res.success === true || !!pageData.records || !!pageData.items,
    items: pageData.records || pageData.items || [],
    total: pageData.total || 0,
    totalPages: pageData.pages || pageData.totalPages || 0
  }
}
