import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 30000,
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const resp = response.data
    if (resp.code && resp.code !== 200) {
      ElMessage.error(resp.msg || '请求失败')
      if (resp.code === 401 || resp.code === 402) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(resp.msg))
    }
    return resp
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      window.location.href = '/login'
      return Promise.reject(error)
    }
    // Try to get message from response body
    const respData = error.response?.data
    const msg = respData?.msg || respData?.message || error.message || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(new Error(msg))
  }
)

export default request
