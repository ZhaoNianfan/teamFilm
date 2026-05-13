import request from './request'

export interface DashboardData {
  storage: {
    used: number
    quota: number
    percentage: number
  }
  totalFiles: number
  totalFolders: number
  fileTypeDistribution: { type: string; count: number; totalSize: number }[]
  recentFiles: { id: number; name: string; type: string; uploader: string; createdAt: string }[]
  hotTags: { id: number; name: string; color: string; count: number }[]
  todayUploads: number
}

export interface AdminStats {
  totalUsers: number
  activeUsers: number
  totalFiles: number
  totalFolders: number
  teamFiles: number
  personalFiles: number
  totalStorageUsed: number
  todayUploads: number
  todayDownloads: number
  recycleItems: number
}

export interface OperationLogItem {
  id: number
  userId: number
  username: string
  module: string
  operation: string
  method: string
  requestParams: string
  ip: string
  executionTime: number
  result: number
  errorMsg: string
  createdAt: string
}

export interface BackupItem {
  name: string
  size: number
  time: string
}

export function getDashboard(): Promise<{ data: DashboardData }> {
  return request.get('/statistics/dashboard')
}

export function getAdminOverview(): Promise<{ data: AdminStats }> {
  return request.get('/statistics/admin/overview')
}

export function getOperationLogs(page?: number, size?: number): Promise<{ data: { total: number; records: OperationLogItem[] } }> {
  return request.get('/admin/logs', { params: { page, size } })
}

export function cleanLogs(days?: number): Promise<void> {
  return request.delete('/admin/logs/clean', { params: { days } })
}

export function createBackup(): Promise<{ data: string }> {
  return request.post('/admin/backup/create')
}

export function getBackupList(): Promise<{ data: BackupItem[] }> {
  return request.get('/admin/backup/list')
}

export function restoreBackup(name: string): Promise<void> {
  return request.post(`/admin/backup/${name}/restore`)
}
