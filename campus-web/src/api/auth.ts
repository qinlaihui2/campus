import request from './request'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  nickname: string
  email?: string
  phone?: string
  role?: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  userId: number
  username: string
  nickname: string
  avatar: string
  role: string
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  role: string
}

export function login(data: LoginRequest) {
  return request.post<any, { code: number; data: LoginResponse }>('/auth/login', data)
}

export function register(data: RegisterRequest) {
  return request.post('/auth/register', data)
}

export function getUserProfile() {
  return request.get<any, { code: number; data: UserInfo }>('/user/profile')
}

export function updateUserProfile(data: Partial<UserInfo>) {
  return request.put('/user/profile', data)
}
