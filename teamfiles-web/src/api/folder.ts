import request from './request'

export interface FolderItem {
  id: number
  folderName: string
  parentId: number
  storageSpace: string
  ownerUserId: number
  sortOrder: number
  createdAt: string
  children?: FolderItem[]
  fileCount?: number
}

export function createFolder(folder: {
  folderName: string
  parentId?: number
  storageSpace: string
  sortOrder?: number
}): Promise<{ data: FolderItem }> {
  return request.post('/folders', folder)
}

export function getFolderList(
  storageSpace?: string,
  parentId?: number
): Promise<{ data: FolderItem[] }> {
  return request.get('/folders', { params: { storageSpace, parentId } })
}

export function getFolderTree(storageSpace?: string): Promise<{ data: FolderItem[] }> {
  return request.get('/folders/tree', { params: { storageSpace } })
}

export function getFolderDetail(id: number): Promise<{ data: FolderItem }> {
  return request.get(`/folders/${id}`)
}

export function renameFolder(id: number, name: string): Promise<void> {
  return request.put(`/folders/${id}/rename`, null, { params: { name } })
}

export function deleteFolder(id: number): Promise<void> {
  return request.delete(`/folders/${id}`)
}

export function batchDeleteFolders(ids: number[]): Promise<void> {
  return request.post('/folders/batch-delete', ids)
}
