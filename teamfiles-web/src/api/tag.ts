import request from './request'

export interface TagItem {
  id: number
  tagName: string
  color: string
  creatorUserId: number
  creatorUsername: string
  sortOrder: number
  fileCount: number
  personalFileCount: number
  sharedFileCount: number
  createdAt: string
}

export interface TagGroupItem {
  id: number
  groupName: string
  color: string
  sortOrder: number
  tags: TagItem[]
  tagCount: number
}

export function createTag(data: { tagName: string; color?: string }): Promise<{ data: TagItem }> {
  return request.post('/tags', data)
}

export function getTagList(): Promise<{ data: TagItem[] }> {
  return request.get('/tags')
}

export function getTagDetail(id: number): Promise<{ data: TagItem }> {
  return request.get(`/tags/${id}`)
}

export function updateTag(id: number, data: { tagName?: string; color?: string; sortOrder?: number }): Promise<{ data: TagItem }> {
  return request.put(`/tags/${id}`, data)
}

export function deleteTag(id: number): Promise<void> {
  return request.delete(`/tags/${id}`)
}

export function getFilesByTag(id: number): Promise<{ data: any[] }> {
  return request.get(`/tags/${id}/files`)
}

export function autocompleteTags(keyword: string): Promise<{ data: TagItem[] }> {
  return request.get('/tags/autocomplete', { params: { keyword } })
}

export function getSimilarTags(keyword: string): Promise<{ data: TagItem[] }> {
  return request.get('/tags/similar', { params: { keyword } })
}

export function addTagsToFile(fileId: number, tagIds: number[]): Promise<void> {
  return request.post(`/tags/files/${fileId}/tags`, tagIds)
}

export function removeTagFromFile(fileId: number, tagId: number): Promise<void> {
  return request.delete(`/tags/files/${fileId}/tags/${tagId}`)
}

export function batchTags(data: { fileIds: number[]; tagIds: number[]; add: boolean }): Promise<void> {
  return request.post('/tags/files/batch-tags', data)
}

// Tag groups
export function createTagGroup(data: { groupName: string; color?: string }): Promise<{ data: TagGroupItem }> {
  return request.post('/tags/groups', data)
}

export function getTagGroupList(): Promise<{ data: TagGroupItem[] }> {
  return request.get('/tags/groups')
}

export function updateTagGroup(id: number, data: { groupName?: string; color?: string; sortOrder?: number }): Promise<{ data: TagGroupItem }> {
  return request.put(`/tags/groups/${id}`, data)
}

export function deleteTagGroup(id: number): Promise<void> {
  return request.delete(`/tags/groups/${id}`)
}

export function addTagsToGroup(groupId: number, tagIds: number[]): Promise<void> {
  return request.post(`/tags/groups/${groupId}/tags`, tagIds)
}

export function removeTagFromGroup(groupId: number, tagId: number): Promise<void> {
  return request.delete(`/tags/groups/${groupId}/tags/${tagId}`)
}
