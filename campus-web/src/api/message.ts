import request from './request'

export interface Conversation {
  id: number
  user1Id: number
  user2Id: number
  lastMessage: string
  lastMessageAt: string
  peerName: string
  peerAvatar: string | null
}

export interface Message {
  id: number
  conversationId: number
  senderId: number
  content: string
  type: string
  isRead: number
  createdAt: string
}

export interface UserBrief {
  id: number
  username: string
  nickname: string
  avatar: string | null
  role: string
}

export function getConversations(page = 1, size = 20) {
  return request.get<any, { data: { records: Conversation[]; total: number } }>(
    '/messages/conversations', { params: { page, size } }
  )
}

export function getMessages(conversationId: number, page = 1, size = 50) {
  return request.get<any, { data: { records: Message[] } }>(
    `/messages/conversations/${conversationId}`, { params: { page, size } }
  )
}

export function sendMessage(toUserId: number, content: string, type = 'TEXT') {
  return request.post('/messages/send', { toUserId, content, type })
}

export function deleteConversation(id: number) {
  return request.delete(`/messages/conversations/${id}`)
}

export function getUnreadCount() {
  return request.get<any, { data: number }>('/messages/unread-count')
}

export function searchUser(keyword: string) {
  return request.get<any, { data: UserBrief[] }>('/messages/search-user', { params: { keyword } })
}
