import request from './request'

export interface FileItem {
  id: number
  fileName: string
  originalName: string
  fileSize: number
  fileType: string
  mimeType: string
  fileExtension: string
  md5: string
  storageSpace: string
  folderId: number | null
  folderName: string | null
  uploadUserId: number
  uploadUsername: string
  downloadCount: number
  previewCount: number
  shared: boolean
  tags: { id: number; tagName: string; color: string }[]
  createdAt: string
  updatedAt: string
  displaySize: string
}

export interface FilePageParams {
  page?: number
  size?: number
  keyword?: string
  fileType?: string
  storageSpace?: string
  folderId?: number
  orderBy?: string
  orderDir?: string
}

export interface PageResult<T> {
  total: number
  page: number
  pageSize: number
  records: T[]
}

export interface ChunkInitResult {
  uploadId: string
  chunkCount: number
  chunkSize: number
  skipUpload: boolean
}

export interface ChunkProgress {
  uploadId: string
  total: number
  uploaded: number
  completed: boolean
}

export function uploadFile(
  file: File,
  storageSpace = 'PERSONAL',
  folderId?: number
): Promise<{ data: FileItem }> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('storageSpace', storageSpace)
  if (folderId) formData.append('folderId', String(folderId))
  return request.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function batchUploadFiles(
  files: File[],
  storageSpace = 'PERSONAL',
  folderId?: number
): Promise<{ data: FileItem[] }> {
  const formData = new FormData()
  files.forEach((f) => formData.append('files', f))
  formData.append('storageSpace', storageSpace)
  if (folderId) formData.append('folderId', String(folderId))
  return request.post('/files/batch-upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function initChunkUpload(
  fileName: string,
  fileSize: number,
  fileMd5?: string,
  storageSpace?: string,
  folderId?: number
): Promise<{ data: ChunkInitResult }> {
  return request.post('/files/chunk/init', {
    fileName,
    fileSize,
    fileMd5,
    storageSpace,
    folderId,
  })
}

export function uploadChunk(
  uploadId: string,
  chunkIndex: number,
  chunk: Blob
): Promise<void> {
  const formData = new FormData()
  formData.append('uploadId', uploadId)
  formData.append('chunkIndex', String(chunkIndex))
  formData.append('chunk', chunk)
  return request.post('/files/chunk/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function mergeChunks(uploadId: string): Promise<{ data: FileItem }> {
  return request.post('/files/chunk/merge', null, { params: { uploadId } })
}

export function cancelChunkUpload(uploadId: string): Promise<void> {
  return request.post('/files/chunk/cancel', null, { params: { uploadId } })
}

export function getChunkProgress(uploadId: string): Promise<{ data: ChunkProgress }> {
  return request.get('/files/chunk/progress', { params: { uploadId } })
}

export function getFileList(params: FilePageParams): Promise<{ data: PageResult<FileItem> }> {
  return request.get('/files', { params })
}

export function getFileDetail(id: number): Promise<{ data: FileItem }> {
  return request.get(`/files/${id}`)
}

export function renameFile(id: number, name: string): Promise<void> {
  return request.put(`/files/${id}/rename`, null, { params: { name } })
}

export function deleteFile(id: number): Promise<void> {
  return request.delete(`/files/${id}`)
}

export function copyFile(id: number, targetFolderId: number): Promise<{ data: FileItem }> {
  return request.post(`/files/${id}/copy`, { targetFolderId })
}

export function moveFile(id: number, targetFolderId: number): Promise<{ data: FileItem }> {
  return request.post(`/files/${id}/move`, { targetFolderId })
}

export function moveToTeam(id: number): Promise<{ data: FileItem }> {
  return request.post(`/files/${id}/move-to-team`)
}

export function getSimilarFiles(id: number): Promise<{ data: FileItem[] }> {
  return request.get(`/files/${id}/similar`)
}

function getAuthToken(): string {
  return localStorage.getItem('token') || ''
}

export function getPreviewUrl(id: number): string {
  const token = getAuthToken()
  const base = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  return `${base}/files/${id}/preview?token=${encodeURIComponent(token)}`
}

export function getDownloadUrl(id: number): string {
  const token = getAuthToken()
  const base = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  return `${base}/files/${id}/download?token=${encodeURIComponent(token)}`
}

export function batchDownload(ids: number[]): void {
  const url = `${import.meta.env.VITE_API_BASE_URL || '/api/v1'}/files/batch-download`
  const token = localStorage.getItem('token')
  // Use form POST to trigger download
  fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(ids),
  }).then((res) => {
    if (res.ok) {
      const disposition = res.headers.get('Content-Disposition')
      const match = disposition?.match(/filename="?([^"]+)"?/)
      const filename = match ? match[1] : 'files.zip'
      return res.blob().then((blob) => {
        const a = document.createElement('a')
        a.href = URL.createObjectURL(blob)
        a.download = filename
        a.click()
        URL.revokeObjectURL(a.href)
      })
    }
  })
}
