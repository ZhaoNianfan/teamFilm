import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo } from '@/api/auth'
import { login as loginApi, logout as logoutApi, getCurrentUser } from '@/api/auth'
import type { LoginParams } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getStoredToken())
  const userInfo = ref<UserInfo | null>(loadUserInfo())
  const permissions = ref<string[]>(userInfo.value ? [userInfo.value.role] : [])

  function getStoredToken(): string {
    return localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  }

  function loadUserInfo(): UserInfo | null {
    try {
      const raw = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo')
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  }

  function setToken(newToken: string, rememberMe: boolean) {
    token.value = newToken
    if (rememberMe) {
      localStorage.setItem('token', newToken)
      sessionStorage.removeItem('token')
    } else {
      sessionStorage.setItem('token', newToken)
      localStorage.removeItem('token')
    }
  }

  function saveUserInfo(info: UserInfo) {
    userInfo.value = info
    permissions.value = [info.role]
    const key = localStorage.getItem('token') ? 'localStorage' : 'sessionStorage'
    const storage = key === 'localStorage' ? localStorage : sessionStorage
    storage.setItem('userInfo', JSON.stringify(info))
  }

  async function login(params: LoginParams) {
    const result = await loginApi(params)
    const data = result.data
    if (data.needChangePassword) {
      return data
    }
    setToken(data.token, params.rememberMe ?? false)
    saveUserInfo(data.userInfo)
    return data
  }

  async function fetchUserInfo() {
    if (!token.value) return
    try {
      const resp = await getCurrentUser()
      saveUserInfo(resp.data)
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
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
  }

  function hasRole(role: string): boolean {
    return userInfo.value?.role === role
  }

  return { token, userInfo, permissions, login, fetchUserInfo, logout, resetState, hasRole }
})
