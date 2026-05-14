import request from './request'

export interface LoginParams {
  username: string
  password: string
  rememberMe?: boolean
}

export interface LoginResult {
  token: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
  needChangePassword?: boolean
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  role: string
  status: number
  storageQuota: number
}

export function login(params: LoginParams) {
  return request.post<any, { code: number; msg: string; data: LoginResult }>('/auth/login', params)
}

export function logout() {
  return request.post('/auth/logout')
}

export function refreshToken() {
  return request.post<any, string>('/auth/refresh')
}

export function getCurrentUser() {
  return request.get<any, { code: number; msg: string; data: UserInfo }>('/auth/me')
}

export function changeFirstPassword(username: string, oldPassword: string, newPassword: string) {
  return request.put('/auth/password', { username, oldPassword, newPassword })
}
