import request from './request'

export interface AiConfig {
  id: number
  userId: number
  isSystem: number
  apiType: string
  apiBaseUrl: string
  modelName: string
  isActive: number
  tested: number
  createdAt: string
}

export interface Conversation {
  id: string
  messageCount: number
  lastMessage: string
  updatedAt: string
}

export interface ChatMessage {
  role: string
  content: string
  time: string
}

export function createAiConfig(data: { apiType: string; apiKey: string; apiBaseUrl?: string; modelName?: string }): Promise<{ data: AiConfig }> {
  return request.post('/ai/config', data)
}

export function updateAiConfig(id: number, data: any): Promise<{ data: AiConfig }> {
  return request.put(`/ai/config/${id}`, data)
}

export function deleteAiConfig(id: number): Promise<void> {
  return request.delete(`/ai/config/${id}`)
}

export function getAiConfigs(): Promise<{ data: AiConfig[] }> {
  return request.get('/ai/config')
}

export function testAiConnection(id: number): Promise<{ data: { success: boolean; response?: string; error?: string } }> {
  return request.post(`/ai/config/${id}/test`)
}

export function createSystemAiConfig(data: any): Promise<{ data: AiConfig }> {
  return request.post('/ai/admin/config', data)
}

export function getFileSummary(fileId: number): Promise<{ data: string }> {
  return request.post('/ai/chat/summary', null, { params: { fileId } })
}

export function getConversations(): Promise<{ data: Conversation[] }> {
  return request.get('/ai/chat/conversations')
}

export function getConversation(id: string): Promise<{ data: { id: string; messages: ChatMessage[] } }> {
  return request.get(`/ai/chat/conversations/${id}`)
}

export function deleteConversation(id: string): Promise<void> {
  return request.delete(`/ai/chat/conversations/${id}`)
}

// Chat non-streaming
export function chatSync(
  message: string,
  conversationId: string | null,
  fileIds: number[]
): Promise<{ data: { conversationId: string; reply: string } }> {
  return request.post('/ai/chat/qa', { message, conversationId, fileIds })
}
