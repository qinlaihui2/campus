import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, getUserProfile, updateUserProfile, type LoginRequest, type UserInfo } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('accessToken') || '')
  const userInfo = ref<UserInfo | null>(null)

  async function login(data: LoginRequest) {
    const res = await loginApi(data)
    token.value = res.data.accessToken
    localStorage.setItem('accessToken', res.data.accessToken)
    localStorage.setItem('refreshToken', res.data.refreshToken)
    localStorage.setItem('role', res.data.role)
    userInfo.value = {
      id: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname,
      email: '',
      phone: '',
      avatar: res.data.avatar,
      role: res.data.role,
    }
    return res
  }

  async function fetchUserInfo() {
    try {
      const res = await getUserProfile()
      userInfo.value = res.data
    } catch {
      logout()
    }
  }

  async function updateProfile(data: Partial<UserInfo>) {
    await updateUserProfile(data)
    await fetchUserInfo()
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('role')
    router.push('/login')
  }

  return { token, userInfo, login, fetchUserInfo, updateProfile, logout }
})
