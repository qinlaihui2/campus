import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 生产环境用完整 URL，开发环境用 Vite 代理
const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

const request = axios.create({
  baseURL: API_BASE ? `${API_BASE}/api` : '/api',
  timeout: 60000,
})

const AUTH_WHITELIST = ['/auth/login', '/auth/register', '/auth/verify-code']

request.interceptors.request.use((config) => {
  // 跳过 ngrok 免费版浏览器警告页
  if (API_BASE.includes('ngrok')) {
    config.headers['ngrok-skip-browser-warning'] = 'true'
  }
  const isWhitelist = AUTH_WHITELIST.some(path => config.url?.includes(path))
  if (!isWhitelist) {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  return config
})

let isRedirecting = false

function handle401() {
  if (isRedirecting) return
  if (router.currentRoute.value.path === '/login') return
  isRedirecting = true
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('role')
  router.push('/login').finally(() => {
    isRedirecting = false
  })
}

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      if (res.code === 401) {
        handle401()
      } else {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      handle401()
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
