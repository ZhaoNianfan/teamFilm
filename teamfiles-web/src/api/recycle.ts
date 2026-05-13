import request from './request'

export interface RecycleItem {
  id: number
  originalType: string
  originalId: number
  fileName: string
  fileSize: number
  storageSpace: string
  originalParentId: number | null
  originalParentName: string | null
  deletedBy: number
  deletedByName: string
  deletedAt: string
  expireAt: string
  displaySize: string
  remainingDays: number
}

export interface RecyclePageParams {
  page?: number
  size?: number
  storageSpace?: string
  keyword?: string
}

export interface PageResult<T> {
  total: number
  page: number
  pageSize: number
  records: T[]
}

export function getRecycleList(params: RecyclePageParams): Promise<{ data: PageResult<RecycleItem> }> {
  return request.get('/recycle', { params })
}

export function restoreRecycleItem(id: number): Promise<void> {
  return request.post(`/recycle/${id}/restore`)
}

export function deleteRecycleItem(id: number): Promise<void> {
  return request.delete(`/recycle/${id}`)
}

export function batchDeleteRecycle(ids: number[]): Promise<void> {
  return request.delete('/recycle/batch', { data: ids })
}

export function clearRecycle(storageSpace?: string): Promise<void> {
  return request.delete('/recycle/clear', { params: { storageSpace } })
}
