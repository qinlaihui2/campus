import request from './request'

export interface NotificationVO {
  id: number
  type: string
  title: string
  content: string
  targetType: string
  targetId: number
  isRead: boolean
  createdAt: string
}

export function getNotifications(page = 1, size = 20) {
  return request.get<any, { code: number; data: { total: number; records: NotificationVO[] } }>(
    '/notifications', { params: { page, size } }
  )
}

export function getUnreadCount() {
  return request.get<any, { code: number; data: { count: number } }>('/notifications/unread-count')
}

export function markAsRead(id: number) {
  return request.put(`/notifications/${id}/read`)
}

export function markAllAsRead() {
  return request.put('/notifications/read-all')
}

export function deleteNotification(id: number) {
  return request.delete(`/notifications/${id}`)
}
