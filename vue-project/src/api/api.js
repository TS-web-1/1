/**
 * Axios HTTP客户端配置文件
 * 
 * 该文件配置了Axios请求客户端，包括：
 * - 基础URL和超时设置
 * - 请求拦截器（自动添加Token）
 * - 响应拦截器（统一错误处理）
 * - 401自动跳转登录
 */
import axios from 'axios'

const TOKEN_KEY = 'token'
const ADMIN_TOKEN_KEY = 'adminToken'
const AUTHOR_TOKEN_KEY = 'authorToken'

/**
 * 创建Axios实例
 * 配置基础URL、超时时间和默认请求头
 */
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * 获取当前存储的认证Token
 * 依次检查用户Token、管理员Token、作者Token
 * @returns {string|null} Token字符串或null
 */
function getAuthToken() {
  return localStorage.getItem(TOKEN_KEY) || 
         localStorage.getItem(ADMIN_TOKEN_KEY) || 
         localStorage.getItem(AUTHOR_TOKEN_KEY)
}

/**
 * 清除所有存储的Token
 */
function clearAllTokens() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(ADMIN_TOKEN_KEY)
  localStorage.removeItem(AUTHOR_TOKEN_KEY)
}

/**
 * 根据当前路径获取重定向路径
 * @returns {string} 重定向路径
 */
function getRedirectPath() {
  const currentPath = window.location.pathname
  if (currentPath.includes('/admin')) {
    return '/profile'
  } else if (currentPath.includes('/author')) {
    return '/login'
  }
  return '/login'
}

/**
 * 请求拦截器
 * 自动在请求头中添加Authorization Token
 */
apiClient.interceptors.request.use(
  (config) => {
    const token = getAuthToken()
    console.log('API Request:', config.method?.toUpperCase(), config.url, 'Token:', token ? '存在' : '不存在')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
      console.log('Authorization header added')
    } else {
      console.log('No token found')
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 * 处理响应数据和错误，401时自动跳转登录
 */
apiClient.interceptors.response.use(
  (response) => {
    console.log('Response received:', response.config.url, response.data)
    return response.data
  },
  (error) => {
    console.error('Response error:', error)
    if (error.response?.status === 401) {
      clearAllTokens()
      window.location.href = getRedirectPath()
    }
    
    const message = error.response?.data?.message || 
                    error.message || 
                    '请求失败，请稍后重试'
    
    return Promise.reject(new Error(message))
  }
)

/**
 * 封装的请求方法对象
 */
const request = {
  get: (url, config) => apiClient.get(url, config),
  post: (url, data) => apiClient.post(url, data),
  put: (url, data) => apiClient.put(url, data),
  patch: (url, data) => apiClient.patch(url, data),
  delete: (url) => apiClient.delete(url)
}

export { apiClient, request }
export default request
