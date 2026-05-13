import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo } from '@/api/auth'
import { login as loginApi, logout as logoutApi, getCurrentUser } from '@/api/auth'
import type { LoginParams } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)
  const permissions = ref<string[]>([])

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  async function login(params: LoginParams) {
    const result = await loginApi(params)
    const data = result.data
    if (data.needChangePassword) {
      return data
    }
    setToken(data.token)
    userInfo.value = data.userInfo
    localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
    permissions.value = [data.userInfo.role]
    return data
  }

  async function fetchUserInfo() {
    if (!token.value) return
    try {
      const info = await getCurrentUser()
      userInfo.value = info
      localStorage.setItem('userInfo', JSON.stringify(info))
      permissions.value = [info.role]
    } catch {
      resetState()
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      resetState()
    }
  }

  function resetState() {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  function hasRole(role: string): boolean {
    return userInfo.value?.role === role
  }

  return { token, userInfo, permissions, login, fetchUserInfo, logout, resetState, hasRole }
})
