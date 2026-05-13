import request from './request'

export interface SearchParams {
  keyword?: string
  fileType?: string
  fileExtension?: string
  storageSpace?: string
  tagIds?: number[]
  tagLogic?: string
  minSize?: number
  maxSize?: number
  startDate?: string
  endDate?: string
  page?: number
  size?: number
  sortBy?: string
  sortDir?: string
}

export interface SearchResultItem {
  fileId: number
  originalName: string
  fileName: string
  fileType: string
  fileExtension: string
  fileSize: number
  storageSpace: string
  uploadUserId: number
  uploadUsername: string
  createdAt: string
  highlights: Record<string, string[]>
  score: number
  displaySize: string
}

export interface SearchHistoryItem {
  id: number
  userId: number
  keyword: string
  searchType: string
  resultCount: number
  createdAt: string
}

export interface SearchTemplateItem {
  id: number
  userId: number
  templateName: string
  searchCondition: string
  createdAt: string
  updatedAt: string
}

export interface PageResult<T> {
  total: number
  page: number
  pageSize: number
  records: T[]
}

export function search(params: SearchParams): Promise<{ data: PageResult<SearchResultItem> }> {
  return request.post('/search', params)
}

export function getSuggestions(keyword: string): Promise<{ data: string[] }> {
  return request.get('/search/suggest', { params: { keyword } })
}

export function getSearchHistory(): Promise<{ data: SearchHistoryItem[] }> {
  return request.get('/search/history')
}

export function deleteSearchHistory(id: number): Promise<void> {
  return request.delete(`/search/history/${id}`)
}

export function clearSearchHistory(): Promise<void> {
  return request.delete('/search/history')
}

export function saveSearchTemplate(data: { templateName: string; searchCondition: string }): Promise<{ data: SearchTemplateItem }> {
  return request.post('/search/templates', data)
}

export function getSearchTemplates(): Promise<{ data: SearchTemplateItem[] }> {
  return request.get('/search/templates')
}

export function updateSearchTemplate(id: number, data: { templateName?: string; searchCondition?: string }): Promise<{ data: SearchTemplateItem }> {
  return request.put(`/search/templates/${id}`, data)
}

export function deleteSearchTemplate(id: number): Promise<void> {
  return request.delete(`/search/templates/${id}`)
}

export function executeSearchTemplate(id: number): Promise<{ data: PageResult<SearchResultItem> }> {
  return request.post(`/search/templates/${id}/execute`)
}

export function getSimilarFiles(fileId: number): Promise<{ data: SearchResultItem[] }> {
  return request.get(`/search/similar/${fileId}`)
}

export function rebuildIndex(): Promise<void> {
  return request.post('/search/admin/rebuild-index')
}
