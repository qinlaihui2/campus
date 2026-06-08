import request from './request'

export interface Conversation {
  id: number
  userId: number
  title: string
  messageCount: number
  createdAt: string
  updatedAt: string
}

export interface Message {
  id: number
  conversationId: number
  role: 'user' | 'assistant'
  content: string
  referencesJson?: string
  feedback?: string
  createdAt: string
}

export function getConversations(page = 1, size = 20) {
  return request.get<any, { code: number; data: { total: number; records: Conversation[] } }>(
    '/chat/conversations', { params: { page, size } }
  )
}

export function getMessages(conversationId: number) {
  return request.get<any, { code: number; data: Message[] }>(`/chat/messages/${conversationId}`)
}

export function deleteConversation(conversationId: number) {
  return request.delete(`/chat/conversation/${conversationId}`)
}

export function feedbackMessage(messageId: number, feedback: string) {
  return request.post(`/chat/feedback/${messageId}`, null, { params: { feedback } })
}
