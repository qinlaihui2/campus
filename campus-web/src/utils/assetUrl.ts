/**
 * 在开发环境使用相对路径（Vite proxy 转发），
 * 生产环境使用完整 API 地址（前后端分离部署）。
 */

const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

export function assetUrl(path: string | null | undefined): string {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return API_BASE ? `${API_BASE}${path}` : path
}

export function apiBaseUrl(): string {
  return API_BASE
}
