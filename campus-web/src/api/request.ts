import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

const AUTH_WHITELIST = ['/auth/login', '/auth/register', '/auth/verify-code']

request.interceptors.request.use((config) => {
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
