import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '',
  timeout: 30000,
})

function getToken(): string | null {
  return localStorage.getItem('token')
}

function getRefreshToken(): string | null {
  return localStorage.getItem('refreshToken')
}

function removeAllTokens() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('role')
  localStorage.removeItem('rememberMe')
}

request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token && token !== 'undefined') {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response
    }
    return response.data
  },
  async (error) => {
    const originalRequest = error.config

    // 401 且未尝试过刷新
    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      originalRequest._retry = true

      try {
        const refreshToken = getRefreshToken()
        // 不勾选记住我时没有 refreshToken，直接登出
        if (!refreshToken) {
          removeAllTokens()
          ElMessage.error('登录已过期，请重新登录')
          window.location.href = '/login'
          return Promise.reject(error)
        }

        const rememberMe = localStorage.getItem('rememberMe') === 'true'
        const res = await axios.post('/api/auth/refresh', { refreshToken, rememberMe })
        const tokenData = res.data?.data

        if (tokenData?.accessToken) {
          localStorage.setItem('token', tokenData.accessToken)
          if (tokenData.refreshToken) {
            localStorage.setItem('refreshToken', tokenData.refreshToken)
          }
          if (tokenData.role) {
            localStorage.setItem('role', tokenData.role)
          }
          originalRequest.headers['Authorization'] = 'Bearer ' + tokenData.accessToken
          return request(originalRequest)
        }
      } catch (refreshError) {
        removeAllTokens()
        ElMessage.error('登录已过期，请重新登录')
        window.location.href = '/login'
        return Promise.reject(refreshError)
      }
    }

    if (error.response?.status === 403) {
      const msg = error.response?.data?.msg || ''
      if (msg.includes('冻结')) {
        removeAllTokens()
        ElMessage.error('账号已被冻结，请联系管理员')
        window.location.href = '/login'
      } else {
        ElMessage.error('权限不足，无法访问该资源')
      }
    } else if (error.response?.status === 404) {
      ElMessage.error('请求的资源不存在')
    } else if (error.response?.status === 500) {
      ElMessage.error('服务器内部错误，请稍后重试')
    } else if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      ElMessage.error('请求超时，请检查网络连接')
    } else if (!error.response) {
      ElMessage.error('网络连接失败，请检查后端服务是否启动')
    }
    return Promise.reject(error)
  }
)

export default request
